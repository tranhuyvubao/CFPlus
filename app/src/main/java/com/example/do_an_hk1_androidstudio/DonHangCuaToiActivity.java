package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DonHangCuaToiActivity extends AppCompatActivity {

    private final List<LocalOrder> orders = new ArrayList<>();
    private MyOrderAdapter adapter;
    private TextView tvEmpty;
    private OrderCloudRepository orderRepository;
    private String currentUserId;
    private ListenerRegistration orderListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_don_hang_cua_toi);
        InsetsHelper.applyActivityRootPadding(this);

        currentUserId = new LocalSessionManager(this).getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderRepository = new OrderCloudRepository(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        RecyclerView rvMyOrders = findViewById(R.id.rvMyOrders);
        tvEmpty = findViewById(R.id.tvEmptyMyOrders);
        rvMyOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyOrderAdapter(orders);
        rvMyOrders.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUserId != null) {
            loadMyOrders();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }

    private void loadMyOrders() {
        if (orderListener != null) {
            orderListener.remove();
        }
        orderListener = orderRepository.listenOrdersByCustomer(currentUserId, fetchedOrders -> runOnUiThread(() -> {
            orders.clear();
            orders.addAll(fetchedOrders);
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    private String fmtTime(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    private class MyOrderAdapter extends RecyclerView.Adapter<MyOrderVH> {
        private final List<LocalOrder> data;

        MyOrderAdapter(List<LocalOrder> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public MyOrderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_order, parent, false);
            return new MyOrderVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyOrderVH holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class MyOrderVH extends RecyclerView.ViewHolder {
        private final TextView tvOrderId;
        private final TextView tvStatus;
        private final TextView tvTotal;
        private final TextView tvTime;
        private final TextView tvItemsPreview;
        private final TextView btnPay;

        MyOrderVH(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvMyOrderId);
            tvStatus = itemView.findViewById(R.id.tvMyOrderStatus);
            tvTotal = itemView.findViewById(R.id.tvMyOrderTotal);
            tvTime = itemView.findViewById(R.id.tvMyOrderTime);
            tvItemsPreview = itemView.findViewById(R.id.tvMyOrderItems);
            btnPay = itemView.findViewById(R.id.btnPayMyOrder);
        }

        void bind(LocalOrder order) {
            String status = order.getStatus();
            tvOrderId.setText("Đơn: " + order.getOrderId());
            tvStatus.setText("Trạng thái: " + mapStatus(status));
            tvTotal.setText("Tổng: " + MoneyFormatter.format(order.getTotal()));
            tvTime.setText("Tạo lúc: " + fmtTime(order.getCreatedAtMillis()));
            String preview = "Loại đơn: " + mapOrderType(order.getOrderType());
            preview += " | Kênh: " + mapOrderChannel(order.getOrderChannel());
            if ("dine_in".equals(order.getOrderType())) {
                preview += " | Bàn: " + safe(order.getTableName(), "-");
            }
            tvItemsPreview.setText(preview);

            boolean canPay = "created".equals(status) || "confirmed".equals(status);
            if (canPay) {
                btnPay.setVisibility(View.VISIBLE);
                btnPay.setOnClickListener(v -> {
                    Intent intent = new Intent(DonHangCuaToiActivity.this, ThanhToanKhachActivity.class);
                    intent.putExtra(ThanhToanKhachActivity.EXTRA_ORDER_ID, order.getOrderId());
                    intent.putExtra(ThanhToanKhachActivity.EXTRA_AMOUNT, order.getTotal());
                    startActivity(intent);
                });
            } else {
                btnPay.setVisibility(View.GONE);
            }
        }
    }

    private String mapStatus(String status) {
        if ("paid".equals(status)) return "Đã thanh toán";
        if ("confirmed".equals(status)) return "Đã xác nhận";
        if ("completed".equals(status)) return "Hoàn tất";
        if ("cancelled".equals(status)) return "Đã hủy";
        return "Chờ xử lý";
    }

    private String mapOrderType(String orderType) {
        if ("dine_in".equals(orderType)) {
            return "Tại chỗ";
        }
        if ("takeaway".equals(orderType)) {
            return "Mang về";
        }
        if ("online".equals(orderType)) {
            return "Qua app";
        }
        return "Khác";
    }

    private String mapOrderChannel(String orderChannel) {
        if ("customer_qr".equals(orderChannel)) {
            return "Khách quét QR";
        }
        if ("customer_app".equals(orderChannel)) {
            return "Khách đặt app";
        }
        if ("staff_pos".equals(orderChannel)) {
            return "Nhân viên tạo";
        }
        return "Khác";
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
