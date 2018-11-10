package com.pycampers.flutterpdfviewer;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;


class PdfViewFactory extends PlatformViewFactory {
    private final BinaryMessenger messenger;
    private final Registrar registrar;

    PdfViewFactory(Registrar registrar) {
        super(StandardMessageCodec.INSTANCE);
        this.registrar = registrar;
        this.messenger = registrar.messenger();
    }

    @Override
    public PlatformView create(Context context, int id, Object o) {
        return new FlutterPdfViewerPlugin(this.registrar, context, messenger, id);
    }
}

class PdfViewerThread extends Thread {
    private final Result result;
    private final MethodCall methodCall;
    private final FlutterPdfViewerPlugin instance;

    PdfViewerThread(
        FlutterPdfViewerPlugin instance, MethodCall methodCall, Result result
    ) {
        this.methodCall = methodCall;
        this.result = result;
        this.instance = instance;
    }

    public void run() {
        PDFView.Configurator configurator;

        try {
            switch (methodCall.method) {
                case "fromFile":
                    configurator = instance.fromFile(methodCall);
                    break;
                case "fromAsset":
                    configurator = instance.fromAsset(methodCall);
                    break;
                case "fromBytes":
                    configurator = instance.fromBytes(methodCall);
                    break;
                default:
                    result.notImplemented();
                    return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.error(
                "IOException", "Encountered `IOException` while loading PDF: " + methodCall.toString(), e
            );
            return;
        }

        configurator
            .password((String) methodCall.argument("password"))
            .scrollHandle(instance.scrollHandle)
            .nightMode((Boolean) methodCall.argument("nightMode"))
            .swipeHorizontal((Boolean) methodCall.argument("swipeHorizontal"))
            .load();

        result.success(true);
    }
}

public class FlutterPdfViewerPlugin implements MethodCallHandler, PlatformView {
    private PDFView pdfView;
    private Registrar registrar;
    public final DefaultScrollHandle scrollHandle;
    private final Context context;

    FlutterPdfViewerPlugin(
        Registrar registrar, Context context, BinaryMessenger messenger, int id
    ) {
        this.registrar = registrar;
        this.context = context;

        pdfView = new PDFView(context, null);
        scrollHandle = new DefaultScrollHandle(context);

        MethodChannel methodChannel = new MethodChannel(
            messenger, "flutter_pdf_viewer/pdfview_" + id
        );
        methodChannel.setMethodCallHandler(this);
    }

    public static void registerWith(Registrar registrar) {
        registrar
            .platformViewRegistry()
            .registerViewFactory(
                "flutter_pdf_viewer/pdfview",
                new PdfViewFactory(registrar)
            );
    }

    @Override
    public View getView() {
        return pdfView;
    }

    @Override
    public void dispose() {
        pdfView.removeView(pdfView);
    }

    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {
        new PdfViewerThread(this, methodCall, result).start();
    }

    private String extractXorDecryptKey(MethodCall methodCall) {
        return methodCall.argument("xorDecryptKey");
    }

    PDFView.Configurator fromFile(MethodCall methodCall) throws IOException {
        String xorDecryptKey = extractXorDecryptKey(methodCall);
        String filePath = methodCall.argument("info");
        if (xorDecryptKey == null)
            return pdfView.fromFile(new File(Uri.parse(filePath).getPath()));
        else
            return pdfView.fromBytes(
                xorEncryptDecrypt(readBytesFromFile(filePath), xorDecryptKey)
            );
    }

    PDFView.Configurator fromAsset(MethodCall methodCall) throws IOException {
        String xorDecryptKey = extractXorDecryptKey(methodCall);
        String assetPath = registrar.lookupKeyForAsset(
            (String) methodCall.argument("info")
        );
        if (xorDecryptKey == null)
            return pdfView.fromAsset(assetPath);
        else
            return pdfView.fromBytes(
                xorEncryptDecrypt(readBytesFromAsset(assetPath), xorDecryptKey)
            );
    }

    PDFView.Configurator fromBytes(MethodCall methodCall) throws IOException {
        String xorDecryptKey = extractXorDecryptKey(methodCall);
        byte[] pdfBytes = readBytesFromSocket(
            (Integer) methodCall.argument("info")
        );
        if (xorDecryptKey == null)
            return pdfView.fromBytes(pdfBytes);
        else
            return pdfView.fromBytes(xorEncryptDecrypt(pdfBytes, xorDecryptKey));
    }

    private byte[] readBytesFromSocket(int pdfBytesSize) throws IOException {
        byte[] bytes = new byte[pdfBytesSize];

        Socket socket = new Socket("0.0.0.0", 4567);
        InputStream inputStream = socket.getInputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

        int readTillNow = 0;
        while (readTillNow < pdfBytesSize)
            readTillNow += bufferedInputStream.read(
                bytes, readTillNow, pdfBytesSize - readTillNow
            );

        bufferedInputStream.close();
        inputStream.close();
        socket.close();

        return bytes;
    }

    private byte[] readBytesFromAsset(String pathname) throws IOException {
        InputStream inputStream = context.getAssets().open(pathname);
        byte[] bytes = new byte[inputStream.available()];

        DataInputStream dataInputStream = new DataInputStream(inputStream);
        dataInputStream.readFully(bytes);

        dataInputStream.close();
        inputStream.close();

        return bytes;
    }

    private byte[] readBytesFromFile(String pathname) throws IOException {
        File file = new File(pathname);
        byte[] bytes = new byte[(int) file.length()];

        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
        dataInputStream.readFully(bytes);

        dataInputStream.close();

        return bytes;
    }

    private byte[] xorEncryptDecrypt(byte[] bytes, String key) {
        int secretKeyLength = key.length();
        for (int index = 0; index < bytes.length; index++)
            bytes[index] ^= key.charAt(index % secretKeyLength);
        return bytes;
    }
}
