package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.model.PromoBanner;
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

public class BannerCloudRepository {

    public interface BannersCallback {
        void onChanged(@NonNull List<PromoBanner> banners);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private static final String COLLECTION = "promo_banners";

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public BannerCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public ListenerRegistration listenBanners(@NonNull BannersCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection(COLLECTION)
                    .addSnapshotListener((value, error) -> {
                        List<PromoBanner> banners = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                banners.add(mapBanner(snapshot));
                            }
                        }
                        banners.sort(Comparator.comparingInt(PromoBanner::getSortOrder)
                                .thenComparing(PromoBanner::getTitle, String.CASE_INSENSITIVE_ORDER));
                        callback.onChanged(banners);
                    }));
        });
        return holder;
    }

    public void saveBanner(@Nullable String bannerId,
                           @NonNull String title,
                           @Nullable String subtitle,
                           @Nullable String imageUrl,
                           @Nullable String actionText,
                           @Nullable String productId,
                           @NonNull List<String> productIds,
                           int sortOrder,
                           boolean active,
                           @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            String nextBannerId = TextUtils.isEmpty(bannerId) ? DataHelper.newId("banner") : bannerId;
            Map<String, Object> values = new HashMap<>();
            values.put("banner_id", nextBannerId);
            values.put("title", safe(title));
            values.put("subtitle", safe(subtitle));
            values.put("image_url", normalizeNullable(imageUrl));
            values.put("action_text", safe(actionText));
            List<String> cleanProductIds = cleanProductIds(productIds);
            String primaryProductId = TextUtils.isEmpty(productId)
                    ? (cleanProductIds.isEmpty() ? "" : cleanProductIds.get(0))
                    : safe(productId);
            values.put("product_id", primaryProductId);
            values.put("product_ids", cleanProductIds);
            values.put("sort_order", sortOrder);
            values.put("is_active", active);
            values.put("updated_at", FieldValue.serverTimestamp());
            firestore.collection(COLLECTION)
                    .document(nextBannerId)
                    .set(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public void deleteBanner(@NonNull String bannerId, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection(COLLECTION)
                    .document(bannerId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private PromoBanner mapBanner(DocumentSnapshot snapshot) {
        return new PromoBanner(
                stringValue(snapshot.getString("banner_id"), snapshot.getId()),
                stringValue(snapshot.getString("title"), ""),
                stringValue(snapshot.getString("subtitle"), ""),
                stringValue(snapshot.getString("image_url"), ""),
                stringValue(snapshot.getString("action_text"), "Xem ngay"),
                stringValue(snapshot.getString("product_id"), ""),
                stringListValue(snapshot.get("product_ids"), snapshot.getString("product_id")),
                intValue(snapshot.get("sort_order")),
                boolValue(snapshot.get("is_active"), true)
        );
    }

    private List<String> cleanProductIds(@Nullable List<String> productIds) {
        List<String> cleaned = new ArrayList<>();
        if (productIds == null) {
            return cleaned;
        }
        for (String productId : productIds) {
            String safeProductId = safe(productId);
            if (!safeProductId.isEmpty() && !cleaned.contains(safeProductId)) {
                cleaned.add(safeProductId);
            }
        }
        return cleaned;
    }

    private List<String> stringListValue(@Nullable Object value, @Nullable String fallbackProductId) {
        List<String> values = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                String safeValue = item instanceof String ? safe((String) item) : "";
                if (!safeValue.isEmpty() && !values.contains(safeValue)) {
                    values.add(safeValue);
                }
            }
        }
        String fallback = safe(fallbackProductId);
        if (values.isEmpty() && !fallback.isEmpty()) {
            values.add(fallback);
        }
        return values;
    }

    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @Nullable
    private String normalizeNullable(@Nullable String value) {
        String safeValue = safe(value);
        return safeValue.isEmpty() ? null : safeValue;
    }

    private String stringValue(@Nullable String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private int intValue(@Nullable Object value) {
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return 0;
    }

    private boolean boolValue(@Nullable Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
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
