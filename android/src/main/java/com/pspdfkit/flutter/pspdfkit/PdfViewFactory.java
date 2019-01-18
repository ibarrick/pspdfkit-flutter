package com.pspdfkit.flutter.pspdfkit;

import android.content.Context;
import android.net.Uri;

import com.pspdfkit.PSPDFKit;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfFragment;

import java.util.HashMap;
import java.util.Map;

import androidx.fragment.app.FragmentActivity;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class PdfViewFactory extends PlatformViewFactory {
    private BinaryMessenger messenger = null;
    private Uri uri;
    private PdfConfiguration config;
    private static final String FILE_SCHEME = "file:///";
    private FragmentActivity activity;

    public PdfViewFactory(BinaryMessenger messenger, FragmentActivity activity) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;
        this.uri = uri;
        this.config = config;
        this.activity = activity;
    }
    @Override
    public PlatformView create(Context context, int i, Object o) {

        HashMap args = (HashMap) o;
        PdfConfiguration.Builder configBuilder = new PdfConfiguration.Builder();
        PdfConfiguration config = configBuilder.build();
        String sUri = (String) args.get("uri");
        HashMap rect = (HashMap) args.get("rect");
        if (Uri.parse(sUri).getScheme() == null) {
            if (sUri.startsWith("/")) {
                sUri = sUri.substring(1);
            }
            sUri = FILE_SCHEME + sUri;
        }
        return new FlutterPdfView(context, this.messenger, i, Uri.parse(sUri), config, rect, (FragmentActivity) activity);
    }
}
