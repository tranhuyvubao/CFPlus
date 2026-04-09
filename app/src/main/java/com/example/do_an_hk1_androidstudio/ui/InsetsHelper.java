package com.example.do_an_hk1_androidstudio.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetsHelper {

    private InsetsHelper() {
    }

    public static void applySystemBarsPadding(View target) {
        if (target == null) {
            return;
        }

        final int initialLeft = target.getPaddingLeft();
        final int initialTop = target.getPaddingTop();
        final int initialRight = target.getPaddingRight();
        final int initialBottom = target.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(target, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(target);
    }

    public static void applyStatusBarPadding(View target) {
        if (target == null) {
            return;
        }

        final int initialLeft = target.getPaddingLeft();
        final int initialTop = target.getPaddingTop();
        final int initialRight = target.getPaddingRight();
        final int initialBottom = target.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(target, (view, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(
                    initialLeft + statusBar.left,
                    initialTop + statusBar.top,
                    initialRight + statusBar.right,
                    initialBottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(target);
    }

    public static void applyNavigationBarPadding(View target) {
        if (target == null) {
            return;
        }

        final int initialLeft = target.getPaddingLeft();
        final int initialTop = target.getPaddingTop();
        final int initialRight = target.getPaddingRight();
        final int initialBottom = target.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(target, (view, insets) -> {
            Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(
                    initialLeft + navBar.left,
                    initialTop,
                    initialRight + navBar.right,
                    initialBottom + navBar.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(target);
    }

    public static void applyActivityRootPadding(AppCompatActivity activity) {
        if (activity == null) {
            return;
        }
        EdgeToEdge.enable(activity);
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup contentGroup = (ViewGroup) content;
        if (contentGroup.getChildCount() == 0) {
            return;
        }
        applySystemBarsPadding(contentGroup.getChildAt(0));
    }
}
