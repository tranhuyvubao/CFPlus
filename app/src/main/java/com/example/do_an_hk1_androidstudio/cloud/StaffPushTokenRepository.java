package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;

public class StaffPushTokenRepository {
    public static final String STAFF_TOPIC = "staff_only";

    private final Context appContext;
    private final FirebaseFirestore firestore;
    private final LocalSessionManager sessionManager;

    public StaffPushTokenRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
        sessionManager = new LocalSessionManager(appContext);
    }

    public void syncForCurrentSession() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(this::syncExplicitTokenForCurrentSession);
    }

    public void syncExplicitTokenForCurrentSession(@Nullable String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        String role = sessionManager.getCurrentUserRole();
        if (!"staff".equals(role)) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(STAFF_TOPIC);
            markTokenInactive(token);
            return;
        }
        FirebaseMessaging.getInstance().subscribeToTopic(STAFF_TOPIC);
        saveToken(token);
    }

    public void deactivateCurrentSessionToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(STAFF_TOPIC);
                    markTokenInactive(token);
                });
    }

    private void saveToken(@NonNull String token) {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                return;
            }
            String documentId = userId + "_" + DataHelper.sha256(token).substring(0, 16);
            Map<String, Object> values = new HashMap<>();
            values.put("userId", userId);
            values.put("role", "staff");
            values.put("token", token);
            values.put("platform", "android");
            values.put("topic", STAFF_TOPIC);
            values.put("active", true);
            values.put("updatedAt", FieldValue.serverTimestamp());
            firestore.collection("staff_device_tokens")
                    .document(documentId)
                    .set(values);
        });
    }

    private void markTokenInactive(@Nullable String token) {
        String userId = sessionManager.getCurrentUserId();
        if (token == null || token.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            return;
        }
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                return;
            }
            String documentId = userId + "_" + DataHelper.sha256(token).substring(0, 16);
            Map<String, Object> values = new HashMap<>();
            values.put("active", false);
            values.put("updatedAt", FieldValue.serverTimestamp());
            firestore.collection("staff_device_tokens")
                    .document(documentId)
                    .set(values, com.google.firebase.firestore.SetOptions.merge());
        });
    }
}
