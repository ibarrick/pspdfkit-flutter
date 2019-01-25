package com.pspdfkit.flutter.pspdfkit;

import android.content.Context;
import android.net.Uri;

import com.pspdfkit.PSPDFKit;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.page.PageLayoutMode;
import com.pspdfkit.configuration.signatures.SignatureSavingStrategy;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.signatures.SignaturePickerFragment;

import java.util.HashMap;
import java.util.Map;

import androidx.fragment.app.FragmentActivity;
import io.flutter.plugin.common.BasicMessageChannel;
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
    private HashMap<String, PdfDocument> openPdfs;
    private BasicMessageChannel messageChannel;

    public PdfViewFactory(BinaryMessenger messenger, FragmentActivity activity, HashMap<String, PdfDocument> openPdfs, BasicMessageChannel messageChannel) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;
        this.uri = uri;
        this.config = config;
        this.openPdfs = openPdfs;
        this.activity = activity;
        this.messageChannel = messageChannel;
    }
    @Override
    public PlatformView create(Context context, int i, Object o) {

        HashMap args = (HashMap) o;
        PdfConfiguration.Builder configBuilder = new PdfConfiguration.Builder();
        if (args.get("disableFormEditing") != null && (boolean) args.get("disableFormEditing")) {
            configBuilder.disableFormEditing();
        }
        if (args.get("doublePageLayout") == null || !((boolean) args.get("doublePageLayout"))) {
            configBuilder.layoutMode(PageLayoutMode.SINGLE);
        }
        configBuilder.disableAnnotationEditing();
        configBuilder.signatureSavingStrategy(SignatureSavingStrategy.NEVER_SAVE);
        PdfConfiguration config = configBuilder.build();
        String sUri = (String) args.get("uri");
        String documentName = (String) args.get("documentName");
        HashMap rect = (HashMap) args.get("rect");
        if (sUri != null && Uri.parse(sUri).getScheme() == null) {
            if (sUri.startsWith("/")) {
                sUri = sUri.substring(1);
            }
            sUri = FILE_SCHEME + sUri;
        }
        return new FlutterPdfView(context, this.messenger, i, sUri == null ? null : Uri.parse(sUri), config, rect, (FragmentActivity) activity, documentName, openPdfs, messageChannel);
    }
}
