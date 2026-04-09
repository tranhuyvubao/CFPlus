package com.example.do_an_hk1_androidstudio;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentOnlineOrders extends Fragment {

    private final List<LocalOrder> orders = new ArrayList<>();
    private OrderCloudRepository orderRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration orderListener;
    private TextView tvEmpty;
    private OnlineOrderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_online_orders, container, false);
        sessionManager = new LocalSessionManager(requireContext());
        orderRepository = new OrderCloudRepository(requireContext());

        tvEmpty = view.findViewById(R.id.tvEmptyOnlineOrders);
        RecyclerView rvOnlineOrders = view.findViewById(R.id.rvOnlineOrders);
        rvOnlineOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OnlineOrderAdapter(orders);
        rvOnlineOrders.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadOnlineOrders();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }

    private void loadOnlineOrders() {
        if (orderListener != null) {
            orderListener.remove();
        }
        orderListener = orderRepository.listenOnlineOrders(fetchedOrders -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                orders.clear();
                for (LocalOrder order : fetchedOrders) {
                    if ("online".equals(order.getOrderType()) || "customer_app".equals(order.getOrderChannel())) {
                        orders.add(order);
                    }
                }
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private class OnlineOrderAdapter extends RecyclerView.Adapter<OnlineOrderViewHolder> {
        private final List<LocalOrder> data;

        OnlineOrderAdapter(List<LocalOrder> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public OnlineOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_online_order, parent, false);
            return new OnlineOrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OnlineOrderViewHolder holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class OnlineOrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMaDon;
        private final TextView tvBan;
        private final TextView tvTrangThai;
        private final TextView tvTongTien;
        private final TextView tvChiTietMon;
        private final TextView tvThoiGian;
        private final TextView tvDiaChiGiao;
        private final TextView tvHanhDong;
        private final TextView tvInBill;

        OnlineOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMaDon = itemView.findViewById(R.id.tvMaDon);
            tvBan = itemView.findViewById(R.id.tvBanOnline);
            tvTrangThai = itemView.findViewById(R.id.tvTrangThai);
            tvTongTien = itemView.findViewById(R.id.tvTongTien);
            tvChiTietMon = itemView.findViewById(R.id.tvChiTietMon);
            tvThoiGian = itemView.findViewById(R.id.tvThoiGian);
            tvDiaChiGiao = itemView.findViewById(R.id.tvDiaChiGiao);
            tvHanhDong = itemView.findViewById(R.id.tvHanhDong);
            tvInBill = itemView.findViewById(R.id.tvInBill);
        }

        void bind(LocalOrder order) {
            String status = order.getStatus();
            tvMaDon.setText("Mã đơn: " + order.getDisplayOrderCode());
            tvBan.setText("Loại: Qua app | Kênh: " + mapOrderChannel(order.getOrderChannel()));
            tvTrangThai.setText("Trạng thái: " + mapOrderStatus(status));
            tvTongTien.setText("Tổng tiền: " + formatMoney(order.getTotal()));
            tvChiTietMon.setText(buildOrderDetails(order));
            tvThoiGian.setText("Thời gian: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getCreatedAtMillis())));

            if (order.getDeliveryAddressText() != null && !order.getDeliveryAddressText().trim().isEmpty()) {
                tvDiaChiGiao.setVisibility(View.VISIBLE);
                tvDiaChiGiao.setText("Địa chỉ giao: " + order.getDeliveryAddressText());
            } else {
                tvDiaChiGiao.setVisibility(View.GONE);
            }

            if ("created".equals(status)) {
                tvHanhDong.setEnabled(true);
                tvHanhDong.setText("Xác nhận");
            } else if ("confirmed".equals(status)) {
                tvHanhDong.setEnabled(true);
                tvHanhDong.setText("Đã làm xong");
            } else {
                tvHanhDong.setEnabled(false);
                tvHanhDong.setText("-");
            }

            tvInBill.setEnabled(true);
            tvInBill.setAlpha(1f);

            tvHanhDong.setOnClickListener(v -> {
                String nextStatus = "created".equals(status) ? "confirmed" : "paid";
                orderRepository.updateOrderStatus(
                        order.getOrderId(),
                        nextStatus,
                        sessionManager.getCurrentUserId(),
                        (success, message) -> {
                            if (!isAdded()) {
                                return;
                            }
                            requireActivity().runOnUiThread(() -> {
                                if (!success) {
                                    tvHanhDong.setText("Thử lại");
                                }
                            });
                        }
                );
            });

            tvInBill.setOnClickListener(v -> showDemoBill(order));
        }
    }

    private String mapOrderChannel(String orderChannel) {
        if ("customer_app".equals(orderChannel)) {
            return "Khách app";
        }
        if ("customer_qr".equals(orderChannel)) {
            return "Khách QR";
        }
        if ("staff_pos".equals(orderChannel)) {
            return "Nhân viên";
        }
        return "Khác";
    }

    private String mapOrderStatus(String status) {
        if ("created".equals(status)) {
            return "Chờ xác nhận";
        }
        if ("confirmed".equals(status)) {
            return "Đang làm";
        }
        if ("paid".equals(status)) {
            return "Đã thanh toán";
        }
        if ("cancelled".equals(status)) {
            return "Đã hủy";
        }
        return status;
    }

    private String buildOrderDetails(LocalOrder order) {
        List<LocalOrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            return "Chưa có chi tiết món.";
        }

        StringBuilder builder = new StringBuilder("Món đã đặt:\n");
        for (LocalOrderItem item : items) {
            builder.append("- ")
                    .append(item.getProductName())
                    .append(" x")
                    .append(item.getQty())
                    .append(" - ")
                    .append(formatMoney(item.getLineTotal()));
            if (item.getVariantName() != null && !item.getVariantName().trim().isEmpty()) {
                builder.append(" (").append(item.getVariantName()).append(")");
            }
            if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
                builder.append("\n  Ghi chú: ").append(item.getNote());
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    private void showDemoBill(LocalOrder order) {
        if (!isAdded()) {
            return;
        }
        StringBuilder bill = new StringBuilder();
        bill.append("CFPLUS\n\n")
                .append("Đơn online: ").append(order.getDisplayOrderCode()).append("\n")
                .append("Kênh: ").append(mapOrderChannel(order.getOrderChannel())).append("\n")
                .append("Thời gian: ")
                .append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getCreatedAtMillis())))
                .append("\n\n");

        for (LocalOrderItem item : order.getItems()) {
            bill.append("- ")
                    .append(item.getProductName())
                    .append(" x")
                    .append(item.getQty())
                    .append(": ")
                    .append(formatMoney(item.getLineTotal()))
                    .append("\n");
        }

        bill.append("\nTổng cộng: ").append(formatMoney(order.getTotal()));

        new AlertDialog.Builder(requireContext())
                .setTitle("In bill demo")
                .setMessage(bill.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    private String formatMoney(int amount) {
        return String.format(Locale.getDefault(), "%,dđ", amount).replace(',', '.');
    }
}
