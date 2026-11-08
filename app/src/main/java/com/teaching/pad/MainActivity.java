package com.teaching.pad;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_PDF = 101;
    private static final int REQ_IMG = 102;

    private TeachingCanvasView canvasView;
    private TextView tvPage;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    private static class PageData {
        int pdfPageIndex;
        Bitmap cachedBitmap;
        PageData(int index, Bitmap bmp) {
            this.pdfPageIndex = index;
            this.cachedBitmap = bmp;
        }
    }

    private List<PageData> allPages = new ArrayList<>();
    private int currentPageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvasView = findViewById(R.id.teaching_canvas);
        tvPage = findViewById(R.id.tv_page);

        findViewById(R.id.btn_open_pdf).setOnClickListener(v -> openPdfPicker());

        findViewById(R.id.btn_add_page).setOnClickListener(v -> {
            Bitmap blank = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888);
            blank.eraseColor(android.graphics.Color.WHITE);
            int insertPos = allPages.isEmpty() ? 0 : currentPageIndex + 1;
            allPages.add(insertPos, new PageData(-1, blank));
            currentPageIndex = insertPos;
            loadCurrentPage();
        });

        findViewById(R.id.btn_add_image).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_IMG);
        });

        findViewById(R.id.btn_pen).setOnClickListener(v -> canvasView.setEraser(false));
        findViewById(R.id.btn_eraser).setOnClickListener(v -> canvasView.setEraser(true));
        findViewById(R.id.btn_clear).setOnClickListener(v -> canvasView.clearCanvas());

        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                loadCurrentPage();
            }
        });

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (currentPageIndex < allPages.size() - 1) {
                currentPageIndex++;
                loadCurrentPage();
            }
        });
    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, REQ_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == REQ_PDF) {
                loadPdfFromUri(uri);
            } else if (requestCode == REQ_IMG) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    Bitmap img = BitmapFactory.decodeStream(is);
                    canvasView.insertImage(img);
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadPdfFromUri(Uri uri) {
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();

            fileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            pdfRenderer = new PdfRenderer(fileDescriptor);
            allPages.clear();

            for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                allPages.add(new PageData(i, bitmap));
            }
            currentPageIndex = 0;
            loadCurrentPage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCurrentPage() {
        if (allPages.isEmpty()) return;
        PageData page = allPages.get(currentPageIndex);
        canvasView.setPageBackground(page.cachedBitmap);
        tvPage.setText("Page: " + (currentPageIndex + 1) + " / " + allPages.size());
    }

    @Override
    protected void onDestroy() {
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}
