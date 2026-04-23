package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KdsBarActivity extends AppCompatActivity {

    private final List<LocalOrder> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private OrderCloudRepository orderRepository;
    private ListenerRegistration listenerRegistration;
    private KdsAdapter adapter;
    private TextView tvEmpty;
    private String staffId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kds_bar);
        InsetsHelper.applyActivityRootPadding(this);

        staffId = new LocalSessionManager(this).getCurrentUserId();
        orderRepository = new OrderCloudRepository(this);

        RecyclerView recyclerView = findViewById(R.id.rvKdsOrders);
        tvEmpty = findViewById(R.id.tvKdsEmpty);
        findViewById(R.id.btnKdsBack).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new KdsAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenerRegistration = orderRepository.listenAllOrders(orders -> runOnUiThread(() -> {
            items.clear();
            for (LocalOrder order : orders) {
                if ("created".equals(order.getStatus()) || "confirmed".equals(order.getStatus())) {
                    items.add(order);
                }
            }
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

    private class KdsAdapter extends RecyclerView.Adapter<KdsViewHolder> {
        @NonNull
        @Override
        public KdsViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_kds_order, parent, false);
            return new KdsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull KdsViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class KdsViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvMeta;
        private final TextView tvItems;
        private final TextView btnAction;

        KdsViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvKdsTitle);
            tvMeta = itemView.findViewById(R.id.tvKdsMeta);
            tvItems = itemView.findViewById(R.id.tvKdsItems);
            btnAction = itemView.findViewById(R.id.btnKdsAction);
        }

        void bind(LocalOrder order) {
            tvTitle.setText("Đơn " + order.getDisplayOrderCode());
            String channel;
            if ("online".equals(order.getOrderType())) {
                channel = "Khách app";
            } else if (TableCloudRepository.TAKEAWAY_TABLE_ID.equals(order.getTableId()) || "takeaway".equals(order.getOrderType())) {
                channel = "Mang về";
            } else {
                channel = "Tại bàn";
            }
            String meta = channel + "  •  " + timeFormat.format(order.getCreatedAtMillis()) + "  •  " + MoneyFormatter.format(order.getTotal());
            tvMeta.setText(meta);
            tvItems.setText(buildItemSummary(order));
            btnAction.setText("created".equals(order.getStatus()) ? "Đang làm" : "Xong");
            btnAction.setOnClickListener(v -> {
                String nextStatus = "created".equals(order.getStatus()) ? "confirmed" : "paid";
                orderRepository.updateOrderStatus(order.getOrderId(), nextStatus, staffId, (success, message) -> runOnUiThread(() -> {
                    Toast.makeText(
                            KdsBarActivity.this,
                            success ? "Đã cập nhật trạng thái." : (message == null ? "Không thể cập nhật." : message),
                            Toast.LENGTH_SHORT
                    ).show();
                }));
            });
        }

        @NonNull
        private String buildItemSummary(LocalOrder order) {
            List<String> lines = new ArrayList<>();
            order.getItems().forEach(item -> {
                String line = item.getQty() + "x " + item.getProductName();
                if (!TextUtils.isEmpty(item.getVariantName())) {
                    line += " • " + item.getVariantName();
                }
                lines.add(line);
            });
            if (lines.isEmpty() && !TextUtils.isEmpty(order.getOrderCode())) {
                lines.add("Đơn đang chờ xử lý");
            }
            return TextUtils.join("\n", lines);
        }
    }
}
