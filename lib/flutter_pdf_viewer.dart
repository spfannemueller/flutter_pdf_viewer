import 'dart:core';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'downloader.dart';

Widget createPDFView(Function callback) {
  return AndroidView(
    viewType: 'flutter_pdf_viewer/pdfview',
    onPlatformViewCreated: (int id) {
      callback(MethodChannel('flutter_pdf_viewer/pdfview_$id'));
    },
  );
}

_invokeMethod(
  MethodChannel channel,
  String name,
  dynamic info,
  String password,
  bool nightMode,
  String xorDecryptKey,
  bool swipeHorizontal,
) {
  return channel.invokeMethod(
    name,
    {
      'info': info,
      'password': password,
      'nightMode': nightMode ?? false,
      'xorDecryptKey': xorDecryptKey,
      'swipeHorizontal': swipeHorizontal ?? false,
    },

  );
}

class PDFView {
  static Widget fromFile(
    String filePath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
    Function onLoad,
  }) {
    return createPDFView((MethodChannel channel) async {
      await _invokeMethod(
        channel,
        "fromFile",
        filePath,
        password,
        nightMode,
        xorDecryptKey,
        swipeHorizontal,
      );
      if (onLoad != null) await onLoad();
    });
  }

  static Widget fromUrl(
    String url, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
    Function onDownload,
    Function onLoad,
    bool cache: true,
  }) {
    return createPDFView((MethodChannel channel) async {
      String filePath = await downloadAsFile(url);
      if (onDownload != null) await onDownload(filePath);
      var x = await _invokeMethod(
        channel,
        "fromFile",
        filePath,
        password,
        nightMode,
        xorDecryptKey,
        swipeHorizontal,
      );
      print('retvalue: $x');
      if (onLoad != null) await onLoad();
    });
  }

  static Widget fromAsset(
    String assetPath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
    Function onLoad,
  }) {
    return createPDFView((MethodChannel channel) async {
      await _invokeMethod(
        channel,
        "fromAsset",
        assetPath,
        password,
        nightMode,
        xorDecryptKey,
        swipeHorizontal,
      );
      if (onLoad != null) await onLoad();
    });
  }

  static Widget fromBytes(
    Uint8List pdfBytes, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
    Function onLoad,
  }) {
    return createPDFView((MethodChannel channel) async {
      int pdfBytesSize = pdfBytes.length;

      ServerSocket server = await ServerSocket.bind('0.0.0.0', 4567);
      server.listen(
        (Socket client) {
          client.add(pdfBytes);
          client.close();
          server.close();
        },
      );

      await _invokeMethod(
        channel,
        "fromBytes",
        pdfBytesSize,
        password,
        nightMode,
        xorDecryptKey,
        swipeHorizontal,
      );
      if (onLoad != null) await onLoad();
    });
  }
}
