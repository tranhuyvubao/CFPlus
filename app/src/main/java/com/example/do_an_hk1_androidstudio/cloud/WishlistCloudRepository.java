package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class WishlistCloudRepository {

    public interface FavoriteIdsCallback {
        void onChanged(@NonNull Set<String> productIds);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public WishlistCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public ListenerRegistration listenFavoriteIds(@NonNull String customerId,
                                                  @NonNull FavoriteIdsCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new LinkedHashSet<>());
                return;
            }
            holder.setDelegate(firestore.collection("users")
                    .document(customerId)
                    .collection("favorites")
                    .addSnapshotListener((value, error) -> {
                        LinkedHashSet<String> ids = new LinkedHashSet<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot document : value) {
                                String productId = document.getString("product_id");
                                ids.add(productId == null || productId.trim().isEmpty()
                                        ? document.getId()
                                        : productId.trim());
                            }
                        }
                        callback.onChanged(ids);
                    }));
        });
        return holder;
    }

    public void setFavorite(@NonNull String customerId,
                            @NonNull String productId,
                            boolean favorite,
                            @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            if (favorite) {
                Map<String, Object> values = new HashMap<>();
                values.put("customer_id", customerId);
                values.put("product_id", productId);
                values.put("created_at", FieldValue.serverTimestamp());
                firestore.collection("users")
                        .document(customerId)
                        .collection("favorites")
                        .document(productId)
                        .set(values)
                        .addOnSuccessListener(unused -> callback.onComplete(true, null))
                        .addOnFailureListener(e -> callback.onComplete(false, friendlyMessage(e.getMessage())));
                return;
            }

            firestore.collection("users")
                    .document(customerId)
                    .collection("favorites")
                    .document(productId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, friendlyMessage(e.getMessage())));
        });
    }

    private String fallbackMessage(@Nullable String value) {
        return value == null ? "Firebase auth chưa sẵn sàng" : value;
    }

    private String friendlyMessage(@Nullable String value) {
        if (value == null) {
            return "Chưa đồng bộ được món yêu thích lên Firebase.";
        }
        String normalized = value.toLowerCase();
        if (normalized.contains("permission_denied") || normalized.contains("insufficient permissions")) {
            return "Firebase Rules chưa cho phép lưu món yêu thích. Hãy deploy lại firestore.rules.";
        }
        return value;
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
