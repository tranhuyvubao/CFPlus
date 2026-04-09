package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.model.LocalCategory;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
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

public class CatalogCloudRepository {

    public interface CategoriesCallback {
        void onChanged(@NonNull List<LocalCategory> categories);
    }

    public interface ProductsCallback {
        void onChanged(@NonNull List<LocalProduct> products);
    }

    public interface ProductCallback {
        void onResult(@Nullable LocalProduct product, @Nullable String message);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public CatalogCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public ListenerRegistration listenCategories(@NonNull CategoriesCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("categories")
                    .addSnapshotListener((value, error) -> {
                        List<LocalCategory> categories = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                categories.add(mapCategory(snapshot));
                            }
                        }
                        categories.sort(Comparator.comparing(LocalCategory::getName, String::compareToIgnoreCase));
                        callback.onChanged(categories);
                    }));
        });
        return holder;
    }

    public ListenerRegistration listenProducts(@NonNull ProductsCallback callback) {
        ListenerRegistrationHolder holder = new ListenerRegistrationHolder();
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onChanged(new ArrayList<>());
                return;
            }
            holder.setDelegate(firestore.collection("products")
                    .addSnapshotListener((value, error) -> {
                        List<LocalProduct> products = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                products.add(mapProduct(snapshot));
                            }
                        }
                        products.sort(Comparator.comparing(LocalProduct::getName, String::compareToIgnoreCase));
                        callback.onChanged(products);
                    }));
        });
        return holder;
    }

    public void getProductById(String productId, @NonNull ProductCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onResult(null, fallbackMessage(message));
                return;
            }
            firestore.collection("products")
                    .document(productId)
                    .get()
                    .addOnSuccessListener(snapshot -> callback.onResult(snapshot.exists() ? mapProduct(snapshot) : null, null))
                    .addOnFailureListener(e -> callback.onResult(null, e.getMessage()));
        });
    }

    public void addCategory(String name, @Nullable String imageUrl, boolean active, @NonNull CompletionCallback callback) {
        String id = DataHelper.newId("cat");
        long now = System.currentTimeMillis();
        Map<String, Object> values = new HashMap<>();
        values.put("category_id", id);
        values.put("name", safe(name));
        values.put("image_url", normalizeNullable(imageUrl));
        values.put("is_active", active);
        values.put("created_at", now);
        values.put("updated_at", now);
        saveDocument("categories", id, values, callback);
    }

    public void updateCategory(String categoryId, String name, @Nullable String imageUrl, boolean active, @NonNull CompletionCallback callback) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", safe(name));
        values.put("image_url", normalizeNullable(imageUrl));
        values.put("is_active", active);
        values.put("updated_at", FieldValue.serverTimestamp());
        updateDocument("categories", categoryId, values, callback);
    }

    public void deleteCategory(String categoryId, @NonNull CompletionCallback callback) {
        deleteDocument("categories", categoryId, callback);
    }

    public void addProduct(String name,
                           int basePrice,
                           @Nullable String imageUrl,
                           String categoryId,
                           boolean active,
                           @NonNull CompletionCallback callback) {
        String id = DataHelper.newId("product");
        long now = System.currentTimeMillis();
        Map<String, Object> values = new HashMap<>();
        values.put("product_id", id);
        values.put("name", safe(name));
        values.put("base_price", basePrice);
        values.put("image_url", normalizeNullable(imageUrl));
        values.put("category_id", categoryId);
        values.put("is_active", active);
        values.put("created_at", now);
        values.put("updated_at", now);
        saveDocument("products", id, values, callback);
    }

    public void updateProduct(String productId,
                              String name,
                              int basePrice,
                              @Nullable String imageUrl,
                              String categoryId,
                              boolean active,
                              @NonNull CompletionCallback callback) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", safe(name));
        values.put("base_price", basePrice);
        values.put("image_url", normalizeNullable(imageUrl));
        values.put("category_id", categoryId);
        values.put("is_active", active);
        values.put("updated_at", FieldValue.serverTimestamp());
        updateDocument("products", productId, values, callback);
    }

    public void deleteProduct(String productId, @NonNull CompletionCallback callback) {
        deleteDocument("products", productId, callback);
    }

    private void saveDocument(String collectionName, String documentId, Map<String, Object> values, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection(collectionName)
                    .document(documentId)
                    .set(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private void updateDocument(String collectionName, String documentId, Map<String, Object> values, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection(collectionName)
                    .document(documentId)
                    .update(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private void deleteDocument(String collectionName, String documentId, @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection(collectionName)
                    .document(documentId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private LocalCategory mapCategory(DocumentSnapshot snapshot) {
        return new LocalCategory(
                stringValue(snapshot.getString("category_id"), snapshot.getId()),
                stringValue(snapshot.getString("name"), ""),
                snapshot.getString("image_url"),
                boolValue(snapshot.get("is_active"), true)
        );
    }

    private LocalProduct mapProduct(DocumentSnapshot snapshot) {
        return new LocalProduct(
                stringValue(snapshot.getString("product_id"), snapshot.getId()),
                stringValue(snapshot.getString("category_id"), ""),
                stringValue(snapshot.getString("name"), ""),
                intValue(snapshot.get("base_price")),
                snapshot.getString("image_url"),
                boolValue(snapshot.get("is_active"), true)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Nullable
    private String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private String stringValue(@Nullable String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
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
