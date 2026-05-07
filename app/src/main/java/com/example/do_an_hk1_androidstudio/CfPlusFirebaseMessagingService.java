package com.example.do_an_hk1_androidstudio;

import androidx.annotation.NonNull;

import com.example.do_an_hk1_androidstudio.cloud.StaffPushTokenRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.ui.NotificationCenter;
import com.example.do_an_hk1_androidstudio.ui.ShiftAttendanceStateStore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CfPlusFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String title = valueOrDefault(remoteMessage.getData().get("title"), "CFPLUS");
        String body = valueOrDefault(remoteMessage.getData().get("body"), "Có cập nhật mới từ quán.");
        String type = valueOrDefault(remoteMessage.getData().get("type"), "order_status");
        String orderId = remoteMessage.getData().get("orderId");
        String status = remoteMessage.getData().get("status");
        String notificationId = valueOrDefault(remoteMessage.getData().get("notificationId"), remoteMessage.getMessageId() == null ? "fcm_" + System.currentTimeMillis() : remoteMessage.getMessageId());
        String eventKey = remoteMessage.getData().get("eventKey");
        LocalSessionManager sessionManager = new LocalSessionManager(this);
        String userId = sessionManager.getCurrentUserId();
        if ("staff".equals(sessionManager.getCurrentUserRole())
                && !ShiftAttendanceStateStore.isCheckedIn(this)) {
            return;
        }

        if (remoteMessage.getNotification() != null) {
            title = valueOrDefault(remoteMessage.getNotification().getTitle(), title);
            body = valueOrDefault(remoteMessage.getNotification().getBody(), body);
        }

        NotificationCenter.storeAndShow(this, notificationId, eventKey, userId, title, body, type, orderId, status);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        new StaffPushTokenRepository(this).syncExplicitTokenForCurrentSession(token);
    }

    @NonNull
    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
