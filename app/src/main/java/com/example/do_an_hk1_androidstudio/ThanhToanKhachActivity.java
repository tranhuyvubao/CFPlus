package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.PromotionCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalPromotion;
import com.example.do_an_hk1_androidstudio.payment.VnpayPaymentSessionStore;
import com.example.do_an_hk1_androidstudio.payment.VnpaySettingsStore;
import com.example.do_an_hk1_androidstudio.payment.VnpayUtils;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ThanhToanKhachActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_CUSTOMER_ONLINE_ONLY = "customer_online_only";

    private final List<LocalPromotion> promotions = new ArrayList<>();
    private TextView tvDiscountPreview;
    private TextView tvPayHint;
    private RadioGroup rgMethod;
    private EditText edtBankRef;
    private EditText edtPromoCode;
    private String orderId;
    private int amount;
    private boolean customerOnlineOnly;
    private OrderCloudRepository orderCloudRepository;
    private ListenerRegistration promotionsListener;
    private VnpayPaymentSessionStore paymentSessionStore;
    private VnpaySettingsStore vnpaySettingsStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thanh_toan_khach);
        InsetsHelper.applyActivityRootPadding(this);

        orderCloudRepository = new OrderCloudRepository(this);
        paymentSessionStore = new VnpayPaymentSessionStore(this);
        vnpaySettingsStore = new VnpaySettingsStore(this);
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
        TextView btnPay = findViewById(R.id.btnPayNow);

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        amount = getIntent().getIntExtra(EXTRA_AMOUNT, 0);
        customerOnlineOnly = getIntent().getBooleanExtra(EXTRA_CUSTOMER_ONLINE_ONLY, false);

        tvOrder.setText("Đơn: " + (orderId != null ? orderId : "-"));
        tvAmount.setText("Số tiền: " + MoneyFormatter.format(amount));
        tvDiscountPreview.setText("Giảm: " + MoneyFormatter.format(0));

        if (customerOnlineOnly) {
            TextView rbPayCash = findViewById(R.id.rbPayCash);
            View rbPayBank = findViewById(R.id.rbPayBank);
            if (rbPayCash != null) {
                rbPayCash.setText("Thanh toán khi nhận hàng (COD)");
            }
            if (rbPayBank != null) {
                rbPayBank.setVisibility(View.GONE);
            }
            rgMethod.check(R.id.rbPayCash);
        }

        rgMethod.setOnCheckedChangeListener((group, checkedId) -> updateMethodState());
        updateMethodState();
        btnPay.setOnClickListener(v -> doPay());
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

    private void updateMethodState() {
        int checkedId = rgMethod.getCheckedRadioButtonId();
        boolean isBank = checkedId == R.id.rbPayBank && !customerOnlineOnly;
        boolean isVnpay = checkedId == R.id.rbPayVnpay;
        edtBankRef.setVisibility(isBank ? View.VISIBLE : View.GONE);
        if (isVnpay) {
            tvPayHint.setText("Ứng dụng sẽ mở cổng thanh toán VNPAY sandbox, sau đó quay lại app để xác nhận.");
        } else if (isBank) {
            tvPayHint.setText("Nhập mã giao dịch chuyển khoản để lưu vào lịch sử thanh toán.");
        } else {
            tvPayHint.setText(customerOnlineOnly
                    ? "Đơn sẽ được ghi nhận ở trạng thái chờ thanh toán khi giao hàng."
                    : "Tiền mặt sẽ được ghi nhận ngay sau khi xác nhận.");
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

    @Nullable
    private PromotionResult resolvePromotion() {
        String promoCode = edtPromoCode.getText().toString().trim();
        if (TextUtils.isEmpty(promoCode)) {
            tvDiscountPreview.setText("Giảm: " + MoneyFormatter.format(0));
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
        tvDiscountPreview.setText("Giảm: " + MoneyFormatter.format(discount));
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
        if (!vnpaySettingsStore.isConfigured()) {
            Toast.makeText(this, "Hãy cấu hình TMN code, hash secret và return URL VNPAY sandbox trước.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, VnpaySandboxConfigActivity.class));
            return;
        }

        VnpaySettingsStore.MerchantConfig merchantConfig = vnpaySettingsStore.read();
        int finalAmount = Math.max(0, amount - promotionResult.discountAmount);
        paymentSessionStore.save(orderId, amount, promotionResult.discountAmount, promotionResult.promoCode);
        String paymentUrl = VnpayUtils.buildPaymentUrl(
                orderId,
                finalAmount,
                "Thanh toán đơn " + orderId,
                merchantConfig.tmnCode,
                merchantConfig.hashSecret,
                merchantConfig.returnUrl
        );
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl)));
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
                    finish();
                })
        );
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
