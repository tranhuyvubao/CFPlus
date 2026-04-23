package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.VnpayConfigCloudRepository;
import com.example.do_an_hk1_androidstudio.payment.VnpaySettingsStore;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

public class VnpaySandboxConfigActivity extends AppCompatActivity {

    private EditText edtTmnCode;
    private EditText edtHashSecret;
    private EditText edtReturnUrl;
    private VnpaySettingsStore settingsStore;
    private VnpayConfigCloudRepository cloudRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_sandbox_config);
        InsetsHelper.applyActivityRootPadding(this);

        settingsStore = new VnpaySettingsStore(this);
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
        VnpaySettingsStore.MerchantConfig config = settingsStore.read();
        edtTmnCode.setText(config.tmnCode);
        edtHashSecret.setText(config.hashSecret);
        edtReturnUrl.setText(config.returnUrl);

        cloudRepository.getConfig((cloudConfig, message) -> runOnUiThread(() -> {
            if (cloudConfig == null) {
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            settingsStore.save(cloudConfig.tmnCode, cloudConfig.hashSecret, cloudConfig.returnUrl);
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
            Toast.makeText(this, "Vui lòng nhập đủ TMN code, hash secret và return URL.", Toast.LENGTH_SHORT).show();
            return;
        }
        cloudRepository.saveConfig(tmnCode, hashSecret, returnUrl, (success, message) -> runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(this, message == null ? "Không thể lưu cấu hình VNPAY lên Firebase." : message, Toast.LENGTH_LONG).show();
                return;
            }
            settingsStore.save(tmnCode, hashSecret, returnUrl);
            Toast.makeText(this, "Đã lưu cấu hình VNPAY sandbox lên Firebase.", Toast.LENGTH_SHORT).show();
            finish();
        }));
    }

    private void clearConfig() {
        cloudRepository.clearConfig((success, message) -> runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(this, message == null ? "Không thể xóa cấu hình VNPAY trên Firebase." : message, Toast.LENGTH_LONG).show();
                return;
            }
            settingsStore.clear();
            fillCurrentValues();
            Toast.makeText(this, "Đã xóa cấu hình VNPAY sandbox trên Firebase.", Toast.LENGTH_SHORT).show();
        }));
    }
}
