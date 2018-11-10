package com.pycampers.flutterpdfviewer;

import android.content.Intent;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class FlutterPdfViewerPlugin implements MethodCallHandler {
    private final Registrar registrar;

    private FlutterPdfViewerPlugin(Registrar registrar) {
        this.registrar = registrar;
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(
            registrar.messenger(), "flutter_pdf_viewer"
        );
        channel.setMethodCallHandler(new FlutterPdfViewerPlugin(registrar));

        registrar
            .platformViewRegistry()
            .registerViewFactory(
                "flutter_pdf_viewer/pdfview",
                new PdfViewFactory(registrar.messenger())
            );
    }

    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {
        Intent intent = new Intent(this.registrar.context(), PdfActivity.class);

        intent.putExtra("password", (String) methodCall.argument("password"));
        intent.putExtra("nightMode", (Boolean) methodCall.argument("nightMode"));
        intent.putExtra("xorDecryptKey", (String) methodCall.argument("xorDecryptKey"));
        intent.putExtra("swipeHorizontal", (Boolean) methodCall.argument("swipeHorizontal"));
        intent.putExtra("method", methodCall.method);

        switch (methodCall.method) {
            case "fromFile":
                intent.putExtra(methodCall.method, (String) methodCall.argument("filePath"));
                break;
            case "fromBytes":
                intent.putExtra(methodCall.method, (Integer) methodCall.argument("pdfBytesSize"));
                break;
            case "fromAsset":
                intent.putExtra(
                    methodCall.method, this.registrar.lookupKeyForAsset((String) methodCall.argument("assetPath"))
                );
                break;
            default: {
                result.notImplemented();
                return;
            }
        }

        this.registrar.activity().startActivity(intent);
        result.success(true);
    }


}
