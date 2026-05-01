package com.example.do_an_hk1_androidstudio;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class TableQrActivity extends AppCompatActivity {

    public static final String EXTRA_TABLE_NAME = "table_name";
    public static final String EXTRA_TABLE_CODE = "table_code";
    private static final String WEB_ORDER_BASE_URL = "https://cafeplus-1fd32.web.app/menu.html";
    private static final String WEB_CACHE_BUSTER = "web-20260428-sizefirebase";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_table_qr);
        InsetsHelper.applySystemBarsPadding(findViewById(R.id.scrollContent));

        TextView tvBack = findViewById(R.id.tvBack);
        TextView tvTitle = findViewById(R.id.tvQrTitle);
        TextView tvCode = findViewById(R.id.tvQrCode);
        TextView tvPayload = findViewById(R.id.tvQrPayload);
        ImageView imgQr = findViewById(R.id.imgTableQr);

        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        String tableName = getIntent().getStringExtra(EXTRA_TABLE_NAME);
        String tableCode = getIntent().getStringExtra(EXTRA_TABLE_CODE);
        if (tableName == null) {
            tableName = "Bàn";
        }
        if (tableCode == null) {
            tableCode = "";
        }

        String payload = WEB_ORDER_BASE_URL
                + "?table=" + Uri.encode(tableCode)
                + "&v=" + WEB_CACHE_BUSTER;

        tvTitle.setText("QR " + tableName);
        tvCode.setText("Mã bàn: " + tableCode);
        tvPayload.setText("Link order: " + payload);
        renderQrCode(imgQr, payload);
    }

    private void renderQrCode(ImageView imageView, String payload) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(payload, BarcodeFormat.QR_CODE, 900, 900);
            imageView.setImageBitmap(bitmap);
        } catch (WriterException exception) {
            imageView.setImageResource(R.drawable.cfplus4);
        }
    }
}
