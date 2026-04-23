package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.MimeTypeMap;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public interface ImageUploadCallback {
        void onComplete(boolean success, @Nullable String imageUrl, @Nullable String message);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public CatalogCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
        storage = FirebaseProvider.getStorage(appContext);
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

    public void uploadCatalogImage(@NonNull Uri sourceUri,
                                   @NonNull String folder,
                                   @NonNull ImageUploadCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, null, fallbackMessage(message));
                return;
            }
            String dataUrl = buildCatalogImageDataUrl(sourceUri);
            if (TextUtils.isEmpty(dataUrl)) {
                callback.onComplete(false, null, "Không đọc được ảnh đã chọn. Hãy chọn lại file JPG/PNG khác.");
                return;
            }
            callback.onComplete(true, dataUrl, null);
        });
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

    private String resolveExtension(@NonNull Uri uri) {
        String mimeType = appContext.getContentResolver().getType(uri);
        String extension = TextUtils.isEmpty(mimeType)
                ? null
                : MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return TextUtils.isEmpty(extension) ? "jpg" : extension;
    }

    private String resolveContentType(@NonNull Uri uri) {
        String mimeType = appContext.getContentResolver().getType(uri);
        return TextUtils.isEmpty(mimeType) ? "image/jpeg" : mimeType;
    }

    @Nullable
    private String buildCatalogImageDataUrl(@NonNull Uri uri) {
        try (InputStream stream = appContext.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                return null;
            }
            Bitmap source = BitmapFactory.decodeStream(stream);
            if (source == null) {
                return null;
            }

            Bitmap resized = resizeBitmap(source, 720);
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 78, outputStream);

            if (resized != source) {
                resized.recycle();
            }
            source.recycle();

            String encoded = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
            return "data:image/jpeg;base64," + encoded;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Bitmap resizeBitmap(@NonNull Bitmap source, int maxSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        int largestSide = Math.max(width, height);
        if (largestSide <= maxSize) {
            return source;
        }

        float scale = maxSize / (float) largestSide;
        int nextWidth = Math.max(1, Math.round(width * scale));
        int nextHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, nextWidth, nextHeight, true);
    }

    private String buildPublicStorageUrl(@NonNull StorageReference reference) {
        return "https://firebasestorage.googleapis.com/v0/b/"
                + reference.getBucket()
                + "/o/"
                + urlEncodePath(reference.getPath())
                + "?alt=media";
    }

    private String urlEncodePath(@NonNull String path) {
        try {
            return URLEncoder.encode(path, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return path.replace("/", "%2F").replace(" ", "%20");
        }
    }

    private String friendlyStorageMessage(Exception exception) {
        if (exception instanceof StorageException) {
            StorageException storageException = (StorageException) exception;
            if (storageException.getErrorCode() == StorageException.ERROR_NOT_AUTHENTICATED) {
                return "Chưa đăng nhập Firebase, không tải được ảnh.";
            }
            if (storageException.getErrorCode() == StorageException.ERROR_NOT_AUTHORIZED) {
                return "Firebase Storage chưa cho phép upload. Hãy deploy storage.rules rồi thử lại.";
            }
            if (storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return "Không lấy được link ảnh sau khi tải lên. Kiểm tra đúng Storage bucket và deploy storage.rules.";
            }
        }
        return exception == null || TextUtils.isEmpty(exception.getMessage())
                ? "Không tải được ảnh lên Firebase Storage."
                : exception.getMessage();
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
