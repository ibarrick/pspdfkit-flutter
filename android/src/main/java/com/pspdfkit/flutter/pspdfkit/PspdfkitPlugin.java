package com.pspdfkit.flutter.pspdfkit;

import android.content.Context;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.pspdfkit.PSPDFKit;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationProvider;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.annotations.InkAnnotation;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.DocumentSource;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.PdfDocumentLoader;
import com.pspdfkit.document.checkpoint.PdfDocumentCheckpointingStrategy;
import com.pspdfkit.document.printing.DocumentPrintManager;
import com.pspdfkit.document.processor.NewPage;
import com.pspdfkit.document.processor.PdfProcessor;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.document.sharing.DocumentSharingManager;
import com.pspdfkit.document.sharing.ShareAction;
import com.pspdfkit.forms.CheckBoxFormElement;
import com.pspdfkit.forms.FormElement;
import com.pspdfkit.forms.FormField;
import com.pspdfkit.forms.FormProvider;
import com.pspdfkit.forms.FormType;
import com.pspdfkit.forms.SignatureFormElement;
import com.pspdfkit.forms.TextFormElement;
import com.pspdfkit.ui.PdfActivity;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.fragment.app.FragmentActivity;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.JSONMessageCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.StandardMessageCodec;

import static com.pspdfkit.flutter.pspdfkit.util.Preconditions.requireNotNullNotEmpty;
import static io.flutter.util.PathUtils.getFilesDir;

/**
 * Pspdfkit Plugin.
 */
public class PspdfkitPlugin implements MethodCallHandler {
    private static final String FILE_SCHEME = "file:///";
    private final Context context;

    private static HashMap<String, PdfDocument> openPdfs = new HashMap<String, PdfDocument>();
    private static HashMap<String, List<HashMap<String, Object>>> preservedFields = new HashMap<String, List<HashMap<String, Object>>>();

    public PspdfkitPlugin(Context context) {
        this.context = context;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "pspdfkit");
        final BasicMessageChannel messageChannel = new BasicMessageChannel<Object>(registrar.messenger(), "pspdfkit_messages", JSONMessageCodec.INSTANCE);
        PspdfkitPlugin plugin = new PspdfkitPlugin(registrar.activeContext());
        channel.setMethodCallHandler(plugin);
        registrar.platformViewRegistry().registerViewFactory("com.pspdfkit.flutter/pdfview", new PdfViewFactory(registrar.messenger(), (FragmentActivity) registrar.activity(), openPdfs, messageChannel));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        String name;
        PdfDocument doc;
        PdfProcessorTask task;
        File outputFile;
        String outputPath;
        String documentPath;
        FormProvider formProvider;
        switch (call.method) {
            case "frameworkVersion":
                result.success("Android " + PSPDFKit.VERSION);
                break;
            case "setLicenseKey":
                String licenseKey = call.argument("licenseKey");
                requireNotNullNotEmpty(licenseKey, "License key");
                PSPDFKit.initialize(context, licenseKey);
                break;
            case "resizeView":

                break;
            case "renameEstimateFields":
                name = call.argument("name");
                doc = openPdfs.get(name);
                HashMap<String, String> mapping = new HashMap<String, String>();
                mapping.put("Signature", "EstimateSignature");
                task = PdfProcessorTask.fromDocument(doc)
                        .setFormFieldNameMappings(mapping);
                UUID estimateUuid = UUID.randomUUID();
                outputFile = new File(getFilesDir(this.context) + "/" + name + estimateUuid.toString() + "-renamed.pdf");
                PdfProcessor.processDocument(task, outputFile);
                doc = null;
                try {
                    doc = PdfDocumentLoader.openDocument(this.context, Uri.fromFile(outputFile));
                } catch (IOException e) {
                    e.printStackTrace();
                    result.error(null, null, null);
                    return;
                }
                openPdfs.put(name, doc);
                result.success(null);
                break;
            case "openPdfDocument":
                documentPath = call.argument("uri");
                if (documentPath == null) {
                    result.error("Document Path is Null", "", null);
                    return;
                }
                if (Uri.parse(documentPath).getScheme() == null) {
                    if (documentPath.startsWith("/")) {
                        documentPath = documentPath.substring(1);
                    }
                    documentPath = FILE_SCHEME + documentPath;
                }
                String uri = documentPath;
                name = call.argument("name");
                DocumentSource source = new DocumentSource(Uri.parse(uri));
                doc = null;
                try {
                    doc = PdfDocumentLoader.openDocument(this.context, source);
                } catch (IOException e) {
                    e.printStackTrace();
                    result.error(null, null, null);
                    return;
                }
                if (doc != null) {
                    openPdfs.put(name, doc);
                    doc.getCheckpointer().setStrategy(PdfDocumentCheckpointingStrategy.IMMEDIATE);
                }
                result.success(null);
                break;
            case "checkPdf":
                name = call.argument("name");
                result.success(openPdfs.get(name) != null);
                break;
            case "getPageCount":
                name = call.argument("name");
                doc = openPdfs.get(name);
                if (doc != null) {
                    result.success(doc.getPageCount());
                } else {
                    result.success(0);
                }
                break;
            case "fillPdfForm":
                name = call.argument("name");
                Map<String, Object> args = call.argument("fields");
                doc = openPdfs.get(name);
                FormProvider provider = doc.getFormProvider();
                List<FormField> fields = provider.getFormFields();
                for (FormField field : fields) {
                    if (arts.get(formName) == null) {
                        continue;
                    }
                    if (field.getType() == FormType.TEXT) {
                        TextFormElement textElement = (TextFormElement) field.getFormElement();
                        String formName = field.getName();
                        textElement.setText(args.get(formName));
                    } else if (field.getType() == FormType.CHECKBOX) {
                        CheckBoxFormElement checkBoxElement = (CheckBoxFormElement) field.getFormElement();
                        if ((Boolean) args.get(field.getName())) {
                            checkBoxElement.toggleSelection();
                        }
                    }
                }
                result.success(null);
                break;
            case "preserveFormFields":
                name = call.argument("name");
                List<String> fieldNames = call.argument("fieldNames");
                doc = openPdfs.get(name);
                formProvider = doc.getFormProvider();
                List<HashMap<String, Object>> preserved;
                preserved = new ArrayList<HashMap<String, Object>>();
                for (int i = 0; i < fieldNames.size(); ++i) {
                    FormElement formElement = formProvider.getFormElementWithName(fieldNames.get(i));
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    RectF rectF = formElement.getAnnotation().getBoundingBox();
                    map.put("fieldName", fieldNames.get(i));
                    map.put("rect", rectF);
                    preserved.add(map);
                }
                preservedFields.put(name, preserved);
                break;
//            case "restoreFormFields":
//                name = call.argument("name");
//                doc = openPdfs.get(name);
//                doc.getFormProvider().addFormElementToPage()
            case "flattenPdfForm":
                name = call.argument("name");
                doc = openPdfs.get(name);
                task = PdfProcessorTask.fromDocument(doc)
                        .changeAllAnnotations(PdfProcessorTask.AnnotationProcessingMode.KEEP)
                        .changeFormsOfType(FormType.CHECKBOX, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeFormsOfType(FormType.COMBOBOX, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeFormsOfType(FormType.LISTBOX, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeFormsOfType(FormType.TEXT, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeFormsOfType(FormType.RADIOBUTTON, PdfProcessorTask.AnnotationProcessingMode.FLATTEN);
                UUID uuid = UUID.randomUUID();
                outputFile = new File(getFilesDir(this.context) + "/" + name + uuid.toString() + ".pdf");
                PdfProcessor.processDocument(task, outputFile);
                PdfDocument newDoc = null;
                try {
                    newDoc = PdfDocumentLoader.openDocument(this.context, Uri.fromFile(outputFile));
                } catch (IOException e) {
                    e.printStackTrace();
                    result.error(null, null, null);
                    return;
                }
                openPdfs.put(name, newDoc);
                result.success(null);
                break;
            case "clearFiles":
                File dir = new File(getFilesDir(this.context));
                File[] files = dir.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; ++i) {
                        files[i].delete();
                    }
                }
                result.success(null);
                break;
            case "flattenSignatures":
                name = call.argument("name");
                doc = openPdfs.get(name);
                task = PdfProcessorTask.fromDocument(doc)
                        .changeAllAnnotations(PdfProcessorTask.AnnotationProcessingMode.KEEP)
                        .changeFormsOfType(FormType.SIGNATURE, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeAnnotationsOfType(AnnotationType.INK, PdfProcessorTask.AnnotationProcessingMode.FLATTEN);
                outputFile = new File(getFilesDir(this.context) + "/" + name + "-signed.pdf");
                PdfProcessor.processDocument(task, outputFile);
                PdfDocument outDoc = null;
                try {
                    outDoc = PdfDocumentLoader.openDocument(this.context, Uri.fromFile(outputFile));
                } catch (IOException e) {
                    e.printStackTrace();
                    result.error(null, null, null);
                    return;
                }
                openPdfs.put(name, outDoc);
                result.success(null);
                break;
            case "mergePdfs":
                List<String> names = call.argument("names");
                outputPath = call.argument("outputPath");
                PdfProcessorTask mergeTask = PdfProcessorTask.fromDocument(openPdfs.get(names.get(0)));
                int beginIndex = openPdfs.get(names.get(0)).getPageCount();
                List<String> restNames = names.subList(1, names.size());
                for (String docName : restNames) {
                    PdfDocument currDoc = openPdfs.get(docName);
                    for (int i = 0; i < currDoc.getPageCount(); ++i) {
                        mergeTask.addNewPage(NewPage.fromPage(currDoc, i).build(), beginIndex + i);
                    }
                    beginIndex = beginIndex + currDoc.getPageCount();
                }
                outputFile = new File(outputPath);
                PdfProcessor.processDocument(mergeTask, outputFile);
                PdfDocument mergedDoc = null;
                try {
                    mergedDoc = PdfDocumentLoader.openDocument(this.context, Uri.fromFile(outputFile));
                } catch (IOException e) {
                    e.printStackTrace();
                    result.error(null, null, null);
                    return;
                }
                openPdfs.put("merged", mergedDoc);

                result.success(null);
                break;
            case "savePdf":
                name = call.argument("name");
                doc = openPdfs.get(name);
                outputPath = call.argument("outputPath");

                try {
                    doc.save(outputPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    result.error(null, null, null);
                    return;
                }

                result.success(null);
                break;
            case "present":
                documentPath = call.argument("document");
                requireNotNullNotEmpty(documentPath, "Document path");
                if (Uri.parse(documentPath).getScheme() == null) {
                    if (documentPath.startsWith("/")) {
                        documentPath = documentPath.substring(1);
                    }
                    documentPath = FILE_SCHEME + documentPath;
                }
                boolean imageDocument = isImageDocument(documentPath);
                if (imageDocument) {
                    PdfActivity.showImage(context, Uri.parse(documentPath), new PdfActivityConfiguration.Builder(context).build());
                } else {
                    PdfActivity.showDocument(context, Uri.parse(documentPath), new PdfActivityConfiguration.Builder(context).build());
                }
                break;
            case "printDocument":
                documentPath = call.argument("uri");
                if (Uri.parse(documentPath).getScheme() == null) {
                    if (documentPath.startsWith("/")) {
                        documentPath = documentPath.substring(1);
                    }
                    documentPath = FILE_SCHEME + documentPath;
                }
                DocumentSource shareSource = new DocumentSource(Uri.parse(documentPath));
                doc = null;
                try {
                    doc = PdfDocumentLoader.openDocument(this.context, shareSource);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                PdfProcessorTask printTask = PdfProcessorTask.fromDocument(doc);
                for (int i = 0; i < doc.getPageCount(); ++i) {
                    printTask.rotatePage(i, doc.getPageRotation(i));
                }
                DocumentPrintManager.get().print(this.context, doc, printTask);
                break;
            case "checkSignature":
                name = call.argument("name");
                String fieldName = call.argument("fieldName");
                doc = openPdfs.get(name);
                if (doc == null) {
                    result.success(false);
                } else {
                    formProvider = doc.getFormProvider();
                    List<FormElement> elements = formProvider.getFormElements();
                    for (int i = 0; i < elements.size(); ++i) {
                        FormElement formElement = elements.get(i);
                        if (formElement instanceof SignatureFormElement) {
                            InkAnnotation ink = ((SignatureFormElement) formElement).getOverlappingInkSignature();
                            result.success(ink != null && ink.isSignature());
                            return;
                        }
                    }
                }
                result.success(false);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private boolean isImageDocument(@NonNull String documentPath) {
        String extension = "";
        int lastDot = documentPath.lastIndexOf('.');
        if (lastDot != -1) {
            extension = documentPath.substring(lastDot + 1).toLowerCase();
        }
        return extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg");
    }

}
