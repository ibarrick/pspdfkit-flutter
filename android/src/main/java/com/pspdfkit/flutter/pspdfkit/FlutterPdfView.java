package com.pspdfkit.flutter.pspdfkit;

import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.ui.PdfFragment;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.flutter.app.FlutterFragmentActivity;
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
    private PdfFragmentContainer containerView;

    FlutterPdfView(Context context, BinaryMessenger messenger, int id, Uri uri, PdfConfiguration config, Map<String, Number> rect, FragmentActivity activity) {
        textView = new TextView(context);
        this.fragment = PdfFragment.newInstance(uri, config);
        this.activity = activity;
        this.context = context;
        this.containerView = new PdfFragmentContainer(context, activity, fragment);
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

        methodChannel = new MethodChannel(messenger, "com.pspdfkit.flutter/pdfview_" + id);
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        switch (methodCall.method) {
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
