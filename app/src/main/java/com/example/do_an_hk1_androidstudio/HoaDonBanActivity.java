package com.example.do_an_hk1_androidstudio;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.PromotionCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalPromotion;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HoaDonBanActivity extends AppCompatActivity {

    public static final String EXTRA_TABLE_ID = "tableId";
    public static final String EXTRA_TABLE_NAME = "tableName";

    private final List<LocalOrderItem> billItems = new ArrayList<>();
    private final List<LocalPromotion> promotions = new ArrayList<>();

    private HoaDonItemAdapter adapter;
    private OrderCloudRepository orderRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration orderListener;
    private ListenerRegistration promotionsListener;

    private String tableId;
    private String tableName;
    private LocalOrder currentOrder;
    private LocalPromotion appliedPromotion;
    private int appliedDiscountAmount;

    private TextView tvTitle;
    private TextView tvEmpty;
    private TextView tvOrderCode;
    private TextView tvOrderTime;
    private TextView tvTableStatus;
    private TextView tvSubtotal;
    private TextView tvDiscount;
    private TextView tvTotal;
    private EditText edtPromotionCode;
    private TextView btnApplyPromotion;
    private TextView btnPrintBill;
    private TextView btnPay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hoa_don_ban);
        InsetsHelper.applyStatusBarPadding(findViewById(R.id.headerHoaDonBan));
        InsetsHelper.applyNavigationBarPadding(findViewById(R.id.rootHoaDonBan));

        sessionManager = new LocalSessionManager(this);
        String role = sessionManager.getCurrentUserRole();
        if (!"manager".equals(role) && !"staff".equals(role)) {
            finish();
            return;
        }

        tableId = getIntent().getStringExtra(EXTRA_TABLE_ID);
        tableName = getIntent().getStringExtra(EXTRA_TABLE_NAME);
        orderRepository = new OrderCloudRepository(this);

        tvTitle = findViewById(R.id.tvTitle);
        tvEmpty = findViewById(R.id.tvEmptyBill);
        tvOrderCode = findViewById(R.id.tvOrderCode);
        tvOrderTime = findViewById(R.id.tvOrderTime);
        tvTableStatus = findViewById(R.id.tvTableStatus);
        tvSubtotal = findViewById(R.id.tvSubtotalValue);
        tvDiscount = findViewById(R.id.tvDiscountValue);
        tvTotal = findViewById(R.id.tvTotalValue);
        edtPromotionCode = findViewById(R.id.edtPromotionCode);
        btnApplyPromotion = findViewById(R.id.btnApplyPromotion);
        btnPrintBill = findViewById(R.id.btnPrintBill);
        btnPay = findViewById(R.id.btnPayBill);

        RecyclerView rvItems = findViewById(R.id.rvBillItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HoaDonItemAdapter();
        rvItems.setAdapter(adapter);

        View backView = findViewById(R.id.tvBack);
        if (backView != null) {
            backView.setOnClickListener(v -> finish());
        }

        tvTitle.setText("Hóa đơn " + (TextUtils.isEmpty(tableName) ? "bàn" : tableName));
        tvEmpty.setText((TextUtils.isEmpty(tableName) ? "Bàn này" : tableName) + " đang trống.");

        btnApplyPromotion.setOnClickListener(v -> applyPromotionCode());
        btnPrintBill.setOnClickListener(v -> {
            if (currentOrder == null) {
                Toast.makeText(this, "Bàn này chưa có hóa đơn.", Toast.LENGTH_SHORT).show();
                return;
            }
            showDemoBill();
        });
        btnPay.setOnClickListener(v -> payCurrentOrder());

        renderOrder(null);
        listenPromotions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenTableOrder();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (promotionsListener != null) {
            promotionsListener.remove();
        }
    }

    private void listenPromotions() {
        promotionsListener = new PromotionCloudRepository(this).listenPromotions(fetched -> {
            promotions.clear();
            promotions.addAll(fetched);
        });
    }

    private void listenTableOrder() {
        if (orderListener != null) {
            orderListener.remove();
        }
        orderListener = orderRepository.listenAllOrders(fetchedOrders -> runOnUiThread(() -> {
            LocalOrder latestOpenOrder = null;
            for (LocalOrder order : fetchedOrders) {
                if (tableId != null
                        && tableId.equals(order.getTableId())
                        && ("created".equals(order.getStatus()) || "confirmed".equals(order.getStatus()))) {
                    latestOpenOrder = order;
                    break;
                }
            }
            renderOrder(latestOpenOrder);
        }));
    }

    private void renderOrder(@Nullable LocalOrder order) {
        currentOrder = order;
        billItems.clear();
        if (order != null) {
            billItems.addAll(order.getItems());
        }
        adapter.notifyDataSetChanged();

        boolean hasOrder = order != null;
        tvEmpty.setVisibility(hasOrder ? View.GONE : View.VISIBLE);
        findViewById(R.id.contentBill).setVisibility(hasOrder ? View.VISIBLE : View.GONE);

        if (!hasOrder) {
            appliedPromotion = null;
            appliedDiscountAmount = 0;
            edtPromotionCode.setText("");
            tvOrderCode.setText("Mã hóa đơn: -");
            tvOrderTime.setText("Thời gian: -");
            tvTableStatus.setText("Bàn trống");
            tvSubtotal.setText("0đ");
            tvDiscount.setText("0đ");
            tvTotal.setText("0đ");
            return;
        }

        tvOrderCode.setText("Mã hóa đơn: " + order.getDisplayOrderCode());
        tvOrderTime.setText("Thời gian: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getCreatedAtMillis())));
        tvTableStatus.setText("Đang phục vụ");
        recalculateTotals();
    }

    private void applyPromotionCode() {
        if (currentOrder == null) {
            Toast.makeText(this, "Bàn này chưa có hóa đơn.", Toast.LENGTH_SHORT).show();
            return;
        }

        String code = edtPromotionCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            appliedPromotion = null;
            appliedDiscountAmount = 0;
            recalculateTotals();
            Toast.makeText(this, "Đã bỏ mã giảm giá.", Toast.LENGTH_SHORT).show();
            return;
        }

        LocalPromotion promotion = findPromotionByCode(code);
        if (promotion == null || !promotion.isActive()) {
            Toast.makeText(this, "Mã giảm giá không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentOrder.getSubtotal() < promotion.getMinOrder()) {
            Toast.makeText(this, "Hóa đơn chưa đạt giá trị tối thiểu để dùng mã.", Toast.LENGTH_SHORT).show();
            return;
        }

        appliedPromotion = promotion;
        appliedDiscountAmount = calculateDiscount(currentOrder.getSubtotal(), promotion);
        recalculateTotals();
        Toast.makeText(this, "Đã áp dụng mã " + promotion.getCode(), Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private LocalPromotion findPromotionByCode(String code) {
        for (LocalPromotion promotion : promotions) {
            if (promotion.getCode() != null && promotion.getCode().equalsIgnoreCase(code)) {
                return promotion;
            }
        }
        return null;
    }

    private void recalculateTotals() {
        if (currentOrder == null) {
            return;
        }
        int subtotal = currentOrder.getSubtotal();
        if (appliedPromotion != null) {
            appliedDiscountAmount = calculateDiscount(subtotal, appliedPromotion);
        } else {
            appliedDiscountAmount = 0;
        }
        int total = Math.max(0, subtotal - appliedDiscountAmount);
        tvSubtotal.setText(formatMoney(subtotal));
        tvDiscount.setText(formatMoney(appliedDiscountAmount));
        tvTotal.setText(formatMoney(total));
    }

    private int calculateDiscount(int subtotal, LocalPromotion promotion) {
        int discount;
        if ("percent".equalsIgnoreCase(promotion.getType())) {
            discount = (int) Math.round(subtotal * (promotion.getValue() / 100d));
        } else {
            discount = (int) Math.round(promotion.getValue());
        }
        if (promotion.getMaxDiscount() != null) {
            discount = Math.min(discount, promotion.getMaxDiscount());
        }
        return Math.max(0, discount);
    }

    private void payCurrentOrder() {
        if (currentOrder == null) {
            Toast.makeText(this, "Bàn này chưa có hóa đơn.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPay.setEnabled(false);
        orderRepository.payOrder(
                currentOrder.getOrderId(),
                currentOrder.getSubtotal(),
                "cash",
                null,
                appliedDiscountAmount,
                appliedPromotion == null ? null : appliedPromotion.getCode(),
                (success, message) -> runOnUiThread(() -> {
                    btnPay.setEnabled(true);
                    if (!success) {
                        Toast.makeText(this, message == null ? "Không thể thanh toán." : message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(this, "Thanh toán thành công.", Toast.LENGTH_SHORT).show();
                    finish();
                })
        );
    }

    private void showDemoBill() {
        if (currentOrder == null) {
            return;
        }
        StringBuilder bill = new StringBuilder();
        bill.append("CFPLUS\n")
                .append("Hóa đơn bàn: ").append(safe(tableName, "-")).append("\n")
                .append("Mã: ").append(currentOrder.getDisplayOrderCode()).append("\n")
                .append("Thời gian: ")
                .append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(currentOrder.getCreatedAtMillis())))
                .append("\n\n");

        for (LocalOrderItem item : currentOrder.getItems()) {
            bill.append("- ")
                    .append(item.getProductName())
                    .append(" x")
                    .append(item.getQty())
                    .append(": ")
                    .append(formatMoney(item.getLineTotal()))
                    .append("\n");
        }

        bill.append("\nTạm tính: ").append(formatMoney(currentOrder.getSubtotal())).append("\n")
                .append("Giảm giá: ").append(formatMoney(appliedDiscountAmount)).append("\n")
                .append("Tổng tiền: ").append(tvTotal.getText());

        new AlertDialog.Builder(this)
                .setTitle("In bill demo")
                .setMessage(bill.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    private String formatMoney(int amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    private String safe(@Nullable String primary, @Nullable String fallback) {
        if (!TextUtils.isEmpty(primary)) {
            return primary;
        }
        return fallback == null ? "-" : fallback;
    }

    private class HoaDonItemAdapter extends RecyclerView.Adapter<HoaDonItemViewHolder> {
        @NonNull
        @Override
        public HoaDonItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hoa_don_ban, parent, false);
            return new HoaDonItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HoaDonItemViewHolder holder, int position) {
            holder.bind(billItems.get(position));
        }

        @Override
        public int getItemCount() {
            return billItems.size();
        }
    }

    private class HoaDonItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgProduct;
        private final TextView tvName;
        private final TextView tvQty;
        private final TextView tvUnitPrice;
        private final TextView tvLineTotal;
        private final TextView tvNote;

        HoaDonItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgBillProduct);
            tvName = itemView.findViewById(R.id.tvBillProductName);
            tvQty = itemView.findViewById(R.id.tvBillQty);
            tvUnitPrice = itemView.findViewById(R.id.tvBillUnitPrice);
            tvLineTotal = itemView.findViewById(R.id.tvBillLineTotal);
            tvNote = itemView.findViewById(R.id.tvBillNote);
        }

        void bind(LocalOrderItem item) {
            tvName.setText(item.getProductName());
            tvQty.setText(String.valueOf(item.getQty()));
            tvUnitPrice.setText(formatMoney(item.getUnitPrice()));
            tvLineTotal.setText(formatMoney(item.getLineTotal()));
            if (TextUtils.isEmpty(item.getNote())) {
                tvNote.setVisibility(View.GONE);
            } else {
                tvNote.setVisibility(View.VISIBLE);
                tvNote.setText("Ghi chú: " + item.getNote());
            }

            if (TextUtils.isEmpty(item.getImageUrl())) {
                imgProduct.setImageResource(R.drawable.cfplus4);
            } else {
                Picasso.get()
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.cfplus4)
                        .error(R.drawable.cfplus4)
                        .into(imgProduct);
            }
        }
    }
}
