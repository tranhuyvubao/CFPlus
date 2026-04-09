package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.CloudDataSeeder;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

public class FirebaseSeederActivity extends AppCompatActivity {

    private TextView btnSeedAll;
    private TextView tvLog;
    private CloudDataSeeder seeder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_seeder);
        InsetsHelper.applyActivityRootPadding(this);
        seeder = new CloudDataSeeder(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        btnSeedAll = findViewById(R.id.btnSeedAll);
        tvLog = findViewById(R.id.tvSeedLog);

        btnSeedAll.setOnClickListener(v -> seedAll());
    }

    private void seedAll() {
        seeder.seedBaseData((success, message) -> {
            if (success) {
                tvLog.setText("Đã seed dữ liệu mẫu lên Firebase: tài khoản, danh mục, món, bàn, kho, khuyến mãi.\nTài khoản quản lý mặc định: admin@cfplus.app / 01020304.");
                Toast.makeText(this, "Seed Firebase thành công!", Toast.LENGTH_SHORT).show();
                return;
            }
            tvLog.setText("Seed Firebase thất bại: " + (message == null ? "Không rõ nguyên nhân" : message));
            Toast.makeText(this, "Seed Firebase thất bại", Toast.LENGTH_SHORT).show();
        });
    }
}
