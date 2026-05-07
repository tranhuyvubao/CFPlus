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
            showResult("Không nhận được dữ liệu trả về", "VNPAY chưa trả kết quả về ứng dụng.");
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
                showResult("Không đọc được cấu hình VNPAY", message);
                return;
            }
            if (!isConfigured(cloudConfig)) {
                showResult("Thiếu cấu hình VNPAY", "Hãy cập nhật TMN code, hash secret và return URL trong cấu hình sandbox.");
                return;
            }
            continueHandleReturn(data, cloudConfig);
        }));
    }

    private void continueHandleReturn(Uri data, VnpayConfigCloudRepository.MerchantConfig merchantConfig) {
        if (!isConfigured(merchantConfig)) {
            showResult("Thiếu cấu hình VNPAY", "Hãy cập nhật TMN code, hash secret và return URL trong cấu hình sandbox.");
            return;
        }
        if (!VnpayUtils.isValidReturnSignature(data, merchantConfig.hashSecret)) {
            showResult("Chữ ký không hợp lệ", "Ứng dụng đã chặn kết quả thanh toán vì chữ ký phản hồi không khớp.");
            return;
        }

        VnpayPaymentSessionStore.PendingPayment pendingPayment = paymentSessionStore.read();
        if (pendingPayment == null) {
            showResult("Không tìm thấy phiên thanh toán", "Ứng dụng không còn lưu đơn chờ thanh toán qua VNPAY.");
            return;
        }

        String txnRef = VnpayUtils.readQuery(data, "vnp_TxnRef");
        if (!pendingPayment.orderId.equals(txnRef)) {
            showResult("Sai mã đơn", "Mã đơn trả về từ VNPAY không khớp với đơn đang chờ.");
            return;
        }

        String responseCode = VnpayUtils.readQuery(data, "vnp_ResponseCode");
        String transactionNo = VnpayUtils.readQuery(data, "vnp_TransactionNo");
        if (!"00".equals(responseCode)) {
            showResult("Thanh toán chưa thành công", "VNPAY trả về mã phản hồi: " + (TextUtils.isEmpty(responseCode) ? "-" : responseCode));
            return;
        }

        tvStatus.setText("Đang xác nhận thanh toán...");
        tvDetail.setText("Ứng dụng đang cập nhật hóa đơn sau khi VNPAY báo thành công.");

        orderRepository.payOrder(
                pendingPayment.orderId,
                pendingPayment.amount,
                "vnpay",
                TextUtils.isEmpty(transactionNo) ? null : transactionNo,
                pendingPayment.discountAmount,
                pendingPayment.promoCode,
                (success, message) -> runOnUiThread(() -> {
                    if (!success) {
                        showResult("Không thể cập nhật đơn", message == null ? "VNPAY đã trả thành công nhưng app chưa cập nhật được hóa đơn." : message);
                        return;
                    }
                    paymentSessionStore.clear();
                    showResult("Thanh toán thành công", "Đơn đã được xác nhận thanh toán qua VNPAY. Đang quay về trang chủ...");
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
