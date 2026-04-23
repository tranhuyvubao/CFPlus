package com.example.do_an_hk1_androidstudio.ui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.do_an_hk1_androidstudio.R;
import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.room.CfPlusLocalDatabase;
import com.example.do_an_hk1_androidstudio.local.room.NotificationInboxEntity;

public final class NotificationCenter {

    public static final String CHANNEL_ID = "cfplus_orders";

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
        Context appContext = context.getApplicationContext();
        ensureChannel(appContext);
        NotificationInboxEntity entity = new NotificationInboxEntity();
        entity.id = DataHelper.newId("notif");
        entity.title = title;
        entity.body = body;
        entity.type = type;
        entity.orderId = orderId;
        entity.status = status;
        entity.createdAt = System.currentTimeMillis();
        entity.read = false;
        CfPlusLocalDatabase.getInstance(appContext).notificationInboxDao().insert(entity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManagerCompat.from(appContext).notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
    }
}
