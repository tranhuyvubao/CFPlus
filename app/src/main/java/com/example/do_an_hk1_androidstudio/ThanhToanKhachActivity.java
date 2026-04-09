package com.example.do_an_hk1_androidstudio;

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
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ThanhToanKhachActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_AMOUNT = "amount";

    private final List<LocalPromotion> promotions = new ArrayList<>();
    private TextView tvDiscountPreview;
    private RadioGroup rgMethod;
    private EditText edtBankRef;
    private EditText edtPromoCode;
    private TextView tvPayHint;
    private String orderId;
    private int amount;
    private OrderCloudRepository orderCloudRepository;
    private ListenerRegistration promotionsListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thanh_toan_khach);
        InsetsHelper.applyActivityRootPadding(this);

        orderCloudRepository = new OrderCloudRepository(this);
        listenPromotions();

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        TextView tvOrder = findViewById(R.id.tvPayOrderId);
        TextView tvAmount = findViewById(R.id.tvPayAmount);
        tvDiscountPreview = findViewById(R.id.tvDiscountPreview);
        rgMethod = findViewById(R.id.rgPayMethod);
        edtBankRef = findViewById(R.id.edtBankRef);
        edtPromoCode = findViewById(R.id.edtPromoCode);
        TextView btnPay = findViewById(R.id.btnPayNow);

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        amount = getIntent().getIntExtra(EXTRA_AMOUNT, 0);

        tvOrder.setText("Đơn: " + (orderId != null ? orderId : "-"));
        tvAmount.setText("Số tiền: " + amount + "đ");
        tvDiscountPreview.setText("Giảm: 0đ");
        tvPayHint = findViewById(R.id.tvPayHint);

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
        boolean isBank = rgMethod.getCheckedRadioButtonId() == R.id.rbPayBank;
        edtBankRef.setVisibility(isBank ? View.VISIBLE : View.GONE);
        if (tvPayHint != null) {
            tvPayHint.setText(isBank
                    ? "Nhập mã giao dịch chuyển khoản để lưu vào lịch sử thanh toán."
                    : "Tiền mặt sẽ được ghi nhận ngay sau khi xác nhận.");
        }
    }

    private void doPay() {
        if (TextUtils.isEmpty(orderId)) {
            Toast.makeText(this, "Thiếu mã đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String method = rgMethod.getCheckedRadioButtonId() == R.id.rbPayBank ? "bank" : "cash";
        String bankRef = edtBankRef.getText().toString().trim();
        if ("bank".equals(method) && TextUtils.isEmpty(bankRef)) {
            Toast.makeText(this, "Vui lòng nhập mã giao dịch", Toast.LENGTH_SHORT).show();
            return;
        }

        String promoCode = edtPromoCode.getText().toString().trim();
        if (TextUtils.isEmpty(promoCode)) {
            payWithDiscount(method, bankRef, 0, null);
            return;
        }

        LocalPromotion promotion = findPromotionByCode(promoCode);
        if (promotion == null || !promotion.isActive()) {
            Toast.makeText(this, "Mã giảm giá không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount < promotion.getMinOrder()) {
            Toast.makeText(this, "Đơn chưa đạt tối thiểu để dùng mã", Toast.LENGTH_SHORT).show();
            return;
        }

        int discount = 0;
        if ("percent".equals(promotion.getType())) {
            discount = (int) ((amount * promotion.getValue()) / 100.0);
            if (promotion.getMaxDiscount() != null) {
                discount = Math.min(discount, promotion.getMaxDiscount());
            }
        } else {
            discount = (int) promotion.getValue();
        }
        discount = Math.max(0, Math.min(discount, amount));
        tvDiscountPreview.setText("Giảm: " + discount + "đ");
        payWithDiscount(method, bankRef, discount, promotion.getCode());
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
}
