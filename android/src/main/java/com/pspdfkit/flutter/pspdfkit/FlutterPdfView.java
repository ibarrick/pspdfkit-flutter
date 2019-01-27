package com.pspdfkit.flutter.pspdfkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.InkAnnotation;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.document.DocumentSaveOptions;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.forms.FormElement;
import com.pspdfkit.forms.FormType;
import com.pspdfkit.listeners.DocumentListener;
import com.pspdfkit.signatures.Signature;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.signatures.SignaturePickerFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

public class FlutterPdfView implements PlatformView, MethodChannel.MethodCallHandler {
    private PdfFragment fragment;
    private MethodChannel methodChannel;
    private FragmentActivity activity;
    private ViewGroup viewGroup;
    private Context context;
    private TextView textView;
    private Map<String, PdfDocument> openPdfs;
    private String documentName;
    private PdfFragmentContainer containerView;
    private BasicMessageChannel messageChannel;

    FlutterPdfView(Context context, BinaryMessenger messenger, int id, Uri uri, PdfConfiguration config, Map<String, Number> rect, FragmentActivity activity, String documentName, Map<String, PdfDocument> openPdfs, BasicMessageChannel messageChannel) {
        textView = new TextView(context);
        if (uri != null) {
            this.fragment = PdfFragment.newInstance(uri, config);
        } else {
            PdfDocument doc = openPdfs.get(documentName);
            this.fragment = PdfFragment.newInstance(doc, config);
        }
        this.activity = activity;
        this.context = context;
        this.containerView = new PdfFragmentContainer(context, activity, fragment);
        this.documentName = documentName;
        this.openPdfs = openPdfs;
        containerView.setId(View.generateViewId());
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction tran = fm.beginTransaction();
        tran.add(fragment, "PsPDFKit");
        tran.commit();
        fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentActivityCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
                super.onFragmentActivityCreated(fm, f, savedInstanceState);
                if (f instanceof PdfFragment) {
                    containerView.addView(f.getView());
                    fm.unregisterFragmentLifecycleCallbacks(this);
                }
            }
        }, false);
        this.messageChannel = messageChannel;
        methodChannel = new MethodChannel(messenger, "com.pspdfkit.flutter/pdfview_" + id);
        methodChannel.setMethodCallHandler(this);
        fragment.addDocumentListener(new PageListener(messageChannel));
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        PdfConfiguration config;
        FragmentTransaction tran;
        FragmentManager fm;
        switch (methodCall.method) {
            case "reloadDocument":
                config =  fragment.getConfiguration();
                PdfDocument doc = openPdfs.get(documentName);
                PdfFragment newFragment = PdfFragment.newInstance(doc, config);
                fm = activity.getSupportFragmentManager();
                tran = fm.beginTransaction();
                tran.add(newFragment, "PsPDFKit");
                tran.commit();
                newFragment.addDocumentListener(new PageListener(messageChannel, result));
                fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentActivityCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
                        super.onFragmentActivityCreated(fm, f, savedInstanceState);
                        if (f == newFragment) {
                            containerView.removeAllViews();
                            containerView.addView(f.getView());
                            fragment = newFragment;
                        }
                    }

                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        super.onFragmentResumed(fm, f);
                        if (f == fragment) {
                            fm.unregisterFragmentLifecycleCallbacks(this);
                        }
                    }
                }, false);
                break;
            case "toggleFormEditing":
                config = fragment.getConfiguration();
                PdfConfiguration.Builder builder = new PdfConfiguration.Builder(config);
                if (config.isFormEditingEnabled()) {
                    builder.disableFormEditing();
                } else {
                    builder.enableFormEditing();
                }
                config = builder.build();
                PdfFragment fragment2 = PdfFragment.newInstance(fragment.getDocument(), config);
                fm = activity.getSupportFragmentManager();
                tran = fm.beginTransaction();
                tran.add(fragment2, "PsPDFKit");
                tran.commit();
                fragment2.addDocumentListener(new PageListener(messageChannel, result));
                fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentActivityCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
                        super.onFragmentActivityCreated(fm, f, savedInstanceState);
                        if (f == fragment2) {
                            containerView.removeAllViews();
                            containerView.addView(f.getView());
                            fragment = fragment2;
                        }
                    }

                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        super.onFragmentResumed(fm, f);
                        if (f == fragment) {
                            fm.unregisterFragmentLifecycleCallbacks(this);
                        }
                    }
                }, false);
                break;
            case "incrementPage":
                fragment.setPageIndex((fragment.getPageIndex() + 1) % fragment.getDocument().getPageCount());
                break;
            case "decrementPage":
                fragment.setPageIndex(Math.floorMod(fragment.getPageIndex() - 1, fragment.getDocument().getPageCount()));
                break;
            case "setPage":
                fragment.setPageIndex(methodCall.argument("page"));
                break;
            case "collectSignature":
                SignaturePickerFragment.show(activity.getSupportFragmentManager(), new SignaturePickerFragment.OnSignaturePickedListener() {
                    @Override
                    public void onSignaturePicked(@NonNull Signature signature) {
                        List<FormElement> formElements = fragment.getDocument().getFormProvider().getFormElements();
                        for (int i = 0; i < formElements.size(); ++i) {
                            FormElement formElement = formElements.get(i);
                            if (formElement.getType() != FormType.SIGNATURE || formElement.getName().indexOf("Estimate") > -1) {
                                continue;
                            } else {
                                InkAnnotation ink = signature.toInkAnnotation(fragment.getDocument(), formElement.getAnnotation().getPageIndex(), formElement.getAnnotation().getBoundingBox());
                                fragment.getDocument().getAnnotationProvider().addAnnotationToPage(ink);
                            }
                        }
                        result.success(null);
                    }

                    @Override
                    public void onDismiss() {

                    }
                });
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public View getView() {
        return containerView;
    }

    @Override
    public void dispose() {

    }

}

class PdfFragmentContainer extends FrameLayout {
    FragmentActivity activity;
    PdfFragment fragment;

    public PdfFragmentContainer(Context c, FragmentActivity activity, PdfFragment fragment) {
        super(c);
        this.activity = activity;
        this.fragment = fragment;
        this.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

}

class PageListener implements DocumentListener {
    BasicMessageChannel messageChannel;
    MethodChannel.Result result;
    public PageListener(BasicMessageChannel messageChannel, MethodChannel.Result result) {
        this.messageChannel = messageChannel;
        this.result = result;
    }

    public PageListener(BasicMessageChannel messageChannel) {
        this.messageChannel = messageChannel;
        this.result = result;
    }

    @Override
    public void onDocumentLoaded(@NonNull PdfDocument pdfDocument) {
        if (result != null) {
            result.success(null);
        }
    }

    @Override
    public void onDocumentLoadFailed(@NonNull Throwable throwable) {

    }

    @Override
    public boolean onDocumentSave(@NonNull PdfDocument pdfDocument, @NonNull DocumentSaveOptions documentSaveOptions) {
        return false;
    }

    @Override
    public void onDocumentSaved(@NonNull PdfDocument pdfDocument) {

    }

    @Override
    public void onDocumentSaveFailed(@NonNull PdfDocument pdfDocument, @NonNull Throwable throwable) {

    }

    @Override
    public void onDocumentSaveCancelled(PdfDocument pdfDocument) {

    }

    @Override
    public boolean onPageClick(@NonNull PdfDocument pdfDocument, int i, @Nullable MotionEvent motionEvent, @Nullable PointF pointF, @Nullable Annotation annotation) {
        return false;
    }

    @Override
    public boolean onDocumentClick() {
        return false;
    }

    @Override
    public void onPageChanged(@NonNull PdfDocument pdfDocument, int i) {
        HashMap<String, Integer> msg = new HashMap<String, Integer>();
        msg.put("page", new Integer(i));
        Log.d("Servisuite", "ON PAGE CHANGED");
        messageChannel.send(msg);
    }

    @Override
    public void onDocumentZoomed(@NonNull PdfDocument pdfDocument, int i, float v) {

    }

    @Override
    public void onPageUpdated(@NonNull PdfDocument pdfDocument, int i) {

    }
}
