package com.pycampers.flutterpdfviewer;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.github.barteksc.pdfviewer.PDFView;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

import static io.flutter.plugin.common.MethodChannel.MethodCallHandler;

public class PdfView implements PlatformView, MethodCallHandler {
    private PDFView pdfView;

    PdfView(Context context, BinaryMessenger messenger, int id) {
        pdfView = ((Activity) context).findViewById(R.id.pdfView);
        MethodChannel methodChannel = new MethodChannel(messenger, "flutter_pdf_viewer/pdfview_" + id);
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public View getView() {
        return pdfView;
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        PDFView.Configurator configurator;

        switch (methodCall.method) {
            case "fromFile":
                configurator = pdfView.fromUri(
                    Uri.parse((String) methodCall.argument("filePath"))
                );
                break;
            default:
                result.notImplemented();
                return;
        }
        configurator
//                    .password(opts.getString("password"))
//                    .scrollHandle(scrollHandle)
//                    .nightMode(opts.getBoolean("nightMode"))
//                    .swipeHorizontal(opts.getBoolean("swipeHorizontal"))
//                    .onLoad(onLoadCompleteListener)
            .load();
    }

    @Override
    public void dispose() {
    }
}
