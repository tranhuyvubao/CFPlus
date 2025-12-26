package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

public class ThongTinCuaHangActivity extends AppCompatActivity {
    private Button btnTroVe;
    private NestedScrollView scrollView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable EdgeToEdge để xử lý notch và status bar
        EdgeToEdge.enable(this);
        
        setContentView(R.layout.thongtin_cuahang);
        
        scrollView = findViewById(R.id.scrollView);
        btnTroVe = findViewById(R.id.btnTroVe);
        
        // Xử lý window insets để tránh bị che bởi notch và status bar
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Xử lý sự kiện khi người dùng nhấn nút Trở về
        btnTroVe.setOnClickListener(v -> {
            finish(); // Đóng Activity và quay lại Fragment_Setting
        });
    }
}
