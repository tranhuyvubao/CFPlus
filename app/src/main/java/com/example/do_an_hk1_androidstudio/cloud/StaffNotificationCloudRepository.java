package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffNotificationCloudRepository {
    public static final String TARGET_ROLE_STAFF = "staff";

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    public interface NotificationsCallback {
        void onChanged(@NonNull List<StaffNotificationRecord> notifications);
    }

    public static class StaffNotificationRecord {
        public final String id;
        public final String eventKey;
        public final String type;
        public final String title;
        public final String body;
        public final String orderId;
        public final String orderChannel;
        public final String tableId;
        public final String tableName;
        public final String status;
        public final long createdAt;

        public StaffNotificationRecord(String id,
                                       String eventKey,
                                       String type,
                                       String title,
                                       String body,
                                       String orderId,
                                       String orderChannel,
                                       String tableId,
                                       String tableName,
                                       String status,
                                       long createdAt) {
            this.id = id;
            this.eventKey = eventKey;
            this.type = type;
            this.title = title;
            this.body = body;
            this.orderId = orderId;
            this.orderChannel = orderChannel;
            this.tableId = tableId;
            this.tableName = tableName;
            this.status = status;
            this.createdAt = createdAt;
        }
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public StaffNotificationCloudRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public void createNotification(@NonNull String notificationId,
                                   @NonNull String eventKey,
                                   @NonNull String type,
                                   @NonNull String title,
                                   @NonNull String body,
                                   @Nullable String orderId,
                                   @Nullable String orderChannel,
                                   @Nullable String tableId,
                                   @Nullable String tableName,
                                   @Nullable String status,
                                   @NonNull CompletionCallback callback) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                callback.onComplete(false, fallbackMessage(message));
                return;
            }
            Map<String, Object> values = new HashMap<>();
            values.put("id", notificationId);
            values.put("eventKey", eventKey);
            values.put("type", type);
            values.put("title", title);
            values.put("body", body);
            values.put("orderId", normalize(orderId));
            values.put("orderChannel", normalize(orderChannel));
            values.put("tableId", normalize(tableId));
            values.put("tableName", normalize(tableName));
            values.put("status", normalize(status));
            values.put("targetRole", TARGET_ROLE_STAFF);
            values.put("createdAt", System.currentTimeMillis());
            values.put("updatedAt", FieldValue.serverTimestamp());
            values.put("pushDispatchedAt", null);
            firestore.collection("staff_notifications")
                    .document(notificationId)
                    .set(values)
                    .addOnSuccessListener(unused -> callback.onComplete(true, null))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    public ListenerRegistration listenStaffNotifications(@NonNull NotificationsCallback callback) {
        return firestore.collection("staff_notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    List<StaffNotificationRecord> result = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            if (!TARGET_ROLE_STAFF.equals(document.getString("targetRole"))) {
                                continue;
                            }
                            result.add(new StaffNotificationRecord(
                                    valueOf(document.getString("id"), document.getId()),
                                    valueOf(document.getString("eventKey"), document.getId()),
                                    valueOf(document.getString("type"), "staff_notification"),
                                    valueOf(document.getString("title"), "CFPLUS"),
                                    valueOf(document.getString("body"), "Có cập nhật mới."),
                                    document.getString("orderId"),
                                    document.getString("orderChannel"),
                                    document.getString("tableId"),
                                    document.getString("tableName"),
                                    document.getString("status"),
                                    longValue(document.get("createdAt"))
                            ));
                        }
                    }
                    callback.onChanged(result);
                });
    }

    @Nullable
    private String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @NonNull
    private String valueOf(@Nullable String value, @NonNull String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private long longValue(@Nullable Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return System.currentTimeMillis();
    }

    @NonNull
    private String fallbackMessage(@Nullable String value) {
        return value == null ? "Firebase chưa sẵn sàng" : value;
    }
}
