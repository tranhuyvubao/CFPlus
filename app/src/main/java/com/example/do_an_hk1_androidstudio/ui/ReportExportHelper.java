package com.example.do_an_hk1_androidstudio.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class ReportExportHelper {

    private static final String REPORT_FOLDER = "CFPLUS";

    private ReportExportHelper() {
    }

    @NonNull
    public static ExportResult exportSimplePdf(@NonNull Context context,
                                               @NonNull String fileName,
                                               @NonNull String title,
                                               @NonNull String content) throws IOException {
        PdfDocument document = new PdfDocument();
        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint titlePaint = new Paint();
            titlePaint.setTextSize(20f);
            titlePaint.setFakeBoldText(true);

            Paint bodyPaint = new Paint();
            bodyPaint.setTextSize(12f);

            int x = 32;
            int y = 48;
            canvas.drawText(title, x, y, titlePaint);
            y += 32;

            for (String line : content.split("\n")) {
                canvas.drawText(line, x, y, bodyPaint);
                y += 18;
                if (y > 800) {
                    break;
                }
            }

            document.finishPage(page);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return writeToDownloads(context, fileName, document);
            }
            return writeToPublicDownloadsLegacy(context, fileName, document);
        } finally {
            document.close();
        }
    }

    @NonNull
    private static ExportResult writeToDownloads(@NonNull Context context,
                                                 @NonNull String fileName,
                                                 @NonNull PdfDocument document) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + REPORT_FOLDER);
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Cannot create download document");
        }

        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Cannot open download output stream");
            }
            document.writeTo(outputStream);
        } catch (IOException exception) {
            resolver.delete(uri, null, null);
            throw exception;
        }

        ContentValues publishedValues = new ContentValues();
        publishedValues.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, publishedValues, null, null);
        return new ExportResult(uri, null, "Download/" + REPORT_FOLDER + "/" + fileName);
    }

    @NonNull
    private static ExportResult writeToPublicDownloadsLegacy(@NonNull Context context,
                                                             @NonNull String fileName,
                                                             @NonNull PdfDocument document) throws IOException {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), REPORT_FOLDER);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            document.writeTo(outputStream);
            outputStream.flush();
        }
        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
        return new ExportResult(uri, file, "Download/" + REPORT_FOLDER + "/" + fileName);
    }

    public static void shareFile(@NonNull Context context,
                                 @NonNull File file,
                                 @NonNull String mimeType) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );
            shareUri(context, uri, mimeType);
        } catch (Exception e) {
            Toast.makeText(context, "Không thể chia sẻ file báo cáo.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareUri(@NonNull Context context,
                                @NonNull Uri uri,
                                @NonNull String mimeType) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ báo cáo"));
        } catch (Exception e) {
            Toast.makeText(context, "Không thể chia sẻ file báo cáo.", Toast.LENGTH_SHORT).show();
        }
    }

    public static final class ExportResult {
        public final Uri uri;
        public final File file;
        public final String displayPath;

        private ExportResult(@NonNull Uri uri, File file, @NonNull String displayPath) {
            this.uri = uri;
            this.file = file;
            this.displayPath = displayPath;
        }
    }
}
