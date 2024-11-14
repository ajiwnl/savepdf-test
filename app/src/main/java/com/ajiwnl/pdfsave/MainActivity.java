package com.ajiwnl.pdfsave;
import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 1;
    private static final String TAG = "PDFSave";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button saveBtn = findViewById(R.id.saveBtn);
        Button shareBtn = findViewById(R.id.shareBtn);

        saveBtn.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                Log.d(TAG, "Storage permission granted.");
                saveViewAsPDF();
            } else {
                Log.d(TAG, "Requesting storage permission...");
                requestStoragePermission();
            }
        });

    }

    private boolean hasStoragePermission() {
        boolean permissionGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Storage permission status: " + permissionGranted);
        return permissionGranted;
    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Storage permission is required to save PDF", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Showing permission rationale to the user.");
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "User granted storage permission.");
                saveViewAsPDF();
            } else {
                Log.d(TAG, "User denied storage permission.");
                Toast.makeText(this, "Permission denied to save PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void saveViewAsPDF() {
        Log.d(TAG, "Starting PDF creation process...");

        // 1/2 Index Card size in inches: 3 x 2.5 inches
        // Convert to pixels: 1 inch = 72 dpi (density-independent pixels)
        int width = (int) (3 * 72);  // 3 inches in pixels (half of index card width)
        int height = (int) (2.5 * 72);  // 2.5 inches in pixels (half of index card height)

        // Create a PdfDocument to start the PDF creation
        PdfDocument document = new PdfDocument();

        // Define the PDF page size and create a PageInfo object
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, 1).create();

        // Start a new page
        PdfDocument.Page page = document.startPage(pageInfo);

        // Create a Canvas object to draw on the page
        android.graphics.Canvas canvas = page.getCanvas();

        // Set up the Paint object for text style
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(16);  // Set text size to at least 18

        // Find the TextViews
        TextView textView1 = findViewById(R.id.savetext);
        TextView textView2 = findViewById(R.id.savetext0);
        TextView textView3 = findViewById(R.id.savetext1);
        TextView textView4 = findViewById(R.id.savetext2);

        // Extract text from each TextView
        String text1 = textView1.getText().toString();
        String text2 = textView2.getText().toString();
        String text3 = textView3.getText().toString();
        String text4 = textView4.getText().toString();

        // Set the starting positions for the text
        float x = 10; // Starting x position for text (a bit of margin)
        float y = 30; // Starting y position for text (a bit of margin)

        // Draw the text on the PDF canvas
        canvas.drawText(text1, x, y, paint);
        y += 30; // Move y position down for next text (adjust for space)
        canvas.drawText(text2, x, y, paint);
        y += 30;
        canvas.drawText(text3, x, y, paint);
        y += 30;
        canvas.drawText(text4, x, y, paint);

        // Finish the page and document
        document.finishPage(page);

        // Save the PDF to storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, save using MediaStore
            savePDFUsingMediaStore(document);
        } else {
            // For older versions, save using traditional file system
            savePDFUsingFileSystem(document);
        }
    }

    private void savePDFUsingMediaStore(PdfDocument document) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.e(TAG, "MediaStore requires API level 29 or higher");
            savePDFUsingFileSystem(document);
            return;
        }

        try {
            String fileName = "textview_content.pdf";

            // Set up values for the PDF file
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/MyPDFs");

            // Insert the new file into MediaStore and get the URI for the file
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                // Open the OutputStream for the URI returned by the insert
                try (OutputStream fos = getContentResolver().openOutputStream(uri)) {
                    if (fos != null) {
                        document.writeTo(fos);
                        Log.d(TAG, "PDF saved successfully using MediaStore.");
                        Toast.makeText(this, "PDF saved in Downloads/MyPDFs folder.", Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Failed to get OutputStream from MediaStore.");
                        Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error saving PDF using MediaStore: " + e.getMessage(), e);
                    Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Failed to insert file into MediaStore.");
                Toast.makeText(this, "Failed to save PDF in MediaStore.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting PDF file into MediaStore: " + e.getMessage(), e);
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
            Log.d(TAG, "PDF document closed.");
        }
    }

    private void savePDFUsingFileSystem(PdfDocument document) {
        File pdfDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyPDFs");
        if (!pdfDir.exists()) {
            boolean dirCreated = pdfDir.mkdirs();
            Log.d(TAG, "Creating PDF directory: " + dirCreated);
        }

        File pdfFile = new File(pdfDir, "textview_content.pdf");

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            document.writeTo(fos);
            Log.d(TAG, "PDF saved successfully at: " + pdfFile.getAbsolutePath());
            Toast.makeText(this, "PDF saved at: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error saving PDF: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
            Log.d(TAG, "PDF document closed.");
        }
    }
}