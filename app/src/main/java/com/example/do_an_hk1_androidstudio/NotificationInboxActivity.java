package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.room.CfPlusLocalDatabase;
import com.example.do_an_hk1_androidstudio.local.room.NotificationInboxEntity;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.NotificationCenter;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationInboxActivity extends AppCompatActivity {

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private RecyclerView recyclerView;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_inbox);
        InsetsHelper.applyStatusBarPadding(findViewById(R.id.headerNotificationInbox));
        InsetsHelper.applyNavigationBarPadding(findViewById(R.id.rootNotificationInbox));

        recyclerView = findViewById(R.id.rvNotificationInbox);
        tvEmpty = findViewById(R.id.tvNotificationInboxEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnNotificationBack).setOnClickListener(v -> finish());
        bindNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        markAllAsRead();
        bindNotifications();
    }

    private void markAllAsRead() {
        String userId = new LocalSessionManager(this).getCurrentUserId();
        if (userId == null) {
            CfPlusLocalDatabase.getInstance(this).notificationInboxDao().markAllRead();
        } else {
            CfPlusLocalDatabase.getInstance(this).notificationInboxDao().markAllReadForUser(userId);
        }
        NotificationManagerCompat.from(this).cancelAll();
        sendBroadcast(new android.content.Intent(NotificationCenter.ACTION_INBOX_UPDATED)
                .setPackage(getPackageName()));
    }

    private void bindNotifications() {
        String userId = new LocalSessionManager(this).getCurrentUserId();
        List<NotificationInboxEntity> notifications = userId == null
                ? CfPlusLocalDatabase.getInstance(this).notificationInboxDao().getAll()
                : CfPlusLocalDatabase.getInstance(this).notificationInboxDao().getAllForUser(userId);
        recyclerView.setAdapter(new InboxAdapter(notifications));
        tvEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class InboxAdapter extends RecyclerView.Adapter<InboxViewHolder> {
        private final List<NotificationInboxEntity> items;

        InboxAdapter(List<NotificationInboxEntity> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public InboxViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_notification_inbox, parent, false);
            return new InboxViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InboxViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class InboxViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvBody;
        private final TextView tvMeta;

        InboxViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvInboxTitle);
            tvBody = itemView.findViewById(R.id.tvInboxBody);
            tvMeta = itemView.findViewById(R.id.tvInboxMeta);
        }

        void bind(NotificationInboxEntity item) {
            tvTitle.setText(item.title);
            tvBody.setText(item.body);
            String meta = timeFormat.format(item.createdAt);
            if (item.orderId != null && !item.orderId.isEmpty()) {
                meta += " • Mã đơn " + toDisplayOrderCode(item.orderId, item.createdAt);
            }
            tvMeta.setText(meta);
            itemView.setAlpha(item.read ? 0.76f : 1f);
            itemView.setOnClickListener(v -> {
                CfPlusLocalDatabase.getInstance(NotificationInboxActivity.this)
                        .notificationInboxDao()
                        .markRead(item.id);
                item.read = true;
                sendBroadcast(new android.content.Intent(NotificationCenter.ACTION_INBOX_UPDATED)
                        .setPackage(getPackageName()));
                RecyclerView.Adapter<?> adapter = getBindingAdapter();
                if (adapter != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(getBindingAdapterPosition());
                }
            });
        }
    }

    private String toDisplayOrderCode(@NonNull String orderId, long fallbackTimestamp) {
        String normalized = orderId.trim();
        if (normalized.isEmpty()) {
            return DataHelper.newOrderCode(fallbackTimestamp);
        }
        if (normalized.startsWith("web_order_")
                || normalized.startsWith("cloud_order_")
                || normalized.startsWith("online_order_")
                || normalized.startsWith("order_")) {
            return DataHelper.newOrderCode(fallbackTimestamp);
        }
        if (normalized.length() > 28) {
            return normalized.substring(0, 28) + "...";
        }
        return normalized;
    }
}
