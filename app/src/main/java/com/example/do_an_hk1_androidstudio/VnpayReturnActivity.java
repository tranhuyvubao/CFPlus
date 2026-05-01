package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.VnpayConfigCloudRepository;
import com.example.do_an_hk1_androidstudio.payment.VnpayPaymentSessionStore;
import com.example.do_an_hk1_androidstudio.payment.VnpayUtils;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

public class VnpayReturnActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvDetail;
    private OrderCloudRepository orderRepository;
    private VnpayPaymentSessionStore paymentSessionStore;
    private VnpayConfigCloudRepository cloudConfigRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_return);
        InsetsHelper.applyActivityRootPadding(this);

        tvStatus = findViewById(R.id.tvVnpayStatus);
        tvDetail = findViewById(R.id.tvVnpayDetail);
        orderRepository = new OrderCloudRepository(this);
        paymentSessionStore = new VnpayPaymentSessionStore(this);
        cloudConfigRepository = new VnpayConfigCloudRepository(this);

        findViewById(R.id.btnVnpayClose).setOnClickListener(v -> openHome());
        handleReturnIntent(getIntent() == null ? null : getIntent().getData());
    }

    private void handleReturnIntent(@Nullable Uri data) {
        if (data == null) {
            showResult("Khong nhan duoc du lieu tra ve", "VNPAY chua tra ket qua ve ung dung.");
            return;
        }
        VnpayPaymentSessionStore.PendingPayment pendingPayment = paymentSessionStore.read();
        if (pendingPayment != null && pendingPayment.hasMerchantConfig()) {
            continueHandleReturn(data, new VnpayConfigCloudRepository.MerchantConfig(
                    pendingPayment.tmnCode,
                    pendingPayment.hashSecret,
                    pendingPayment.returnUrl
            ));
            return;
        }
        cloudConfigRepository.getConfig((cloudConfig, message) -> runOnUiThread(() -> {
            if (message != null) {
                showResult("Khong doc duoc cau hinh VNPAY", message);
                return;
            }
            if (!isConfigured(cloudConfig)) {
                showResult("Thieu cau hinh VNPAY", "Hay cap nhat TMN code, hash secret va return URL trong cau hinh sandbox.");
                return;
            }
            continueHandleReturn(data, cloudConfig);
        }));
    }

    private void continueHandleReturn(Uri data, VnpayConfigCloudRepository.MerchantConfig merchantConfig) {
        if (!isConfigured(merchantConfig)) {
            showResult("Thieu cau hinh VNPAY", "Hay cap nhat TMN code, hash secret va return URL trong cau hinh sandbox.");
            return;
        }
        if (!VnpayUtils.isValidReturnSignature(data, merchantConfig.hashSecret)) {
            showResult("Chu ky khong hop le", "Ung dung da chan ket qua thanh toan vi chu ky phan hoi khong khop.");
            return;
        }

        VnpayPaymentSessionStore.PendingPayment pendingPayment = paymentSessionStore.read();
        if (pendingPayment == null) {
            showResult("Khong tim thay phien thanh toan", "Ung dung khong con luu don cho thanh toan qua VNPAY.");
            return;
        }

        String txnRef = VnpayUtils.readQuery(data, "vnp_TxnRef");
        if (!pendingPayment.orderId.equals(txnRef)) {
            showResult("Sai ma don", "Ma don tra ve tu VNPAY khong khop voi don dang cho.");
            return;
        }

        String responseCode = VnpayUtils.readQuery(data, "vnp_ResponseCode");
        String transactionNo = VnpayUtils.readQuery(data, "vnp_TransactionNo");
        if (!"00".equals(responseCode)) {
            showResult("Thanh toan chua thanh cong", "VNPAY tra ve ma phan hoi: " + (TextUtils.isEmpty(responseCode) ? "-" : responseCode));
            return;
        }

        tvStatus.setText("Dang xac nhan thanh toan...");
        tvDetail.setText("Ung dung dang cap nhat hoa don sau khi VNPAY bao thanh cong.");

        orderRepository.payOrder(
                pendingPayment.orderId,
                pendingPayment.amount,
                "vnpay",
                TextUtils.isEmpty(transactionNo) ? null : transactionNo,
                pendingPayment.discountAmount,
                pendingPayment.promoCode,
                (success, message) -> runOnUiThread(() -> {
                    if (!success) {
                        showResult("Khong the cap nhat don", message == null ? "VNPAY da tra thanh cong nhung app chua cap nhat duoc hoa don." : message);
                        return;
                    }
                    paymentSessionStore.clear();
                    showResult("Thanh toan thanh cong", "Don da duoc xac nhan thanh toan qua VNPAY. Dang quay ve trang chu...");
                    tvStatus.postDelayed(this::openHome, 900);
                })
        );
    }

    private boolean isConfigured(@Nullable VnpayConfigCloudRepository.MerchantConfig config) {
        return config != null
                && !TextUtils.isEmpty(config.tmnCode)
                && !TextUtils.isEmpty(config.hashSecret)
                && !TextUtils.isEmpty(config.returnUrl);
    }

    private void showResult(String title, String detail) {
        tvStatus.setText(title);
        tvDetail.setText(detail);
    }

    private void openHome() {
        Intent intent = new Intent(this, trangchu.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
