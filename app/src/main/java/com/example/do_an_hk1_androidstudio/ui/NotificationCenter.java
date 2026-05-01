package com.example.do_an_hk1_androidstudio.ui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.do_an_hk1_androidstudio.NotificationInboxActivity;
import com.example.do_an_hk1_androidstudio.R;
import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.room.CfPlusLocalDatabase;
import com.example.do_an_hk1_androidstudio.local.room.NotificationInboxEntity;

public final class NotificationCenter {

    public static final String CHANNEL_ID = "cfplus_orders";
    public static final String ACTION_INBOX_UPDATED = "com.example.do_an_hk1_androidstudio.ACTION_INBOX_UPDATED";

    private NotificationCenter() {
    }

    public static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Trạng thái đơn hàng",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Thông báo cập nhật đơn hàng và thao tác vận hành");
        notificationManager.createNotificationChannel(channel);
    }

    public static void storeAndShow(@NonNull Context context,
                                    @NonNull String title,
                                    @NonNull String body,
                                    @NonNull String type,
                                    String orderId,
                                    String status) {
        LocalSessionManager sessionManager = new LocalSessionManager(context.getApplicationContext());
        storeAndShow(
                context,
                DataHelper.newId("notif"),
                null,
                sessionManager.getCurrentUserId(),
                title,
                body,
                type,
                orderId,
                status,
                true
        );
    }

    public static void storeAndShow(@NonNull Context context,
                                    @NonNull String notificationId,
                                    @Nullable String eventKey,
                                    @Nullable String userId,
                                    @NonNull String title,
                                    @NonNull String body,
                                    @NonNull String type,
                                    @Nullable String orderId,
                                    @Nullable String status) {
        storeAndShow(context, notificationId, eventKey, userId, title, body, type, orderId, status, true);
    }

    public static void storeAndShow(@NonNull Context context,
                                    @NonNull String notificationId,
                                    @Nullable String eventKey,
                                    @Nullable String userId,
                                    @NonNull String title,
                                    @NonNull String body,
                                    @NonNull String type,
                                    @Nullable String orderId,
                                    @Nullable String status,
                                    boolean showSystemNotification) {
        Context appContext = context.getApplicationContext();
        ensureChannel(appContext);
        String localNotificationId = userId == null || userId.trim().isEmpty()
                ? notificationId
                : notificationId + "_" + userId.trim();
        if (CfPlusLocalDatabase.getInstance(appContext).notificationInboxDao().exists(localNotificationId)) {
            return;
        }
        NotificationInboxEntity entity = new NotificationInboxEntity();
        entity.id = localNotificationId;
        entity.title = title;
        entity.body = body;
        entity.type = type;
        entity.userId = userId;
        entity.orderId = orderId;
        entity.eventKey = eventKey;
        entity.status = status;
        entity.createdAt = System.currentTimeMillis();
        entity.read = false;
        CfPlusLocalDatabase.getInstance(appContext).notificationInboxDao().insert(entity);
        appContext.sendBroadcast(new Intent(ACTION_INBOX_UPDATED).setPackage(appContext.getPackageName()));

        if (!showSystemNotification) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent inboxIntent = new Intent(appContext, NotificationInboxActivity.class);
        inboxIntent.putExtra("orderId", orderId);
        inboxIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                appContext,
                localNotificationId.hashCode(),
                inboxIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat.from(appContext).notify(localNotificationId.hashCode(), builder.build());
    }
}
