package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private AnimationDrawable splashAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Đặt layout splash cho nền app 3s
        setContentView(R.layout.activity_splash);

        ImageView imgSplashLoading = findViewById(R.id.imgSplashLoading);
        if (imgSplashLoading != null) {
            imgSplashLoading.setImageResource(R.drawable.pageload_frame_animation);
            if (imgSplashLoading.getDrawable() instanceof AnimationDrawable) {
                splashAnimation = (AnimationDrawable) imgSplashLoading.getDrawable();
            }
        }

        // Delay 3s rồi chuyển sang MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Đóng splash screen
        }, 3000); // 3000ms = 3s
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (splashAnimation != null && !splashAnimation.isRunning()) {
            splashAnimation.start();
        }
    }

    @Override
    protected void onStop() {
        if (splashAnimation != null && splashAnimation.isRunning()) {
            splashAnimation.stop();
        }
        super.onStop();
    }
}
