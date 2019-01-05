package com.pycampers.flutterpdfviewer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.Constants;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class PdfActivity extends AppCompatActivity implements OnLoadCompleteListener {
    FrameLayout progressOverlay;
    Uri uri;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.share) {// do whatever
            sharePDF(uri);
        }
            return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pdf_viewer_layout);

        //Set min Zoom Level
        Constants.Pinch.MINIMUM_ZOOM = 0.5f;

        final PDFView pdfView = findViewById(R.id.pdfView);
        pdfView.enableRenderDuringScale(false);
        pdfView.setMinZoom(0.9f);
        pdfView.toRealScale(0.9f);

        //pdfView.spacing(10);
        progressOverlay = findViewById(R.id.progress_overlay);
        progressOverlay.bringToFront();

        System.out.println("Pdf Activity created");

        Intent intent = getIntent();
        final Bundle opts = intent.getExtras();
        assert opts != null;



        if(!opts.getString("title", "").equals("") || opts.getBoolean("share", false)){
            // Find the toolbar view inside the activity layout
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle(opts.getString("title", ""));



            // Sets the Toolbar to act as the ActionBar for this Activity window.
            // Make sure the toolbar exists in the activity and is not null
            setSupportActionBar(toolbar);
            if( getSupportActionBar() != null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                //getSupportActionBar().setIcon(R.drawable.);
            }
        }

        final DefaultScrollHandle scrollHandle = new DefaultScrollHandle(this);
        final OnLoadCompleteListener onLoadCompleteListener = this;

        class PrimeThread extends Thread {
            public void run() {
                String method = opts.getString("method");
                String xorDecryptKey = opts.getString("xorDecryptKey");

                assert method != null;

                PDFView.Configurator configurator;
                if (xorDecryptKey == null) {
                    switch (method) {
                        case "fromFile":
                            configurator = pdfView.fromUri(
                                Uri.parse(opts.getString(method))
                            );

                            uri =  Uri.parse(opts.getString(method));
                            break;
                        case "fromBytes":
                            try {
                                configurator = pdfView.fromBytes(
                                    readBytesFromSocket(opts.getInt(method))
                                );
                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            }
                            break;
                        case "fromAsset":
                            configurator = pdfView.fromAsset(opts.getString(method));

                            break;
                        default:
                            return;
                    }
                } else {
                    byte[] pdfBytes;
                    try {
                        switch (method) {
                            case "fromFile":
                                pdfBytes = xorEncryptDecrypt(
                                    readBytesFromFile(opts.getString(method)),
                                    xorDecryptKey
                                );

                                break;
                            case "fromBytes":
                                pdfBytes = xorEncryptDecrypt(
                                    readBytesFromSocket(opts.getInt(method)),
                                    xorDecryptKey
                                );

                                break;
                            case "fromAsset":
                                pdfBytes = xorEncryptDecrypt(
                                    readBytesFromAsset(opts.getString(method)),
                                    xorDecryptKey
                                );

                                break;
                            default:
                                return;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    configurator = pdfView.fromBytes(pdfBytes);
                }

                configurator
                    .password(opts.getString("password"))
                    .nightMode(opts.getBoolean("nightMode"))
                    .swipeHorizontal(opts.getBoolean("swipeHorizontal"))
                    .onLoad(onLoadCompleteListener)
                    .spacing(15)
                    .load();


            }
        }
        new PrimeThread().start();
    }


    private void sharePDF(Uri uri){

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("application/pdf");
        startActivity(Intent.createChooser(shareIntent, "Medikationsplan versenden"));
    }



    private byte[] readBytesFromSocket(int pdfBytesSize) throws IOException {
        System.out.println(pdfBytesSize);
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

    private byte[] readBytesFromAsset(String fileName) throws IOException {
        InputStream inputStream = getApplicationContext().getAssets().open(fileName);
        byte[] bytes = new byte[inputStream.available()];

        DataInputStream dataInputStream = new DataInputStream(inputStream);
        dataInputStream.readFully(bytes);

        dataInputStream.close();
        inputStream.close();

        return bytes;
    }

    private byte[] readBytesFromFile(String fileName) throws IOException {
        File file = new File(fileName);
        byte[] bytes = new byte[(int) file.length()];

        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
        dataInputStream.readFully(bytes);

        dataInputStream.close();

        return bytes;
    }

    private byte[] xorEncryptDecrypt(byte[] bytes, String key) {
        int secretKeyLength = key.length();

        System.out.println("decrypting...");
        for (int index = 0; index < bytes.length; index++)
            bytes[index] ^= key.charAt(index % secretKeyLength);
        System.out.println("done!");

        return bytes;
    }

    @Override
    public void loadComplete(int nbPages) {
        System.out.println("loaded!");
        progressOverlay.setVisibility(View.GONE);
    }
}
