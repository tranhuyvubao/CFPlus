package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.CloudDataSeeder;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

public class BootstrapManagerActivity extends AppCompatActivity {

    private static final String ADMIN_EMAIL = "admin@cfplus.app";
    private static final String ADMIN_PASSWORD = "01020304";
    private CloudDataSeeder seeder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap_manager);
        InsetsHelper.applyActivityRootPadding(this);
        seeder = new CloudDataSeeder(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        TextView tvInfo = findViewById(R.id.tvBootstrapInfo);
        TextView btnCreate = findViewById(R.id.btnBootstrapCreate);

        tvInfo.setText("Tài khoản quản lý mặc định:\nEmail: " + ADMIN_EMAIL + "\nMật khẩu: " + ADMIN_PASSWORD + "\n\nChỉ dùng để khởi tạo hệ thống lần đầu trên Firebase.");
        btnCreate.setOnClickListener(v -> createOrEnsureManager());
    }

    private void createOrEnsureManager() {
        seeder.ensureBootstrapManager((success, message) -> Toast.makeText(
                this,
                success ? "Đã khởi tạo tài khoản quản lý trên Firebase: " + ADMIN_EMAIL : "Lỗi bootstrap manager: " + (message == null ? "Không rõ nguyên nhân" : message),
                Toast.LENGTH_LONG
        ).show());
    }
}
