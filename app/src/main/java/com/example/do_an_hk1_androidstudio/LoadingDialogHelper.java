package com.example.do_an_hk1_androidstudio;

import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class LoadingDialogHelper {
    private final AppCompatActivity activity;
    private Dialog dialog;
    private AnimationDrawable animationDrawable;

    public LoadingDialogHelper(@NonNull AppCompatActivity activity) {
        this.activity = activity;
    }

    public void show() {
        if (activity.isFinishing()) {
            return;
        }

        if (dialog == null) {
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_loading);
            dialog.setCancelable(false);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
        }

        ImageView imgLoading = dialog.findViewById(R.id.imgLoading);
        if (imgLoading != null) {
            imgLoading.setImageResource(R.drawable.pageload_frame_animation);
            if (imgLoading.getDrawable() instanceof AnimationDrawable) {
                animationDrawable = (AnimationDrawable) imgLoading.getDrawable();
            }
        }

        dialog.show();
        if (animationDrawable != null && !animationDrawable.isRunning()) {
            animationDrawable.start();
        }
    }

    public void hide() {
        if (animationDrawable != null && animationDrawable.isRunning()) {
            animationDrawable.stop();
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
