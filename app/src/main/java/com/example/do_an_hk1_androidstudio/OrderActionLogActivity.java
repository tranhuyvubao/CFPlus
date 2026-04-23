package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.OrderActionLogRepository;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderActionLogActivity extends AppCompatActivity {

    private final List<OrderActionLogRepository.OrderActionLogItem> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
    private ListenerRegistration listenerRegistration;
    private LogAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_action_log);
        InsetsHelper.applyActivityRootPadding(this);

        RecyclerView recyclerView = findViewById(R.id.rvOrderActionLogs);
        tvEmpty = findViewById(R.id.tvOrderActionLogsEmpty);
        findViewById(R.id.btnOrderLogsBack).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenerRegistration = new OrderActionLogRepository(this).listenLogs(logs -> runOnUiThread(() -> {
            items.clear();
            items.addAll(logs);
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(items.isEmpty() ? TextView.VISIBLE : TextView.GONE);
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {
        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_order_log, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAction;
        private final TextView tvActor;
        private final TextView tvMeta;

        LogViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            tvAction = itemView.findViewById(R.id.tvOrderLogAction);
            tvActor = itemView.findViewById(R.id.tvOrderLogActor);
            tvMeta = itemView.findViewById(R.id.tvOrderLogMeta);
        }

        void bind(OrderActionLogRepository.OrderActionLogItem item) {
            tvAction.setText(item.action);
            tvActor.setText(item.actorName.isEmpty() ? "Hệ thống nội bộ" : item.actorName);
            String meta = "Đơn " + item.orderId + "  •  " + timeFormat.format(item.createdAt);
            if (!item.note.isEmpty()) {
                meta += "\n" + item.note;
            }
            tvMeta.setText(meta);
        }
    }
}
