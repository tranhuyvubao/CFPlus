package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.FirebaseProvider;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StaffSupportThreadsActivity extends AppCompatActivity {
    private final List<SupportThread> threads = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
    private ListenerRegistration threadListener;
    private FirebaseFirestore firestore;
    private RecyclerView rvThreads;
    private TextView tvEmpty;
    private ThreadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_support_threads);
        InsetsHelper.applyActivityRootPadding(this);
        rvThreads = findViewById(R.id.rvSupportThreads);
        tvEmpty = findViewById(R.id.tvSupportThreadsEmpty);
        adapter = new ThreadAdapter(threads);
        rvThreads.setLayoutManager(new LinearLayoutManager(this));
        rvThreads.setAdapter(adapter);
        findViewById(R.id.btnBackSupportThreads).setOnClickListener(v -> finish());
        firestore = FirebaseProvider.getFirestore(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseProvider.ensureAuthenticated(this, (success, message) -> runOnUiThread(() -> {
            if (!success) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Không thể kết nối Firebase chat.");
                return;
            }
            listenThreads();
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (threadListener != null) {
            threadListener.remove();
            threadListener = null;
        }
    }

    private void listenThreads() {
        if (threadListener != null) {
            threadListener.remove();
        }
        threadListener = firestore
                .collection("support_chat_threads")
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((value, error) -> runOnUiThread(() -> {
                    threads.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            threads.add(new SupportThread(
                                    fallback(doc.getString("threadId"), doc.getId()),
                                    fallback(doc.getString("customerId"), ""),
                                    fallback(doc.getString("customerName"), "Khách hàng"),
                                    fallback(doc.getString("lastMessage"), "Chưa có tin nhắn"),
                                    longValue(doc.get("updatedAt"))
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(threads.isEmpty() ? View.VISIBLE : View.GONE);
                }));
    }

    private long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return System.currentTimeMillis();
    }

    @NonNull
    private String fallback(String value, String backup) {
        return TextUtils.isEmpty(value) ? backup : value.trim();
    }

    private class ThreadAdapter extends RecyclerView.Adapter<ThreadVH> {
        private final List<SupportThread> items;

        ThreadAdapter(List<SupportThread> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ThreadVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_staff_support_thread, parent, false);
            return new ThreadVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ThreadVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class ThreadVH extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvLastMessage;
        private final TextView tvMeta;

        ThreadVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvThreadTitle);
            tvLastMessage = itemView.findViewById(R.id.tvThreadLastMessage);
            tvMeta = itemView.findViewById(R.id.tvThreadMeta);
        }

        void bind(SupportThread thread) {
            tvTitle.setText(thread.customerName);
            tvLastMessage.setText(thread.lastMessage);
            tvMeta.setText("Cập nhật: " + timeFormat.format(new Date(thread.updatedAt)));
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(StaffSupportThreadsActivity.this, SupportChatActivity.class);
                intent.putExtra(SupportChatActivity.EXTRA_CUSTOMER_ID, thread.customerId);
                intent.putExtra(SupportChatActivity.EXTRA_CUSTOMER_NAME, thread.customerName);
                startActivity(intent);
            });
        }
    }

    private static class SupportThread {
        final String threadId;
        final String customerId;
        final String customerName;
        final String lastMessage;
        final long updatedAt;

        SupportThread(String threadId, String customerId, String customerName, String lastMessage, long updatedAt) {
            this.threadId = threadId;
            this.customerId = customerId;
            this.customerName = customerName;
            this.lastMessage = lastMessage;
            this.updatedAt = updatedAt;
        }
    }
}
