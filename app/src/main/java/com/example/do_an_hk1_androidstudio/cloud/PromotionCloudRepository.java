package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.model.LocalPromotion;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromotionCloudRepository {

    public interface PromotionsCallback {
        void onChanged(@NonNull List<LocalPromotion> promotions);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public PromotionCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public ListenerRegistration listenPromotions(@NonNull PromotionsCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("promotions")
                    .addSnapshotListener((value, error) -> {
                        List<LocalPromotion> promotions = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                promotions.add(mapPromotion(snapshot));
                            }
                        }
                        promotions.sort(Comparator.comparing(LocalPromotion::getCode, String::compareToIgnoreCase));
                        callback.onChanged(promotions);
                    }));
        });
        return holder;
    }

    public void savePromotion(@Nullable String promotionId,
                              String code,
                              String type,
                              double value,
                              int minOrder,
                              @Nullable Integer maxDiscount,
                              @Nullable Long startDateMillis,
                              @Nullable Long endDateMillis,
                              boolean active,
                              @NonNull CompletionCallback callback) {
        String finalId = TextUtils.isEmpty(promotionId) ? DataHelper.newId("promo") : promotionId;
        long now = System.currentTimeMillis();
        Map<String, Object> values = new HashMap<>();
        values.put("promotion_id", finalId);
        values.put("code", safe(code));
        values.put("type", safe(type));
        values.put("value", value);
        values.put("min_order", minOrder);
        values.put("max_discount", maxDiscount);
        values.put("start_date", startDateMillis);
        values.put("end_date", endDateMillis);
        values.put("is_active", active);
        values.put("updated_at", FieldValue.serverTimestamp());
        if (TextUtils.isEmpty(promotionId)) {
            values.put("created_at", now);
        }

        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("promotions")
                    .document(finalId)
                    .set(values, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void deletePromotion(String promotionId, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection("promotions")
                    .document(promotionId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private LocalPromotion mapPromotion(DocumentSnapshot snapshot) {
        return new LocalPromotion(
                stringValue(snapshot.getString("promotion_id"), snapshot.getId()),
                stringValue(snapshot.getString("code"), ""),
                stringValue(snapshot.getString("type"), "percent"),
                doubleValue(snapshot.get("value")),
                intValue(snapshot.get("min_order")),
                integerObject(snapshot.get("max_discount")),
                longObject(snapshot.get("start_date")),
                longObject(snapshot.get("end_date")),
                boolValue(snapshot.get("is_active"), true)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String stringValue(@Nullable String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private boolean boolValue(@Nullable Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private double doubleValue(@Nullable Object value) {
        if (value instanceof Double) return (Double) value;
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        return 0d;
    }

    private int intValue(@Nullable Object value) {
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();
        return 0;
    }

    @Nullable
    private Integer integerObject(@Nullable Object value) {
        if (value == null) return null;
        return intValue(value);
    }

    @Nullable
    private Long longObject(@Nullable Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        return null;
    }

    private String fallbackMessage(@Nullable String value) {
        return value == null ? "Firebase auth chưa sẵn sàng" : value;
    }

    private static class ListenerRegistrationHolder implements ListenerRegistration {
        private ListenerRegistration delegate;

        void setDelegate(ListenerRegistration delegate) {
            this.delegate = delegate;
        }

        @Override
        public void remove() {
            if (delegate != null) {
                delegate.remove();
            }
        }
    }
}
