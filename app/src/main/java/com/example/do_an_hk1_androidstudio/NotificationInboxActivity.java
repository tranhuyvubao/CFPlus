package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.local.room.CfPlusLocalDatabase;
import com.example.do_an_hk1_androidstudio.local.room.NotificationInboxEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationInboxActivity extends AppCompatActivity {

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_inbox);

        RecyclerView recyclerView = findViewById(R.id.rvNotificationInbox);
        TextView tvEmpty = findViewById(R.id.tvNotificationInboxEmpty);
        findViewById(R.id.btnNotificationBack).setOnClickListener(v -> finish());

        List<NotificationInboxEntity> notifications = CfPlusLocalDatabase.getInstance(this)
                .notificationInboxDao()
                .getAll();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new InboxAdapter(notifications));
        tvEmpty.setVisibility(notifications.isEmpty() ? TextView.VISIBLE : TextView.GONE);
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
                meta += "  •  " + item.orderId;
            }
            tvMeta.setText(meta);
            itemView.setAlpha(item.read ? 0.74f : 1f);
            itemView.setOnClickListener(v -> {
                CfPlusLocalDatabase.getInstance(NotificationInboxActivity.this)
                        .notificationInboxDao()
                        .markRead(item.id);
                item.read = true;
                RecyclerView.Adapter<?> adapter = getBindingAdapter();
                if (adapter != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(getBindingAdapterPosition());
                }
            });
        }
    }
}
