package com.pspdfkit.flutter.pspdfkit;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;

import com.pspdfkit.PSPDFKit;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.DocumentSource;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.PdfDocumentLoader;
import com.pspdfkit.document.processor.NewPage;
import com.pspdfkit.document.processor.PdfProcessor;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.forms.CheckBoxFormElement;
import com.pspdfkit.forms.FormField;
import com.pspdfkit.forms.FormProvider;
import com.pspdfkit.forms.FormType;
import com.pspdfkit.forms.TextFormElement;
import com.pspdfkit.ui.PdfActivity;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.fragment.app.FragmentActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static com.pspdfkit.flutter.pspdfkit.util.Preconditions.requireNotNullNotEmpty;
import static io.flutter.util.PathUtils.getFilesDir;

/**
 * Pspdfkit Plugin.
 */
public class PspdfkitPlugin implements MethodCallHandler {
    private static final String FILE_SCHEME = "file:///";
    private final Context context;

    private HashMap<String, PdfDocument> openPdfs = new HashMap<String, PdfDocument>();
    public PspdfkitPlugin(Context context) {
        this.context = context;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "pspdfkit");
        channel.setMethodCallHandler(new PspdfkitPlugin(registrar.activeContext()));
        registrar.platformViewRegistry().registerViewFactory("com.pspdfkit.flutter/pdfview", new PdfViewFactory(registrar.messenger(), (FragmentActivity) registrar.activity()));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        String name;
        PdfDocument doc;
        File outputFile;
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
            case "openPdfDocument":
                String uri = call.argument("uri");
                name = call.argument("name");
                DocumentSource source = new DocumentSource(Uri.parse(uri));
                doc = null;
                try {
                    doc = PdfDocumentLoader.openDocument(this.context, source);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (doc != null) {
                    openPdfs.put(name, doc);
                }
                break;
            case "fillPdfForm":
                name = call.argument("name");
                Map<String, Object> args = call.argument("fields");
                doc = openPdfs.get(name);
                FormProvider provider = doc.getFormProvider();
                List<FormField> fields = provider.getFormFields();
                for (FormField field : fields) {
                   if (field.getType() == FormType.TEXT) {
                       TextFormElement textElement = (TextFormElement) field.getFormElement();
                       String formName = field.getName();
                       textElement.setText((String) (args.get(formName) != null ? args.get(formName) : ""));
                   } else if (field.getType() == FormType.CHECKBOX) {
                       CheckBoxFormElement checkBoxElement = (CheckBoxFormElement)field.getFormElement();
                       if ((Boolean) args.get(field.getName())) {
                           checkBoxElement.toggleSelection();
                       }
                   }
                }
                break;
            case "flattenPdfForm":
                name = call.argument("name");
                doc = openPdfs.get(name);
                PdfProcessorTask task = PdfProcessorTask.fromDocument(doc)
                        .changeAllAnnotations(PdfProcessorTask.AnnotationProcessingMode.KEEP)
                        .changeFormsOfType(FormType.CHECKBOX, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeFormsOfType(FormType.COMBOBOX, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeFormsOfType(FormType.LISTBOX, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeFormsOfType(FormType.TEXT, PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
                        .changeFormsOfType(FormType.RADIOBUTTON, PdfProcessorTask.AnnotationProcessingMode.FLATTEN);
                outputFile = new File(getFilesDir(this.context) + name + ".pdf");
                PdfProcessor.processDocument(task, outputFile);
                PdfDocument newDoc = null;
                try {
                    newDoc = PdfDocumentLoader.openDocument(this.context, Uri.fromFile(outputFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                openPdfs.put(name, newDoc);
                break;
            case "mergePdfs":
                List<String> names = call.argument("names");
                String outputName = call.argument("name");
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
                outputFile = new File(getFilesDir(this.context) + outputName + ".pdf");
                PdfProcessor.processDocument(mergeTask, outputFile);
                PdfDocument mergedDoc = null;
                try {
                    mergedDoc = PdfDocumentLoader.openDocument(this.context, Uri.fromFile(outputFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                openPdfs.put(outputName, mergedDoc);
                break;
            case "present":
                String documentPath = call.argument("document");
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
