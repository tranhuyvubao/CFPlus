package com.example.do_an_hk1_androidstudio;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.PromotionCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.VnpayConfigCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalPromotion;
import com.example.do_an_hk1_androidstudio.payment.VnpayPaymentSessionStore;
import com.example.do_an_hk1_androidstudio.payment.VnpayUtils;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ThanhToanKhachActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_DISPLAY_ORDER_CODE = "display_order_code";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_TABLE_NAME = "table_name";
    public static final String EXTRA_CUSTOMER_ONLINE_ONLY = "customer_online_only";
    public static final String EXTRA_INITIAL_PAYMENT_METHOD = "initial_payment_method";
    public static final String EXTRA_AUTO_SUBMIT_PAYMENT = "auto_submit_payment";

    private final List<LocalPromotion> promotions = new ArrayList<>();
    private TextView tvDiscountPreview;
    private TextView tvPayHint;
    private RadioGroup rgMethod;
    private EditText edtBankRef;
    private EditText edtPromoCode;
    private String orderId;
    private String displayOrderCode;
    private String initialPaymentMethod;
    private int amount;
    private boolean customerOnlineOnly;
    private OrderCloudRepository orderCloudRepository;
    private ListenerRegistration promotionsListener;
    private ListenerRegistration orderListener;
    private VnpayPaymentSessionStore paymentSessionStore;
    private VnpayConfigCloudRepository vnpayConfigCloudRepository;
    private boolean autoSubmitPayment;
    private boolean autoSubmitHandled;
    private LocalSessionManager sessionManager;
    @Nullable
    private LocalOrder currentOrder;
    @Nullable
    private String currentTableName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thanh_toan_khach);
        InsetsHelper.applyActivityRootPadding(this);

        sessionManager = new LocalSessionManager(this);
        orderCloudRepository = new OrderCloudRepository(this);
        paymentSessionStore = new VnpayPaymentSessionStore(this);
        vnpayConfigCloudRepository = new VnpayConfigCloudRepository(this);
        listenPromotions();

        View tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        TextView tvOrder = findViewById(R.id.tvPayOrderId);
        TextView tvAmount = findViewById(R.id.tvPayAmount);
        tvDiscountPreview = findViewById(R.id.tvDiscountPreview);
        tvPayHint = findViewById(R.id.tvPayHint);
        rgMethod = findViewById(R.id.rgPayMethod);
        edtBankRef = findViewById(R.id.edtBankRef);
        edtPromoCode = findViewById(R.id.edtPromoCode);
        TextView btnViewBill = findViewById(R.id.btnViewBill);
        TextView btnPay = findViewById(R.id.btnPayNow);

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        displayOrderCode = getIntent().getStringExtra(EXTRA_DISPLAY_ORDER_CODE);
        currentTableName = getIntent().getStringExtra(EXTRA_TABLE_NAME);
        initialPaymentMethod = getIntent().getStringExtra(EXTRA_INITIAL_PAYMENT_METHOD);
        amount = getIntent().getIntExtra(EXTRA_AMOUNT, 0);
        customerOnlineOnly = getIntent().getBooleanExtra(EXTRA_CUSTOMER_ONLINE_ONLY, false);
        autoSubmitPayment = getIntent().getBooleanExtra(EXTRA_AUTO_SUBMIT_PAYMENT, false);

        tvOrder.setText("Don: " + buildDisplayOrderCode());
        tvAmount.setText("So tien: " + MoneyFormatter.format(amount));
        tvDiscountPreview.setText("Giam: " + MoneyFormatter.format(0));

        if (customerOnlineOnly) {
            TextView rbPayCash = findViewById(R.id.rbPayCash);
            View rbPayBank = findViewById(R.id.rbPayBank);
            if (rbPayCash != null) {
                rbPayCash.setText("Thanh toan khi nhan hang (COD)");
            }
            if (rbPayBank != null) {
                rbPayBank.setVisibility(View.GONE);
            }
            rgMethod.check("vnpay".equals(initialPaymentMethod) ? R.id.rbPayVnpay : R.id.rbPayCash);
        } else if ("vnpay".equals(initialPaymentMethod)) {
            rgMethod.check(R.id.rbPayVnpay);
        } else if ("bank".equals(initialPaymentMethod)) {
            rgMethod.check(R.id.rbPayBank);
        }

        rgMethod.setOnCheckedChangeListener((group, checkedId) -> updateMethodState());
        updateMethodState();
        btnViewBill.setOnClickListener(v -> openBillPreview());
        btnPay.setOnClickListener(v -> doPay());
        listenOrderDetails();

        if (savedInstanceState != null) {
            autoSubmitHandled = savedInstanceState.getBoolean("auto_submit_handled", false);
        }
        maybeAutoSubmit();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("auto_submit_handled", autoSubmitHandled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (promotionsListener != null) {
            promotionsListener.remove();
        }
        if (orderListener != null) {
            orderListener.remove();
        }
    }

    private void listenPromotions() {
        promotionsListener = new PromotionCloudRepository(this).listenPromotions(fetched -> {
            promotions.clear();
            promotions.addAll(fetched);
        });
    }

    private void listenOrderDetails() {
        orderListener = orderCloudRepository.listenAllOrders(fetchedOrders -> runOnUiThread(() -> {
            LocalOrder matchedOrder = null;
            for (LocalOrder order : fetchedOrders) {
                if (orderId != null && orderId.equals(order.getOrderId())) {
                    matchedOrder = order;
                    break;
                }
            }
            currentOrder = matchedOrder;
            if (matchedOrder != null) {
                currentTableName = !TextUtils.isEmpty(matchedOrder.getTableName()) ? matchedOrder.getTableName() : currentTableName;
                amount = matchedOrder.getSubtotal() > 0 ? matchedOrder.getSubtotal() : amount;
                TextView tvAmount = findViewById(R.id.tvPayAmount);
                tvAmount.setText("So tien: " + MoneyFormatter.format(amount));
            }
        }));
    }

    private void updateMethodState() {
        int checkedId = rgMethod.getCheckedRadioButtonId();
        boolean isBank = checkedId == R.id.rbPayBank && !customerOnlineOnly;
        boolean isVnpay = checkedId == R.id.rbPayVnpay;
        edtBankRef.setVisibility(isBank ? View.VISIBLE : View.GONE);
        if (isVnpay) {
            tvPayHint.setText("Ung dung se mo cong thanh toan VNPAY sandbox, sau do quay lai app de xac nhan.");
        } else if (isBank) {
            tvPayHint.setText("Nhập mã giao dịch chuyển khoản để lưu vào lịch sử thanh toán.");
        } else {
            tvPayHint.setText(customerOnlineOnly
                    ? "Don se duoc ghi nhan o trang thai cho thanh toan khi giao hang."
                    : "Tien mat se duoc ghi nhan ngay sau khi xac nhan.");
        }
    }

    private void doPay() {
        if (TextUtils.isEmpty(orderId)) {
            Toast.makeText(this, "Thiếu mã đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        PromotionResult promotionResult = resolvePromotion();
        if (promotionResult == null) {
            return;
        }

        int checkedId = rgMethod.getCheckedRadioButtonId();
        if (checkedId == R.id.rbPayVnpay) {
            startVnpayPayment(promotionResult);
            return;
        }

        String method = checkedId == R.id.rbPayBank ? "bank" : "cash";
        String bankRef = edtBankRef.getText().toString().trim();
        if ("bank".equals(method) && TextUtils.isEmpty(bankRef)) {
            Toast.makeText(this, "Vui lòng nhập mã giao dịch", Toast.LENGTH_SHORT).show();
            return;
        }
        payWithDiscount(method, bankRef, promotionResult.discountAmount, promotionResult.promoCode);
    }

    private void maybeAutoSubmit() {
        if (autoSubmitHandled || !autoSubmitPayment) {
            return;
        }
        if (!"vnpay".equals(initialPaymentMethod)) {
            return;
        }
        autoSubmitHandled = true;
        rgMethod.check(R.id.rbPayVnpay);
        doPay();
    }

    @Nullable
    private PromotionResult resolvePromotion() {
        String promoCode = edtPromoCode.getText().toString().trim();
        if (TextUtils.isEmpty(promoCode)) {
            tvDiscountPreview.setText("Giam: " + MoneyFormatter.format(0));
            return new PromotionResult(0, null);
        }

        LocalPromotion promotion = findPromotionByCode(promoCode);
        if (promotion == null || !promotion.isActive()) {
            Toast.makeText(this, "Mã giảm giá không hợp lệ", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (amount < promotion.getMinOrder()) {
            Toast.makeText(this, "Đơn chưa đạt tối thiểu để dùng mã", Toast.LENGTH_SHORT).show();
            return null;
        }

        int discount;
        if ("percent".equalsIgnoreCase(promotion.getType())) {
            discount = (int) ((amount * promotion.getValue()) / 100.0);
            if (promotion.getMaxDiscount() != null) {
                discount = Math.min(discount, promotion.getMaxDiscount());
            }
        } else {
            discount = (int) promotion.getValue();
        }
        discount = Math.max(0, Math.min(discount, amount));
        tvDiscountPreview.setText("Giam: " + MoneyFormatter.format(discount));
        return new PromotionResult(discount, promotion.getCode());
    }

    @Nullable
    private LocalPromotion findPromotionByCode(String code) {
        for (LocalPromotion promotion : promotions) {
            if (promotion.getCode() != null && promotion.getCode().equalsIgnoreCase(code.trim())) {
                return promotion;
            }
        }
        return null;
    }

    private void startVnpayPayment(PromotionResult promotionResult) {
        vnpayConfigCloudRepository.getConfig((cloudConfig, message) -> runOnUiThread(() -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                return;
            }
            if (!isConfigured(cloudConfig)) {
                Toast.makeText(this, "Hay cau hinh TMN code, hash secret va return URL VNPAY sandbox truoc.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, VnpaySandboxConfigActivity.class));
                return;
            }
            launchVnpayPayment(promotionResult, cloudConfig);
        }));
    }

    private void launchVnpayPayment(PromotionResult promotionResult,
                                    VnpayConfigCloudRepository.MerchantConfig merchantConfig) {
        int finalAmount = Math.max(0, amount - promotionResult.discountAmount);
        paymentSessionStore.save(
                orderId,
                amount,
                promotionResult.discountAmount,
                promotionResult.promoCode,
                merchantConfig.tmnCode,
                merchantConfig.hashSecret,
                merchantConfig.returnUrl
        );
        String paymentUrl = VnpayUtils.buildPaymentUrl(
                orderId,
                finalAmount,
                "Thanh toán đơn " + buildDisplayOrderCode(),
                merchantConfig.tmnCode,
                merchantConfig.hashSecret,
                merchantConfig.returnUrl
        );
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl)));
    }

    private boolean isConfigured(@Nullable VnpayConfigCloudRepository.MerchantConfig config) {
        return config != null
                && !TextUtils.isEmpty(config.tmnCode)
                && !TextUtils.isEmpty(config.hashSecret)
                && !TextUtils.isEmpty(config.returnUrl);
    }

    private void payWithDiscount(String method, String bankRef, int discountAmount, @Nullable String promoCode) {
        orderCloudRepository.payOrder(
                orderId,
                amount,
                method,
                TextUtils.isEmpty(bankRef) ? null : bankRef,
                discountAmount,
                promoCode,
                (success, message) -> runOnUiThread(() -> {
                    if (!success) {
                        Toast.makeText(this, message == null ? "Không thể thanh toán đơn hàng" : message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                    showDemoBill(new PromotionResult(discountAmount, promoCode), true);
                })
        );
    }

    private void openBillPreview() {
        if (currentOrder == null) {
            Toast.makeText(this, "Bill đang được tải. Thử lại sau giây lát.", Toast.LENGTH_SHORT).show();
            return;
        }
        PromotionResult previewPromotion = resolvePromotion();
        if (previewPromotion == null) {
            return;
        }
        showDemoBill(previewPromotion, false);
    }

    private void showDemoBill(@NonNull PromotionResult promotionResult, boolean finishAfterClose) {
        if (currentOrder == null) {
            if (finishAfterClose) {
                finish();
            }
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_demo_bill, null, false);
        bindDemoBill(dialogView, promotionResult);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnDemoClose).setOnClickListener(v -> {
            dialog.dismiss();
            if (finishAfterClose) {
                finish();
            }
        });
        TextView btnDemoPrint = dialogView.findViewById(R.id.btnDemoPrint);
        if (finishAfterClose) {
            btnDemoPrint.setText("Thanh toán thành công");
        } else {
            btnDemoPrint.setText("In hóa đơn");
        }
        btnDemoPrint.setOnClickListener(v -> {
            if (!finishAfterClose) {
                Toast.makeText(this, "Đã mở bản xem trước hóa đơn. Chưa kết nối máy in thật.", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            if (finishAfterClose) {
                finish();
            }
        });

        dialog.setOnCancelListener(dialogInterface -> {
            if (finishAfterClose) {
                finish();
            }
        });
        dialog.show();
    }

    private void bindDemoBill(@NonNull View dialogView, @NonNull PromotionResult promotionResult) {
        if (currentOrder == null) {
            return;
        }

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
        int subtotal = currentOrder.getSubtotal() > 0 ? currentOrder.getSubtotal() : amount;
        int discountAmount = Math.max(0, promotionResult.discountAmount);
        int grandTotal = Math.max(0, subtotal - discountAmount);

        tvBillTableName.setText(buildBillTargetLabel());
        tvBillDate.setText("Ngày: " + dateFormat.format(new Date(createdMillis)));
        tvBillNumber.setText("So: " + buildDisplayOrderCode());
        tvBillCashier.setText("Thu ngan: " + safe(sessionManager.getCurrentUserFullName(), "Nhan vien"));
        tvBillPrintedAt.setText("In luc: " + timeFormat.format(new Date(printedMillis)));
        tvBillTimeIn.setText("Giờ vào: " + timeFormat.format(new Date(createdMillis)));
        tvBillTimeOut.setText("Giờ ra: " + timeFormat.format(new Date(printedMillis)));
        tvBillSubtotal.setText(MoneyFormatter.format(subtotal));
        tvBillDiscount.setText(MoneyFormatter.format(discountAmount));
        tvBillGrandTotal.setText(MoneyFormatter.format(grandTotal));

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

    @NonNull
    private String buildBillTargetLabel() {
        if (!TextUtils.isEmpty(currentTableName)) {
            return "Ban " + currentTableName;
        }
        if (currentOrder != null && !TextUtils.isEmpty(currentOrder.getDeliveryAddressText())) {
            return "Don online";
        }
        return "Khach le";
    }

    @NonNull
    private String safe(@Nullable String primary, @Nullable String fallback) {
        if (!TextUtils.isEmpty(primary)) {
            return primary;
        }
        return fallback == null ? "-" : fallback;
    }

    private String buildDisplayOrderCode() {
        if (!TextUtils.isEmpty(displayOrderCode)) {
            return displayOrderCode.trim();
        }
        if (TextUtils.isEmpty(orderId)) {
            return "-";
        }
        String normalized = orderId.trim();
        if (normalized.startsWith("online_order_")) {
            normalized = normalized.substring("online_order_".length());
        } else if (normalized.startsWith("cloud_order_")) {
            normalized = normalized.substring("cloud_order_".length());
        } else if (normalized.startsWith("web_order_")) {
            normalized = normalized.substring("web_order_".length());
        } else if (normalized.startsWith("wb_order_")) {
            normalized = normalized.substring("wb_order_".length());
        } else if (normalized.startsWith("order_")) {
            normalized = normalized.substring("order_".length());
        }
        if (normalized.length() > 10) {
            normalized = normalized.substring(normalized.length() - 10);
        }
        return "#" + normalized.toUpperCase();
    }

    private static class PromotionResult {
        final int discountAmount;
        @Nullable
        final String promoCode;

        PromotionResult(int discountAmount, @Nullable String promoCode) {
            this.discountAmount = discountAmount;
            this.promoCode = promoCode;
        }
    }
}
