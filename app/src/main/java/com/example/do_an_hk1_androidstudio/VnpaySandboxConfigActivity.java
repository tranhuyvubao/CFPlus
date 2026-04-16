package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.payment.VnpaySettingsStore;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

public class VnpaySandboxConfigActivity extends AppCompatActivity {

    private EditText edtTmnCode;
    private EditText edtHashSecret;
    private EditText edtReturnUrl;
    private VnpaySettingsStore settingsStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_sandbox_config);
        InsetsHelper.applyActivityRootPadding(this);

        settingsStore = new VnpaySettingsStore(this);
        edtTmnCode = findViewById(R.id.edtVnpayTmnCode);
        edtHashSecret = findViewById(R.id.edtVnpayHashSecret);
        edtReturnUrl = findViewById(R.id.edtVnpayReturnUrl);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveVnpayConfig).setOnClickListener(v -> saveConfig());
        findViewById(R.id.btnClearVnpayConfig).setOnClickListener(v -> clearConfig());
        fillCurrentValues();
    }

    private void fillCurrentValues() {
        VnpaySettingsStore.MerchantConfig config = settingsStore.read();
        edtTmnCode.setText(config.tmnCode);
        edtHashSecret.setText(config.hashSecret);
        edtReturnUrl.setText(config.returnUrl);
    }

    private void saveConfig() {
        String tmnCode = edtTmnCode.getText().toString().trim();
        String hashSecret = edtHashSecret.getText().toString().trim();
        String returnUrl = edtReturnUrl.getText().toString().trim();
        if (TextUtils.isEmpty(tmnCode) || TextUtils.isEmpty(hashSecret) || TextUtils.isEmpty(returnUrl)) {
            Toast.makeText(this, "Vui lòng nhập đủ TMN code, hash secret và return URL.", Toast.LENGTH_SHORT).show();
            return;
        }
        settingsStore.save(tmnCode, hashSecret, returnUrl);
        Toast.makeText(this, "Đã lưu cấu hình VNPAY sandbox.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void clearConfig() {
        settingsStore.clear();
        fillCurrentValues();
        Toast.makeText(this, "Đã xóa cấu hình VNPAY sandbox.", Toast.LENGTH_SHORT).show();
    }
}
