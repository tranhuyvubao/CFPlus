package com.example.do_an_hk1_androidstudio;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.example.do_an_hk1_androidstudio.ui.StaffOrderEditorDialog;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NhanDonOnlineActivity extends AppCompatActivity {

    private final List<LocalOrder> orders = new ArrayList<>();
    private final List<LocalProduct> editableProducts = new ArrayList<>();
    private final Map<String, Long> notifiedAttentionEvents = new HashMap<>();

    private OnlineOrderAdapter adapter;
    private TextView tvEmpty;
    private String selectedTableId;
    private String selectedTableName;
    private OrderCloudRepository orderRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration orderListener;
    private ListenerRegistration productListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nhan_don_online);
        InsetsHelper.applyStatusBarPadding(findViewById(R.id.headerNhanDonOnline));
        InsetsHelper.applyNavigationBarPadding(findViewById(R.id.rootNhanDonOnline));

        sessionManager = new LocalSessionManager(this);
        String role = sessionManager.getCurrentUserRole();
        if (!"manager".equals(role) && !"staff".equals(role)) {
            finish();
            return;
        }

        orderRepository = new OrderCloudRepository(this);
        selectedTableId = getIntent().getStringExtra("tableId");
        selectedTableName = getIntent().getStringExtra("tableName");

        TextView tvTitle = findViewById(R.id.tvTitleOnline);
        TextView tvSubtitle = findViewById(R.id.tvOnlineSubtitle);
        View tvBack = findViewById(R.id.tvBack);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        if (hasSelectedTable()) {
            if (tvTitle != null) {
                tvTitle.setText("Hóa đơn bàn");
            }
            if (tvSubtitle != null) {
                tvSubtitle.setText("Theo dõi đơn đang mở tại bàn, chỉnh sửa món và thanh toán ngay khi khách đổi yêu cầu.");
            }
            if (selectedTableName != null && !selectedTableName.trim().isEmpty()) {
                tvEmpty.setText(selectedTableName + " đang trống.");
            }
        } else {
            if (tvTitle != null) {
                tvTitle.setText("Nhận đơn online");
            }
            if (tvSubtitle != null) {
                tvSubtitle.setText("Xác nhận, chỉnh sửa và hoàn tất đơn khách đặt qua app hoặc QR.");
            }
        }

        RecyclerView rvOnlineOrders = findViewById(R.id.rvOnlineOrders);
        rvOnlineOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OnlineOrderAdapter(orders);
        rvOnlineOrders.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadOnlineOrders();
        listenProducts();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
        if (productListener != null) {
            productListener.remove();
            productListener = null;
        }
    }

    private void listenProducts() {
        if (productListener != null) {
            productListener.remove();
        }
        productListener = new CatalogCloudRepository(this).listenProducts(products -> runOnUiThread(() -> {
            editableProducts.clear();
            editableProducts.addAll(products);
        }));
    }

    private void loadOnlineOrders() {
        if (orderListener != null) {
            orderListener.remove();
        }
        orderListener = orderRepository.listenOnlineOrders(fetchedOrders -> runOnUiThread(() -> {
            orders.clear();
            if (hasSelectedTable()) {
                for (LocalOrder order : fetchedOrders) {
                    if (selectedTableId.equals(order.getTableId()) && isOpenStatus(order.getStatus())) {
                        orders.add(order);
                    }
                }
                if (orders.size() > 1) {
                    LocalOrder newestOpenOrder = orders.get(0);
                    orders.clear();
                    orders.add(newestOpenOrder);
                }
            } else {
                for (LocalOrder order : fetchedOrders) {
                    if (isOpenStatus(order.getStatus()) && isCustomerSubmittedOrder(order)) {
                        orders.add(order);
                    }
                }
            }
            notifyWhenCustomerAddsItems(orders);
            adapter.notifyDataSetChanged();
            if (hasSelectedTable()) {
                tvEmpty.setText(orders.isEmpty()
                        ? (selectedTableName == null ? "Bàn này đang trống." : selectedTableName + " đang trống.")
                        : "");
            } else {
                tvEmpty.setText("Chưa có đơn online nào đang mở.");
            }
            tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    private boolean hasSelectedTable() {
        return selectedTableId != null && !selectedTableId.trim().isEmpty();
    }

    private boolean isOpenStatus(@Nullable String status) {
        return "created".equals(status) || "confirmed".equals(status);
    }

    private boolean isCustomerSubmittedOrder(@NonNull LocalOrder order) {
        String orderChannel = order.getOrderChannel();
        return "customer_app".equals(orderChannel) || "customer_qr".equals(orderChannel);
    }

    private void notifyWhenCustomerAddsItems(@NonNull List<LocalOrder> visibleOrders) {
        for (LocalOrder order : visibleOrders) {
            if (!order.needsStaffAttention()) {
                continue;
            }

            long eventAt = order.getLastCustomerItemAddedAtMillis();
            Long notifiedAt = notifiedAttentionEvents.get(order.getOrderId());
            if (notifiedAt != null && notifiedAt == eventAt) {
                continue;
            }

            int addedQty = Math.max(1, order.getLastCustomerItemAddedQty());
            String tableLabel = order.getTableName() == null || order.getTableName().trim().isEmpty()
                    ? "đơn " + order.getDisplayOrderCode()
                    : order.getTableName();
            String body = tableLabel + " vừa thêm " + addedQty + " món. Vui lòng kiểm tra đơn.";
            Toast.makeText(this, body, Toast.LENGTH_LONG).show();
            notifiedAttentionEvents.put(order.getOrderId(), eventAt);
        }
    }

    private void openOrderEditor(@NonNull LocalOrder order) {
        StaffOrderEditorDialog.show(
                this,
                order.getDisplayOrderCode(),
                order.getItems(),
                editableProducts,
                true,
                updatedItems -> {
                    if (updatedItems.isEmpty()) {
                        confirmCancelOrder(order);
                        return;
                    }
                    orderRepository.updateOrderItems(
                            order.getOrderId(),
                            updatedItems,
                            order.getDiscountAmount(),
                            sessionManager.getCurrentUserId(),
                            (success, message) -> runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(this, "Đã cập nhật lại món trong đơn.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, message == null ? "Không thể lưu thay đổi đơn." : message, Toast.LENGTH_SHORT).show();
                                }
                            })
                    );
                }
        );
    }

    private void confirmCancelOrder(@NonNull LocalOrder order) {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn?")
                .setMessage("Nếu xóa hết món, đơn sẽ được hủy. Bạn có muốn tiếp tục không?")
                .setNegativeButton("Giữ lại", null)
                .setPositiveButton("Hủy đơn", (dialog, which) -> orderRepository.cancelOrder(
                        order.getOrderId(),
                        (success, message) -> runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Đơn đã được hủy.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, message == null ? "Không thể hủy đơn." : message, Toast.LENGTH_SHORT).show();
                            }
                        })
                ))
                .show();
    }

    private class OnlineOrderAdapter extends RecyclerView.Adapter<OnlineOrderViewHolder> {
        private final List<LocalOrder> data;

        OnlineOrderAdapter(List<LocalOrder> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public OnlineOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_online_order, parent, false);
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
        private final TextView tvNewItemsAlert;
        private final TextView tvTongTien;
        private final TextView tvChiTietMon;
        private final TextView tvThoiGian;
        private final TextView tvDiaChiGiao;
        private final TextView tvSuaDon;
        private final TextView tvHanhDong;
        private final TextView tvInBill;

        OnlineOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMaDon = itemView.findViewById(R.id.tvMaDon);
            tvBan = itemView.findViewById(R.id.tvBanOnline);
            tvTrangThai = itemView.findViewById(R.id.tvTrangThai);
            tvNewItemsAlert = itemView.findViewById(R.id.tvNewItemsAlert);
            tvTongTien = itemView.findViewById(R.id.tvTongTien);
            tvChiTietMon = itemView.findViewById(R.id.tvChiTietMon);
            tvThoiGian = itemView.findViewById(R.id.tvThoiGian);
            tvDiaChiGiao = itemView.findViewById(R.id.tvDiaChiGiao);
            tvSuaDon = itemView.findViewById(R.id.tvSuaDon);
            tvHanhDong = itemView.findViewById(R.id.tvHanhDong);
            tvInBill = itemView.findViewById(R.id.tvInBill);
        }

        void bind(LocalOrder order) {
            String status = order.getStatus();
            String orderType = order.getOrderType();
            String orderChannel = order.getOrderChannel();
            tvMaDon.setText("Mã đơn: " + order.getDisplayOrderCode());

            if ("dine_in".equals(orderType)) {
                tvBan.setText("Loại: Tại chỗ | Kênh: " + mapOrderChannel(orderChannel) + " | Bàn: " + (order.getTableName() == null ? "-" : order.getTableName()));
            } else if ("takeaway".equals(orderType)) {
                tvBan.setText("Loại: Mang về | Kênh: " + mapOrderChannel(orderChannel));
            } else if ("online".equals(orderType)) {
                tvBan.setText("Loại: Qua app | Kênh: " + mapOrderChannel(orderChannel));
            } else {
                tvBan.setText("Loại/Kênh: Khác");
            }

            if (hasSelectedTable()) {
                tvTrangThai.setText("Trạng thái: Có khách - chưa thanh toán");
            } else {
                tvTrangThai.setText("Trạng thái: " + mapOrderStatus(status));
            }

            if (order.needsStaffAttention()) {
                tvNewItemsAlert.setVisibility(View.VISIBLE);
                String supportRequestNote = order.getSupportRequestNote();
                List<LocalOrderItem> addedItems = order.getLastCustomerAddedItems();
                StringBuilder alert = new StringBuilder();
                if (!TextUtils.isEmpty(supportRequestNote)) {
                    alert.append("Yêu cầu sửa món: ").append(supportRequestNote.trim());
                } else if (!addedItems.isEmpty()) {
                    int addedQty = Math.max(1, order.getLastCustomerItemAddedQty());
                    alert.append("Khách vừa thêm ")
                            .append(addedQty)
                            .append(" món: ")
                            .append(addedItems.get(0).getProductName());
                    if (addedItems.size() > 1) {
                        alert.append(" +").append(addedItems.size() - 1).append(" món khác");
                    }
                    alert.append(" - kiểm tra lại đơn");
                } else {
                    alert.append("Khách đang cần hỗ trợ, vui lòng kiểm tra đơn.");
                }
                tvNewItemsAlert.setText(alert.toString());
            } else {
                tvNewItemsAlert.setVisibility(View.GONE);
            }

            tvTongTien.setText("Tổng tiền: " + MoneyFormatter.format(order.getTotal()));
            tvChiTietMon.setText(buildOrderDetails(order));
            tvThoiGian.setText("Thời gian: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getCreatedAtMillis())));
            if ("online".equals(orderType) && order.getDeliveryAddressText() != null && !order.getDeliveryAddressText().trim().isEmpty()) {
                tvDiaChiGiao.setVisibility(View.VISIBLE);
                tvDiaChiGiao.setText("Địa chỉ giao: " + order.getDeliveryAddressText());
            } else {
                tvDiaChiGiao.setVisibility(View.GONE);
            }

            boolean canEdit = isOpenStatus(status);
            tvSuaDon.setEnabled(canEdit);
            tvSuaDon.setAlpha(canEdit ? 1f : 0.45f);
            tvSuaDon.setOnClickListener(v -> {
                if (canEdit) {
                    openOrderEditor(order);
                }
            });

            if (hasSelectedTable()) {
                if (isOpenStatus(status)) {
                    tvHanhDong.setEnabled(true);
                    tvHanhDong.setText("Thanh toán");
                } else {
                    tvHanhDong.setEnabled(false);
                    tvHanhDong.setText("-");
                }
            } else if ("created".equals(status)) {
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
                String nextStatus;
                if (hasSelectedTable()) {
                    nextStatus = "paid";
                } else {
                    nextStatus = "created".equals(status) ? "confirmed" : "paid";
                }
                        orderRepository.updateOrderStatus(
                        order.getOrderId(),
                        nextStatus,
                        sessionManager.getCurrentUserId(),
                        (success, message) -> runOnUiThread(() -> {
                            if (!success) {
                                tvHanhDong.setText("Thử lại");
                                Toast.makeText(NhanDonOnlineActivity.this, message == null ? "Không thể cập nhật đơn." : message, Toast.LENGTH_SHORT).show();
                            }
                        })
                );
            });

            tvInBill.setOnClickListener(v -> showDemoBill(order));
        }
    }

    private String mapOrderChannel(@Nullable String orderChannel) {
        if ("customer_qr".equals(orderChannel)) {
            return "Khách QR";
        }
        if ("customer_app".equals(orderChannel)) {
            return "Khách app";
        }
        if ("staff_pos".equals(orderChannel)) {
            return "Nhân viên";
        }
        return "Khác";
    }

    private String mapOrderStatus(@Nullable String status) {
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
        return status == null ? "-" : status;
    }

    private String buildOrderDetails(@NonNull LocalOrder order) {
        List<LocalOrderItem> items = order.getItems();
        if (items.isEmpty()) {
            return "Món: " + order.getDisplayOrderCode();
        }

        StringBuilder builder = new StringBuilder();
        List<LocalOrderItem> addedItems = order.getLastCustomerAddedItems();
        if (order.needsStaffAttention() && !addedItems.isEmpty()) {
            builder.append("Món vừa thêm:\n");
            appendOrderItems(builder, addedItems);
            builder.append("\n\n");
        }
        builder.append("Món đã gọi:\n");
        appendOrderItems(builder, items);
        return builder.toString().trim();
    }

    private void appendOrderItems(@NonNull StringBuilder builder, @NonNull List<LocalOrderItem> items) {
        for (LocalOrderItem item : items) {
            builder.append("- ")
                    .append(item.getProductName())
                    .append(" x")
                    .append(item.getQty())
                    .append(" - ")
                    .append(MoneyFormatter.format(item.getLineTotal()));
            if (item.getVariantName() != null && !item.getVariantName().trim().isEmpty()) {
                builder.append(" (").append(item.getVariantName()).append(")");
            }
            if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
                builder.append("\n  Ghi chú: ").append(item.getNote());
            }
            builder.append("\n");
        }
    }

    private void showDemoBill(@NonNull LocalOrder order) {
        StringBuilder bill = new StringBuilder();
        bill.append("CFPLUS\n\n");
        if (hasSelectedTable()) {
            bill.append("Hóa đơn bàn: ").append(selectedTableName == null ? "-" : selectedTableName).append("\n");
        } else {
            bill.append("Đơn online: ").append(order.getDisplayOrderCode()).append("\n");
        }
        bill.append("Kênh: ").append(mapOrderChannel(order.getOrderChannel())).append("\n");
        bill.append("Thời gian: ")
                .append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getCreatedAtMillis())))
                .append("\n\n");
        for (LocalOrderItem item : order.getItems()) {
            bill.append("- ")
                    .append(item.getProductName())
                    .append(" x")
                    .append(item.getQty())
                    .append(": ")
                    .append(MoneyFormatter.format(item.getLineTotal()))
                    .append("\n");
        }
        bill.append("\nTổng cộng: ").append(MoneyFormatter.format(order.getTotal()));

        new AlertDialog.Builder(this)
                .setTitle("Xem hóa đơn")
                .setMessage(bill.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }
}
