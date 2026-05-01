package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.VnpayConfigCloudRepository;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

public class VnpaySandboxConfigActivity extends AppCompatActivity {

    private EditText edtTmnCode;
    private EditText edtHashSecret;
    private EditText edtReturnUrl;
    private VnpayConfigCloudRepository cloudRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_sandbox_config);
        InsetsHelper.applyActivityRootPadding(this);

        cloudRepository = new VnpayConfigCloudRepository(this);
        edtTmnCode = findViewById(R.id.edtVnpayTmnCode);
        edtHashSecret = findViewById(R.id.edtVnpayHashSecret);
        edtReturnUrl = findViewById(R.id.edtVnpayReturnUrl);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveVnpayConfig).setOnClickListener(v -> saveConfig());
        findViewById(R.id.btnClearVnpayConfig).setOnClickListener(v -> clearConfig());
        fillCurrentValues();
    }

    private void fillCurrentValues() {
        edtTmnCode.setText("");
        edtHashSecret.setText("");
        edtReturnUrl.setText("");

        cloudRepository.getConfig((cloudConfig, message) -> runOnUiThread(() -> {
            if (cloudConfig == null) {
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            edtTmnCode.setText(cloudConfig.tmnCode);
            edtHashSecret.setText(cloudConfig.hashSecret);
            edtReturnUrl.setText(cloudConfig.returnUrl);
        }));
    }

    private void saveConfig() {
        String tmnCode = edtTmnCode.getText().toString().trim();
        String hashSecret = edtHashSecret.getText().toString().trim();
        String returnUrl = edtReturnUrl.getText().toString().trim();
        if (TextUtils.isEmpty(tmnCode) || TextUtils.isEmpty(hashSecret) || TextUtils.isEmpty(returnUrl)) {
            Toast.makeText(this, "Vui long nhap du TMN code, hash secret va return URL.", Toast.LENGTH_SHORT).show();
            return;
        }
        cloudRepository.saveConfig(tmnCode, hashSecret, returnUrl, (success, message) -> runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(this, message == null ? "Khong the luu cau hinh VNPAY len Firebase." : message, Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Da luu cau hinh VNPAY sandbox len Firebase.", Toast.LENGTH_SHORT).show();
            finish();
        }));
    }

    private void clearConfig() {
        cloudRepository.clearConfig((success, message) -> runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(this, message == null ? "Khong the xoa cau hinh VNPAY tren Firebase." : message, Toast.LENGTH_LONG).show();
                return;
            }
            fillCurrentValues();
            Toast.makeText(this, "Da xoa cau hinh VNPAY sandbox tren Firebase.", Toast.LENGTH_SHORT).show();
        }));
    }
}
