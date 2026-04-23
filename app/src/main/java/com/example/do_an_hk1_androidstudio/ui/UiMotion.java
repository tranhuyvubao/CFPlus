package com.example.do_an_hk1_androidstudio.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;

public final class UiMotion {

    private UiMotion() {
    }

    public static void applyPressFeedback(@NonNull View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(120).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
            }
            return false;
        });
    }

    public static void bounce(@NonNull View view) {
        view.animate()
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(140)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220)
                        .setInterpolator(new OvershootInterpolator(2f))
                        .start())
                .start();
    }

    public static void pulse(@NonNull View view) {
        view.animate()
                .alpha(0.82f)
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(120)
                .withEndAction(() -> view.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180)
                        .start())
                .start();
    }
}
