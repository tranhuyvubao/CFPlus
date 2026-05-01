package com.example.do_an_hk1_androidstudio.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;

public final class CfPlusImageLoader {

    private CfPlusImageLoader() {
    }

    public static void load(@NonNull ImageView imageView,
                            @Nullable String imageSource,
                            @DrawableRes int placeholderRes) {
        if (TextUtils.isEmpty(imageSource)) {
            imageView.setImageResource(placeholderRes);
            return;
        }

        String trimmed = imageSource.trim();
        if (trimmed.startsWith("data:image")) {
            Bitmap bitmap = decodeDataUrl(trimmed);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(placeholderRes);
            }
            return;
        }

        Picasso.get()
                .load(trimmed)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .into(imageView);
    }

    @Nullable
    private static Bitmap decodeDataUrl(@NonNull String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0 || commaIndex >= dataUrl.length() - 1) {
            return null;
        }
        try {
            byte[] bytes = Base64.decode(dataUrl.substring(commaIndex + 1), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
