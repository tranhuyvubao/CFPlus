package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.payment.VnpayConfig;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class VnpayConfigCloudRepository {

    public interface ConfigCallback {
        void onResult(@Nullable MerchantConfig config, @Nullable String message);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private static final String SETTINGS_COLLECTION = "settings";
    private static final String VNPAY_DOCUMENT = "vnpay_sandbox";

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public VnpayConfigCloudRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public void getConfig(@NonNull ConfigCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onResult(null, fallbackMessage(message));
                return;
            }
            firestore.collection(SETTINGS_COLLECTION)
                    .document(VNPAY_DOCUMENT)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot == null || !snapshot.exists()) {
                            callback.onResult(null, null);
                            return;
                        }
                        callback.onResult(new MerchantConfig(
                                valueOrEmpty(snapshot.getString("tmn_code")),
                                valueOrEmpty(snapshot.getString("hash_secret")),
                                valueOrDefault(snapshot.getString("return_url"), VnpayConfig.DEFAULT_RETURN_URL)
                        ), null);
                    })
                    .addOnFailureListener(e -> callback.onResult(null, friendlyMessage(e.getMessage())));
        });
    }

    public void saveConfig(@NonNull String tmnCode,
                           @NonNull String hashSecret,
                           @NonNull String returnUrl,
                           @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            Map<String, Object> values = new HashMap<>();
            values.put("tmn_code", tmnCode.trim());
            values.put("hash_secret", hashSecret.trim());
            values.put("return_url", TextUtils.isEmpty(returnUrl.trim()) ? VnpayConfig.DEFAULT_RETURN_URL : returnUrl.trim());
            values.put("environment", "sandbox");
            values.put("updated_at", FieldValue.serverTimestamp());
            firestore.collection(SETTINGS_COLLECTION)
                    .document(VNPAY_DOCUMENT)
                    .set(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, friendlyMessage(e.getMessage())));
        });
    }

    public void clearConfig(@NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            firestore.collection(SETTINGS_COLLECTION)
                    .document(VNPAY_DOCUMENT)
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
            return "Không thể kết nối Firebase.";
        }
        String normalized = value.toLowerCase();
        if (normalized.contains("permission_denied") || normalized.contains("insufficient permissions")) {
            return "Firebase Rules chưa cho phép đọc/ghi cấu hình. Hãy deploy lại firestore.rules.";
        }
        return value;
    }

    private String valueOrEmpty(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOrDefault(@Nullable String value, @NonNull String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value.trim();
    }

    public static class MerchantConfig {
        public final String tmnCode;
        public final String hashSecret;
        public final String returnUrl;

        public MerchantConfig(@Nullable String tmnCode, @Nullable String hashSecret, @Nullable String returnUrl) {
            this.tmnCode = tmnCode == null ? "" : tmnCode.trim();
            this.hashSecret = hashSecret == null ? "" : hashSecret.trim();
            this.returnUrl = TextUtils.isEmpty(returnUrl) ? VnpayConfig.DEFAULT_RETURN_URL : returnUrl.trim();
        }
    }
}
