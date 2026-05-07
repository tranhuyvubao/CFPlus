package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.cloud.FirebaseProvider;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SupportChatActivity extends AppCompatActivity {
    public static final String EXTRA_CUSTOMER_ID = "extra_customer_id";
    public static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";
    public static final String EXTRA_SEED_MESSAGE = "extra_seed_message";
    private static final String CHAT_DRAFT_PREF = "support_chat_draft_pref";
    private static final String CHAT_DRAFT_KEY_PREFIX = "draft_";

    private final List<SupportMessage> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());

    private FirebaseFirestore firestore;
    private LocalSessionManager sessionManager;
    private String currentUserId;
    private String currentRole;
    private String customerId;
    private String customerName;
    private String threadId;
    private ListenerRegistration messageListener;
    private RecyclerView rvMessages;
    private EditText edtMessage;
    private TextView tvTitle;
    private SupportMessageAdapter adapter;
    private boolean seededFirstMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_chat);
        InsetsHelper.applyActivityRootPadding(this);

        firestore = FirebaseProvider.getFirestore(this);
        sessionManager = new LocalSessionManager(this);
        currentUserId = sessionManager.getCurrentUserId();
        currentRole = sessionManager.getCurrentUserRole();

        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Vui lòng đăng nhập để chat hỗ trợ.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        resolveThreadContext();
        if (TextUtils.isEmpty(customerId)) {
            Toast.makeText(this, "Không xác định được cuộc trò chuyện hỗ trợ.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvMessages = findViewById(R.id.rvSupportMessages);
        edtMessage = findViewById(R.id.edtSupportMessage);
        tvTitle = findViewById(R.id.tvSupportChatTitle);
        adapter = new SupportMessageAdapter(messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnBackSupportChat).setOnClickListener(v -> finish());
        findViewById(R.id.btnSendSupportMessage).setOnClickListener(v -> sendMessage());
        restoreDraftIfAny();

        tvTitle.setText("staff".equals(currentRole)
                ? "Chat với " + fallback(customerName, customerId)
                : "Chat với nhân viên");

        FirebaseProvider.ensureAuthenticated(this, (success, message) -> runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(this, message == null ? "Không kết nối được phiên đăng nhập Firebase." : message, Toast.LENGTH_LONG).show();
                return;
            }
            ensureThread(() -> {
                listenMessages();
                String seedMessage = getIntent().getStringExtra(EXTRA_SEED_MESSAGE);
                if (!TextUtils.isEmpty(seedMessage) && !seededFirstMessage) {
                    seededFirstMessage = true;
                    sendMessageInternal(seedMessage.trim());
                }
            });
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistDraft();
    }

    private void resolveThreadContext() {
        if ("staff".equals(currentRole)) {
            customerId = getIntent().getStringExtra(EXTRA_CUSTOMER_ID);
            customerName = getIntent().getStringExtra(EXTRA_CUSTOMER_NAME);
        } else {
            customerId = currentUserId;
            customerName = sessionManager.getCurrentUserFullName();
        }
        threadId = "support_" + customerId;
    }

    private void ensureThread(@NonNull Runnable onDone) {
        Map<String, Object> values = new HashMap<>();
        values.put("threadId", threadId);
        values.put("customerId", customerId);
        values.put("customerName", fallback(customerName, "Khách hàng"));
        values.put("status", "open");
        values.put("updatedAt", System.currentTimeMillis());
        firestore.collection("support_chat_threads")
                .document(threadId)
                .set(values, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> onDone.run())
                .addOnFailureListener(e -> onDone.run());
    }

    private void listenMessages() {
        if (messageListener != null) {
            messageListener.remove();
        }
        messageListener = firestore.collection("support_chat_threads")
                .document(threadId)
                .collection("messages")
                .orderBy("createdAt")
                .addSnapshotListener((value, error) -> runOnUiThread(() -> {
                    messages.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            messages.add(new SupportMessage(
                                    fallback(doc.getString("senderId"), ""),
                                    fallback(doc.getString("senderRole"), "customer"),
                                    fallback(doc.getString("senderName"), "Người dùng"),
                                    fallback(doc.getString("content"), ""),
                                    longValue(doc.get("createdAt"))
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                }));
    }

    private void sendMessage() {
        String content = edtMessage.getText() == null ? "" : edtMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }
        sendMessageInternal(content);
        edtMessage.setText("");
        clearDraft();
    }

    private void restoreDraftIfAny() {
        String draft = getSharedPreferences(CHAT_DRAFT_PREF, MODE_PRIVATE)
                .getString(getDraftKey(), "");
        if (!TextUtils.isEmpty(draft) && edtMessage != null) {
            edtMessage.setText(draft);
            edtMessage.setSelection(draft.length());
        }
    }

    private void persistDraft() {
        if (edtMessage == null) {
            return;
        }
        String draft = edtMessage.getText() == null ? "" : edtMessage.getText().toString().trim();
        getSharedPreferences(CHAT_DRAFT_PREF, MODE_PRIVATE)
                .edit()
                .putString(getDraftKey(), draft)
                .apply();
    }

    private void clearDraft() {
        getSharedPreferences(CHAT_DRAFT_PREF, MODE_PRIVATE)
                .edit()
                .remove(getDraftKey())
                .apply();
    }

    @NonNull
    private String getDraftKey() {
        return CHAT_DRAFT_KEY_PREFIX + (TextUtils.isEmpty(threadId) ? "unknown" : threadId);
    }

    private void sendMessageInternal(@NonNull String content) {
        long now = System.currentTimeMillis();
        String messageId = DataHelper.newId("support_msg");
        String senderName = "staff".equals(currentRole)
                ? fallback(sessionManager.getCurrentUserFullName(), "Nhân viên")
                : fallback(sessionManager.getCurrentUserFullName(), "Khách hàng");

        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("messageId", messageId);
        messageValues.put("senderId", currentUserId);
        messageValues.put("senderRole", currentRole);
        messageValues.put("senderName", senderName);
        messageValues.put("content", content);
        messageValues.put("createdAt", now);

        Map<String, Object> threadValues = new HashMap<>();
        threadValues.put("lastMessage", content);
        threadValues.put("lastSenderRole", currentRole);
        threadValues.put("updatedAt", now);
        threadValues.put("status", "open");

        firestore.collection("support_chat_threads")
                .document(threadId)
                .collection("messages")
                .document(messageId)
                .set(messageValues)
                .continueWithTask(task -> firestore.collection("support_chat_threads")
                        .document(threadId)
                        .set(threadValues, com.google.firebase.firestore.SetOptions.merge()))
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Không gửi được tin nhắn: " + (e.getMessage() == null ? "Lỗi không xác định." : e.getMessage()),
                        Toast.LENGTH_LONG
                ).show());
    }

    private long longValue(@Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return System.currentTimeMillis();
    }

    @NonNull
    private String fallback(@Nullable String value, @NonNull String backup) {
        return TextUtils.isEmpty(value) ? backup : value.trim();
    }

    private class SupportMessageAdapter extends RecyclerView.Adapter<SupportMessageViewHolder> {
        private final List<SupportMessage> items;

        SupportMessageAdapter(List<SupportMessage> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public SupportMessageViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_support_message, parent, false);
            return new SupportMessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SupportMessageViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class SupportMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSender;
        private final TextView tvContent;
        private final TextView tvTime;

        SupportMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSupportSender);
            tvContent = itemView.findViewById(R.id.tvSupportContent);
            tvTime = itemView.findViewById(R.id.tvSupportTime);
        }

        void bind(SupportMessage message) {
            boolean mine = currentUserId.equals(message.senderId);
            tvSender.setText((mine ? "Bạn" : message.senderName) + " • " + ("staff".equals(message.senderRole) ? "Nhân viên" : "Khách"));
            tvContent.setText(message.content);
            tvTime.setText(timeFormat.format(new Date(message.createdAt)));
            tvContent.setBackgroundResource(mine ? R.drawable.chat_bubble_mine : R.drawable.chat_bubble_other);
            tvContent.setTextColor(ContextCompat.getColor(
                    SupportChatActivity.this,
                    mine ? android.R.color.white : R.color.dashboard_text_primary
            ));

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) itemView.getLayoutParams();
            if (params == null) {
                params = new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                );
            }
            params.setMargins(0, 0, 0, dp(2));
            itemView.setLayoutParams(params);

            LinearLayout container = itemView.findViewById(R.id.layoutMessageContainer);
            if (container != null) {
                container.setGravity(mine ? android.view.Gravity.END : android.view.Gravity.START);
            }
            tvSender.setTextAlignment(mine ? View.TEXT_ALIGNMENT_VIEW_END : View.TEXT_ALIGNMENT_VIEW_START);
            tvTime.setTextAlignment(mine ? View.TEXT_ALIGNMENT_VIEW_END : View.TEXT_ALIGNMENT_VIEW_START);
        }
    }

    private static class SupportMessage {
        final String senderId;
        final String senderRole;
        final String senderName;
        final String content;
        final long createdAt;

        SupportMessage(String senderId, String senderRole, String senderName, String content, long createdAt) {
            this.senderId = senderId;
            this.senderRole = senderRole;
            this.senderName = senderName;
            this.content = content;
            this.createdAt = createdAt;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
