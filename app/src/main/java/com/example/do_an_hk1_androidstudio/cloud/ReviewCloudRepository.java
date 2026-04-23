package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewCloudRepository {

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public ReviewCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public void submitOrderReview(@NonNull String orderId,
                                  @NonNull String customerId,
                                  int rating,
                                  @Nullable String comment,
                                  @NonNull List<String> productNames,
                                  @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, message == null ? "Firebase auth chưa sẵn sàng" : message);
                return;
            }

            String reviewId = DataHelper.newId("review");
            Map<String, Object> values = new HashMap<>();
            values.put("review_id", reviewId);
            values.put("order_id", orderId);
            values.put("customer_id", customerId);
            values.put("rating", rating);
            values.put("comment", normalize(comment));
            values.put("product_names", productNames);
            values.put("image_upload_status", "pending_ui_only");
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", FieldValue.serverTimestamp());

            firestore.collection("reviews")
                    .document(reviewId)
                    .set(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    @Nullable
    private String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return TextUtils.isEmpty(trimmed) ? null : trimmed;
    }
}
