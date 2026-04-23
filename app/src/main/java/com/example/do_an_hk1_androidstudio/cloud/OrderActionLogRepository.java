package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderActionLogRepository {

    public interface LogsCallback {
        void onChanged(List<OrderActionLogItem> items);
    }

    public static class OrderActionLogItem {
        public final String logId;
        public final String orderId;
        public final String action;
        public final String actorId;
        public final String actorName;
        public final String note;
        public final long createdAt;

        public OrderActionLogItem(String logId,
                                  String orderId,
                                  String action,
                                  String actorId,
                                  String actorName,
                                  String note,
                                  long createdAt) {
            this.logId = logId;
            this.orderId = orderId;
            this.action = action;
            this.actorId = actorId;
            this.actorName = actorName;
            this.note = note;
            this.createdAt = createdAt;
        }
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public OrderActionLogRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public void log(@NonNull String orderId,
                    @NonNull String action,
                    @Nullable String actorId,
                    @Nullable String actorName,
                    @Nullable String note) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                return;
            }
            String logId = DataHelper.newId("order_log");
            Map<String, Object> data = new HashMap<>();
            data.put("logId", logId);
            data.put("orderId", orderId);
            data.put("action", action);
            data.put("actorId", actorId);
            data.put("actorName", actorName);
            data.put("note", note);
            data.put("createdAt", System.currentTimeMillis());
            firestore.collection("order_action_logs").document(logId).set(data);
        });
    }

    public ListenerRegistration listenLogs(@NonNull LogsCallback callback) {
        return firestore.collection("order_action_logs")
                .addSnapshotListener((value, error) -> {
                    List<OrderActionLogItem> items = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            items.add(new OrderActionLogItem(
                                    valueOf(document.getString("logId")),
                                    valueOf(document.getString("orderId")),
                                    valueOf(document.getString("action")),
                                    valueOf(document.getString("actorId")),
                                    valueOf(document.getString("actorName")),
                                    valueOf(document.getString("note")),
                                    longValue(document.get("createdAt"))
                            ));
                        }
                    }
                    items.sort(Comparator.comparingLong(item -> -item.createdAt));
                    callback.onChanged(items);
                });
    }

    @NonNull
    private String valueOf(@Nullable String value) {
        return value == null ? "" : value;
    }

    private long longValue(@Nullable Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        return System.currentTimeMillis();
    }
}
