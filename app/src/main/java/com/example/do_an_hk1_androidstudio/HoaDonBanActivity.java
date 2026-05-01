package com.example.do_an_hk1_androidstudio;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.do_an_hk1_androidstudio.cloud.PromotionCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalCafeTable;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.local.model.LocalPromotion;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.example.do_an_hk1_androidstudio.ui.StaffOrderEditorDialog;
import com.example.do_an_hk1_androidstudio.ui.CfPlusImageLoader;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HoaDonBanActivity extends AppCompatActivity {

    public static final String EXTRA_TABLE_ID = "tableId";
    public static final String EXTRA_TABLE_NAME = "tableName";

    private final List<LocalOrderItem> billItems = new ArrayList<>();
    private final List<LocalPromotion> promotions = new ArrayList<>();
    private final List<LocalProduct> editableProducts = new ArrayList<>();

    private HoaDonItemAdapter adapter;
    private OrderCloudRepository orderRepository;
    private TableCloudRepository tableRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration orderListener;
    private ListenerRegistration promotionsListener;
    private ListenerRegistration productListener;

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
        tableRepository = new TableCloudRepository(this);

        tvTitle = findViewById(R.id.tvTitle);
        tvEmpty = findViewById(R.id.tvEmptyBill);
        tvOrderCode = findViewById(R.id.tvOrderCode);
        tvOrderTime = findViewById(R.id.tvOrderTime);
        tvTableStatus = findViewById(R.id.tvTableStatus);
        tvSubtotal = findViewById(R.id.tvSubtotalValue);
        tvDiscount = findViewById(R.id.tvDiscountValue);
        tvTotal = findViewById(R.id.tvTotalValue);
        edtPromotionCode = findViewById(R.id.edtPromotionCode);

        RecyclerView rvItems = findViewById(R.id.rvBillItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HoaDonItemAdapter();
        rvItems.setAdapter(adapter);

        View backView = findViewById(R.id.tvBack);
        if (backView != null) {
            backView.setOnClickListener(v -> finish());
        }

        if (TableCloudRepository.TAKEAWAY_TABLE_ID.equals(tableId)) {
            tvTitle.setText("Hóa đơn mang về");
            tvEmpty.setText("Chưa có đơn mang về đang mở.");
        } else {
            tvTitle.setText("Hóa đơn " + (TextUtils.isEmpty(tableName) ? "bàn" : tableName));
            tvEmpty.setText((TextUtils.isEmpty(tableName) ? "Bàn này" : tableName) + " đang trống.");
        }

        findViewById(R.id.btnApplyPromotion).setOnClickListener(v -> applyPromotionCode());
        findViewById(R.id.btnEditBill).setOnClickListener(v -> openOrderEditor());
        View btnMoveTable = findViewById(R.id.btnMoveTable);
        if (btnMoveTable != null) {
            btnMoveTable.setVisibility(TableCloudRepository.TAKEAWAY_TABLE_ID.equals(tableId) ? View.GONE : View.VISIBLE);
            btnMoveTable.setOnClickListener(v -> openMoveTableDialogModern());
        }
        findViewById(R.id.btnPrintBill).setOnClickListener(v -> {
            if (currentOrder == null) {
                Toast.makeText(this, "Bàn này chưa có hóa đơn.", Toast.LENGTH_SHORT).show();
                return;
            }
            showDemoBill();
        });
        findViewById(R.id.btnPayBill).setOnClickListener(v -> openPaymentScreen());

        renderOrder(null);
        listenPromotions();
        listenProducts();
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
        if (productListener != null) {
            productListener.remove();
        }
    }

    private void listenPromotions() {
        promotionsListener = new PromotionCloudRepository(this).listenPromotions(fetched -> {
            promotions.clear();
            promotions.addAll(fetched);
        });
    }

    private void listenProducts() {
        productListener = new CatalogCloudRepository(this).listenProducts(products -> runOnUiThread(() -> {
            editableProducts.clear();
            editableProducts.addAll(products);
            adapter.notifyDataSetChanged();
        }));
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
            tvSubtotal.setText(MoneyFormatter.format(0));
            tvDiscount.setText(MoneyFormatter.format(0));
            tvTotal.setText(MoneyFormatter.format(0));
            return;
        }

        tvOrderCode.setText("Mã hóa đơn: " + order.getDisplayOrderCode());
        tvOrderTime.setText("Thời gian: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getCreatedAtMillis())));
        tvTableStatus.setText("Đang phục vụ");
        recalculateTotals();
    }

    private void openOrderEditor() {
        if (currentOrder == null) {
            Toast.makeText(this, "Bàn này chưa có hóa đơn để chỉnh.", Toast.LENGTH_SHORT).show();
            return;
        }
        StaffOrderEditorDialog.show(
                this,
                currentOrder.getDisplayOrderCode(),
                currentOrder.getItems(),
                editableProducts,
                false,
                updatedItems -> {
                    if (updatedItems.isEmpty()) {
                        confirmCancelCurrentOrder();
                        return;
                    }
                    orderRepository.updateOrderItems(
                            currentOrder.getOrderId(),
                            updatedItems,
                            appliedDiscountAmount,
                            sessionManager.getCurrentUserId(),
                            (success, message) -> runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(this, "Đã cập nhật hóa đơn tại bàn.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, message == null ? "Không thể cập nhật hóa đơn." : message, Toast.LENGTH_SHORT).show();
                                }
                            })
                    );
                }
        );
    }

    private void openMoveTableDialog() {
        if (currentOrder == null) {
            Toast.makeText(this, "Bàn này chưa có hóa đơn để đổi bàn.", Toast.LENGTH_SHORT).show();
            return;
        }
        tableRepository.getActiveTables((tables, message) -> runOnUiThread(() -> {
            if (tables.isEmpty()) {
                Toast.makeText(this, message == null ? "Chưa có bàn khả dụng để chuyển." : message, Toast.LENGTH_SHORT).show();
                return;
            }

            List<LocalCafeTable> availableTables = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (LocalCafeTable table : tables) {
                if (table == null || table.getTableId() == null) {
                    continue;
                }
                if (table.getTableId().equals(tableId)) {
                    continue;
                }
                if ("occupied".equalsIgnoreCase(table.getStatus())) {
                    continue;
                }
                availableTables.add(table);
                StringBuilder label = new StringBuilder(table.getName());
                if (!TextUtils.isEmpty(table.getArea())) {
                    label.append(" • ").append(table.getArea());
                }
                if (!TextUtils.isEmpty(table.getStatus())) {
                    label.append(" • ").append(mapTableStatus(table.getStatus()));
                }
                labels.add(label.toString());
            }

            if (availableTables.isEmpty()) {
                Toast.makeText(this, "Không còn bàn trống phù hợp để chuyển.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Đổi bàn cho khách")
                    .setItems(labels.toArray(new CharSequence[0]), (dialog, which) ->
                            showMoveTableConfirmation(availableTables.get(which)))
                    .setNegativeButton("Đóng", null)
                    .show();
        }));
    }

    private void openMoveTableDialogModern() {
        if (currentOrder == null) {
            Toast.makeText(this, "Bàn này chưa có hóa đơn để đổi bàn.", Toast.LENGTH_SHORT).show();
            return;
        }
        tableRepository.getActiveTables((tables, message) -> runOnUiThread(() -> {
            if (tables == null || tables.isEmpty()) {
                Toast.makeText(this, message == null ? "Chưa có bàn khả dụng để chuyển." : message, Toast.LENGTH_SHORT).show();
                return;
            }
            loadOpenOrdersForMoveTable(tables);
        }));
    }

    private void loadOpenOrdersForMoveTable(@NonNull List<LocalCafeTable> tables) {
        final ListenerRegistration[] listenerHolder = new ListenerRegistration[1];
        listenerHolder[0] = orderRepository.listenAllOrders(fetchedOrders -> runOnUiThread(() -> {
            if (listenerHolder[0] != null) {
                listenerHolder[0].remove();
                listenerHolder[0] = null;
            }

            Set<String> occupiedTableIds = new HashSet<>();
            for (LocalOrder order : fetchedOrders) {
                if (order == null || TextUtils.isEmpty(order.getTableId())) {
                    continue;
                }
                boolean isOpenOrder = "created".equals(order.getStatus()) || "confirmed".equals(order.getStatus());
                if (!isOpenOrder) {
                    continue;
                }
                if (currentOrder != null && order.getOrderId().equals(currentOrder.getOrderId())) {
                    continue;
                }
                occupiedTableIds.add(order.getTableId());
            }

            List<LocalCafeTable> availableTables = new ArrayList<>();
            for (LocalCafeTable table : tables) {
                if (table == null || TextUtils.isEmpty(table.getTableId())) {
                    continue;
                }
                if (table.getTableId().equals(tableId)) {
                    continue;
                }
                if (TableCloudRepository.TAKEAWAY_TABLE_ID.equals(table.getTableId())) {
                    continue;
                }
                if ("occupied".equalsIgnoreCase(table.getStatus())) {
                    continue;
                }
                if (occupiedTableIds.contains(table.getTableId())) {
                    continue;
                }
                availableTables.add(table);
            }

            if (availableTables.isEmpty()) {
                Toast.makeText(this, "Không còn bàn trống phù hợp để chuyển.", Toast.LENGTH_SHORT).show();
                return;
            }
            showMoveTablePickerDialog(availableTables);
        }));
    }

    private void showMoveTablePickerDialog(@NonNull List<LocalCafeTable> availableTables) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_table_for_order, null, false);
        TextView tvSummary = dialogView.findViewById(R.id.tvMoveTableSummary);
        LinearLayout layoutOptions = dialogView.findViewById(R.id.layoutMoveTableOptions);

        tvSummary.setText("Bàn hiện tại: " + safe(tableName, "Bàn không tên")
                + " • Còn " + availableTables.size() + " bàn sẵn sàng chuyển.");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (LocalCafeTable table : availableTables) {
            View itemView = inflater.inflate(R.layout.item_move_table_option, layoutOptions, false);
            TextView tvName = itemView.findViewById(R.id.tvMoveTableOptionName);
            TextView tvMeta = itemView.findViewById(R.id.tvMoveTableOptionMeta);
            TextView btnChoose = itemView.findViewById(R.id.btnChooseMoveTable);

            tvName.setText(table.getName());
            StringBuilder meta = new StringBuilder();
            if (!TextUtils.isEmpty(table.getArea())) {
                meta.append(table.getArea());
            }
            if (!TextUtils.isEmpty(table.getCode())) {
                if (meta.length() > 0) {
                    meta.append(" • ");
                }
                meta.append("Mã ").append(table.getCode());
            }
            if (!TextUtils.isEmpty(table.getStatus())) {
                if (meta.length() > 0) {
                    meta.append(" • ");
                }
                meta.append(mapTableStatus(table.getStatus()));
            }
            tvMeta.setText(meta.length() == 0 ? "Sẵn sàng nhận khách" : meta.toString());
            btnChoose.setOnClickListener(v -> {
                dialog.dismiss();
                showMoveTableConfirmation(table);
            });
            layoutOptions.addView(itemView);
        }

        dialogView.findViewById(R.id.btnCloseMoveTablePicker).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showMoveTableConfirmation(@NonNull LocalCafeTable selectedTable) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_move_table, null, false);
        TextView tvMoveFromTable = dialogView.findViewById(R.id.tvMoveFromTable);
        TextView tvMoveFromArea = dialogView.findViewById(R.id.tvMoveFromArea);
        TextView tvMoveToTable = dialogView.findViewById(R.id.tvMoveToTable);
        TextView tvMoveToArea = dialogView.findViewById(R.id.tvMoveToArea);

        tvMoveFromTable.setText(TextUtils.isEmpty(tableName) ? "Bàn hiện tại" : tableName);
        tvMoveFromArea.setText(TextUtils.isEmpty(currentOrder == null ? null : currentOrder.getTableId())
                ? "Không có mã bàn"
                : "Mã bàn: " + currentOrder.getTableId());
        tvMoveToTable.setText(selectedTable.getName());
        StringBuilder toArea = new StringBuilder();
        if (!TextUtils.isEmpty(selectedTable.getArea())) {
            toArea.append(selectedTable.getArea());
        }
        if (!TextUtils.isEmpty(selectedTable.getStatus())) {
            if (toArea.length() > 0) {
                toArea.append(" • ");
            }
            toArea.append(mapTableStatus(selectedTable.getStatus()));
        }
        tvMoveToArea.setText(toArea.length() == 0 ? "Sẵn sàng nhận khách" : toArea.toString());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancelMoveTable).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirmMoveTable).setOnClickListener(v -> {
            dialog.dismiss();
            orderRepository.reassignOrderTable(
                    currentOrder.getOrderId(),
                    tableId,
                    selectedTable.getTableId(),
                    selectedTable.getName(),
                    sessionManager.getCurrentUserId(),
                    (success, callbackMessage) -> runOnUiThread(() -> {
                        if (success) {
                            tableId = selectedTable.getTableId();
                            tableName = selectedTable.getName();
                            tvTitle.setText("Hóa đơn " + selectedTable.getName());
                            tvEmpty.setText(selectedTable.getName() + " đang trống.");
                            Toast.makeText(this, "Đã chuyển khách sang " + selectedTable.getName() + ".", Toast.LENGTH_SHORT).show();
                            listenTableOrder();
                        } else {
                            Toast.makeText(this, callbackMessage == null ? "Không thể đổi bàn." : callbackMessage, Toast.LENGTH_SHORT).show();
                        }
                    })
            );
        });
        dialog.show();
    }

    @NonNull
    private String mapTableStatus(@Nullable String status) {
        if ("reserved".equalsIgnoreCase(status)) {
            return "Đã đặt";
        }
        if ("occupied".equalsIgnoreCase(status)) {
            return "Đang dùng";
        }
        return "Trống";
    }

    private void confirmCancelCurrentOrder() {
        if (currentOrder == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Hủy hóa đơn?")
                .setMessage("Nếu xóa hết món thì hóa đơn sẽ bị hủy. Bạn có muốn tiếp tục không?")
                .setNegativeButton("Giữ lại", null)
                .setPositiveButton("Hủy đơn", (dialog, which) -> orderRepository.cancelOrder(
                        currentOrder.getOrderId(),
                        (success, message) -> runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Đã hủy hóa đơn hiện tại.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, message == null ? "Không thể hủy hóa đơn." : message, Toast.LENGTH_SHORT).show();
                            }
                        })
                ))
                .show();
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
        tvSubtotal.setText(MoneyFormatter.format(subtotal));
        tvDiscount.setText(MoneyFormatter.format(appliedDiscountAmount));
        tvTotal.setText(MoneyFormatter.format(total));
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

    private void openPaymentScreen() {
        if (currentOrder == null) {
            Toast.makeText(this, "Bàn này chưa có hóa đơn.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ThanhToanKhachActivity.class);
        intent.putExtra(ThanhToanKhachActivity.EXTRA_ORDER_ID, currentOrder.getOrderId());
        intent.putExtra(ThanhToanKhachActivity.EXTRA_DISPLAY_ORDER_CODE, currentOrder.getDisplayOrderCode());
        intent.putExtra(ThanhToanKhachActivity.EXTRA_AMOUNT, currentOrder.getSubtotal());
        intent.putExtra(ThanhToanKhachActivity.EXTRA_TABLE_NAME, tableName);
        startActivity(intent);
    }

    private void showDemoBill() {
        if (currentOrder == null) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_demo_bill, null, false);
        bindDemoBill(dialogView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnDemoClose).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnDemoPrint).setOnClickListener(v -> {
            Toast.makeText(this, "Đã mở bản xem trước hóa đơn. Chưa kết nối máy in thật.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void bindDemoBill(@NonNull View dialogView) {
        TextView tvBillTableName = dialogView.findViewById(R.id.tvBillTableName);
        TextView tvBillDate = dialogView.findViewById(R.id.tvBillDate);
        TextView tvBillNumber = dialogView.findViewById(R.id.tvBillNumber);
        TextView tvBillCashier = dialogView.findViewById(R.id.tvBillCashier);
        TextView tvBillPrintedAt = dialogView.findViewById(R.id.tvBillPrintedAt);
        TextView tvBillTimeIn = dialogView.findViewById(R.id.tvBillTimeIn);
        TextView tvBillTimeOut = dialogView.findViewById(R.id.tvBillTimeOut);
        TextView tvBillSubtotal = dialogView.findViewById(R.id.tvBillSubtotal);
        TextView tvBillDiscount = dialogView.findViewById(R.id.tvBillDiscount);
        TextView tvBillGrandTotal = dialogView.findViewById(R.id.tvBillGrandTotal);
        LinearLayout layoutLines = dialogView.findViewById(R.id.layoutDemoBillLines);

        long createdMillis = currentOrder.getCreatedAtMillis();
        long printedMillis = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        tvBillTableName.setText("Bàn " + safe(tableName, "--"));
        tvBillDate.setText("Ngày: " + dateFormat.format(new Date(createdMillis)));
        tvBillNumber.setText("Số: " + currentOrder.getDisplayOrderCode());
        tvBillCashier.setText("Thu ngân: " + safe(sessionManager.getCurrentUserFullName(), "Nhân viên"));
        tvBillPrintedAt.setText("In lúc: " + timeFormat.format(new Date(printedMillis)));
        tvBillTimeIn.setText("Giờ vào: " + timeFormat.format(new Date(createdMillis)));
        tvBillTimeOut.setText("Giờ ra: " + timeFormat.format(new Date(printedMillis)));
        tvBillSubtotal.setText(MoneyFormatter.format(currentOrder.getSubtotal()));
        tvBillDiscount.setText(MoneyFormatter.format(appliedDiscountAmount));
        tvBillGrandTotal.setText(tvTotal.getText());

        layoutLines.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (LocalOrderItem item : currentOrder.getItems()) {
            View lineView = inflater.inflate(R.layout.item_demo_bill_line, layoutLines, false);
            TextView tvName = lineView.findViewById(R.id.tvDemoBillItemName);
            TextView tvQty = lineView.findViewById(R.id.tvDemoBillItemQty);
            TextView tvPrice = lineView.findViewById(R.id.tvDemoBillItemPrice);
            TextView tvLineTotal = lineView.findViewById(R.id.tvDemoBillItemTotal);

            String itemTitle = item.getProductName();
            if (!TextUtils.isEmpty(item.getVariantName())) {
                itemTitle = itemTitle + " (" + item.getVariantName() + ")";
            }
            tvName.setText(itemTitle);
            tvQty.setText(String.valueOf(item.getQty()));
            tvPrice.setText(MoneyFormatter.format(item.getUnitPrice()));
            tvLineTotal.setText(MoneyFormatter.format(item.getLineTotal()));
            layoutLines.addView(lineView);
        }
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
            String itemTitle = item.getProductName();
            if (!TextUtils.isEmpty(item.getVariantName())) {
                itemTitle = itemTitle + " (" + item.getVariantName() + ")";
            }
            tvName.setText(itemTitle);
            tvQty.setText(String.valueOf(item.getQty()));
            tvUnitPrice.setText(MoneyFormatter.format(item.getUnitPrice()));
            tvLineTotal.setText(MoneyFormatter.format(item.getLineTotal()));

            if (TextUtils.isEmpty(item.getNote())) {
                tvNote.setVisibility(View.GONE);
            } else {
                tvNote.setVisibility(View.VISIBLE);
                tvNote.setText("Ghi chú: " + item.getNote());
            }

            String resolvedImageUrl = resolveProductImageUrl(item);
            CfPlusImageLoader.load(imgProduct, resolvedImageUrl, R.drawable.cfplus4);
        }
    }

    @Nullable
    private String resolveProductImageUrl(@NonNull LocalOrderItem item) {
        if (!TextUtils.isEmpty(item.getImageUrl())) {
            return item.getImageUrl();
        }
        for (LocalProduct product : editableProducts) {
            boolean sameId = !TextUtils.isEmpty(item.getProductId()) && item.getProductId().equals(product.getProductId());
            boolean sameName = !TextUtils.isEmpty(item.getProductName())
                    && item.getProductName().trim().equalsIgnoreCase(product.getName() == null ? "" : product.getName().trim());
            if ((sameId || sameName) && !TextUtils.isEmpty(product.getImageUrl())) {
                return product.getImageUrl();
            }
        }
        return null;
    }
}
