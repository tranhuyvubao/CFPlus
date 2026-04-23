package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.CustomerCartStore;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.CustomerCartItem;
import com.example.do_an_hk1_androidstudio.local.model.LocalCustomerAddress;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;

import java.util.ArrayList;
import java.util.List;

public class DatMonOnlineActivity extends AppCompatActivity {
    private static final long ORDER_CREATE_TIMEOUT_MS = 15000L;

    private LinearLayout cartItemsContainer;
    private TextView tvSelectedAddress;
    private TextView tvCartSummary;
    private TextView tvEmptyCart;
    private TextView edtOrderNote;
    private RadioGroup rgPaymentMethod;
    private TextView btnPlaceOnlineOrder;

    private final List<LocalCustomerAddress> currentAddresses = new ArrayList<>();
    private CustomerCartStore cartStore;
    private OrderCloudRepository orderRepository;
    private LocalSessionManager sessionManager;
    private UserCloudRepository userRepository;
    private LocalCustomerAddress selectedAddress;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable orderCreateTimeoutRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dat_mon_online);
        InsetsHelper.applySystemBarsPadding(findViewById(R.id.scrollContent));

        cartStore = new CustomerCartStore(this);
        orderRepository = new OrderCloudRepository(this);
        sessionManager = new LocalSessionManager(this);
        userRepository = new UserCloudRepository(this);

        View tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        cartItemsContainer = findViewById(R.id.cartItemsContainer);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        tvCartSummary = findViewById(R.id.tvCartSummary);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        edtOrderNote = findViewById(R.id.edtOrderNote);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        btnPlaceOnlineOrder = findViewById(R.id.btnPlaceOnlineOrder);

        findViewById(R.id.btnChooseAddress).setOnClickListener(v -> showAddressChooser());
        findViewById(R.id.btnAddAddress).setOnClickListener(v ->
                startActivity(new Intent(this, HoSoKhachHangActivity.class)));
        btnPlaceOnlineOrder.setOnClickListener(v -> submitOrder());

        rgPaymentMethod.check(R.id.rbPaymentCod);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
        renderCart();
    }

    private void loadAddresses() {
        String customerId = sessionManager.getCurrentUserId();
        if (customerId == null) {
            return;
        }
        userRepository.getCustomerAddresses(customerId, (addresses, message) -> runOnUiThread(() -> {
            currentAddresses.clear();
            currentAddresses.addAll(addresses);
            LocalCustomerAddress previousSelection = selectedAddress;
            selectedAddress = null;
            if (previousSelection != null) {
                for (LocalCustomerAddress address : currentAddresses) {
                    if (address.getAddressId().equals(previousSelection.getAddressId())) {
                        selectedAddress = address;
                        break;
                    }
                }
            }
            if (selectedAddress == null) {
                for (LocalCustomerAddress address : currentAddresses) {
                    if (address.isDefault()) {
                        selectedAddress = address;
                        break;
                    }
                }
            }
            if (selectedAddress == null && !currentAddresses.isEmpty()) {
                selectedAddress = currentAddresses.get(0);
            }
            bindSelectedAddress();
        }));
    }

    private void bindSelectedAddress() {
        if (selectedAddress == null) {
            tvSelectedAddress.setText("Bạn chưa có địa chỉ giao hàng. Hãy thêm địa chỉ để tiếp tục.");
            return;
        }
        tvSelectedAddress.setText(selectedAddress.getLabel() + "\n" + selectedAddress.buildDisplayAddress());
    }

    private void showAddressChooser() {
        if (currentAddresses.isEmpty()) {
            startActivity(new Intent(this, HoSoKhachHangActivity.class));
            Toast.makeText(this, "Hãy thêm địa chỉ giao hàng trước khi đặt online.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (LocalCustomerAddress address : currentAddresses) {
            labels.add(address.getLabel() + " - " + address.buildDisplayAddress());
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn địa chỉ giao hàng")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    selectedAddress = currentAddresses.get(which);
                    bindSelectedAddress();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void renderCart() {
        List<CustomerCartItem> items = cartStore.getItems();
        cartItemsContainer.removeAllViews();
        tvEmptyCart.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

        for (CustomerCartItem item : items) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_cart_checkout, cartItemsContainer, false);
            android.widget.ImageView imgCartItem = itemView.findViewById(R.id.imgCartItem);
            TextView tvCartItemName = itemView.findViewById(R.id.tvCartItemName);
            TextView tvCartItemVariant = itemView.findViewById(R.id.tvCartItemVariant);
            TextView tvCartItemNote = itemView.findViewById(R.id.tvCartItemNote);
            TextView tvCartItemLineTotal = itemView.findViewById(R.id.tvCartItemLineTotal);
            TextView tvCartItemQty = itemView.findViewById(R.id.tvCartItemQty);
            TextView btnDecreaseQty = itemView.findViewById(R.id.btnDecreaseQty);
            TextView btnIncreaseQty = itemView.findViewById(R.id.btnIncreaseQty);

            tvCartItemName.setText(item.getProductName());
            String variantLabel = item.buildVariantLabel();
            tvCartItemVariant.setVisibility(TextUtils.isEmpty(variantLabel) ? View.GONE : View.VISIBLE);
            tvCartItemVariant.setText(variantLabel);
            tvCartItemNote.setVisibility(TextUtils.isEmpty(item.getNote()) ? View.GONE : View.VISIBLE);
            tvCartItemNote.setText(item.getNote());
            tvCartItemLineTotal.setText(MoneyFormatter.format(item.getLineTotal()));
            tvCartItemQty.setText(String.valueOf(item.getQuantity()));

            Glide.with(this)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.cfplus)
                    .error(R.drawable.cfplus)
                    .into(imgCartItem);

            btnDecreaseQty.setOnClickListener(v -> {
                cartStore.updateQuantity(item.getCartItemId(), item.getQuantity() - 1);
                renderCart();
            });
            btnIncreaseQty.setOnClickListener(v -> {
                cartStore.updateQuantity(item.getCartItemId(), item.getQuantity() + 1);
                renderCart();
            });

            cartItemsContainer.addView(itemView);
        }

        tvCartSummary.setText(cartStore.getItemCount() + " món • " + MoneyFormatter.format(cartStore.getSubtotal()));
    }

    private void submitOrder() {
        if (!btnPlaceOnlineOrder.isEnabled()) {
            return;
        }

        String customerId = sessionManager.getCurrentUserId();
        if (customerId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt món online.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<CustomerCartItem> items = cartStore.getItems();
        if (items.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAddress == null) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentPreference = rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbPaymentVnpay
                ? "vnpay"
                : "cod";

        String note = edtOrderNote.getText().toString().trim();
        setSubmitting(true);

        final boolean[] handled = {false};
        orderCreateTimeoutRunnable = () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            setSubmitting(false);
            Toast.makeText(
                    this,
                    "Không kết nối được Firestore. Kiểm tra DNS/mạng emulator rồi thử lại.",
                    Toast.LENGTH_LONG
            ).show();
        };
        mainHandler.postDelayed(orderCreateTimeoutRunnable, ORDER_CREATE_TIMEOUT_MS);

        orderRepository.createOnlineCartOrder(
                customerId,
                items,
                selectedAddress,
                TextUtils.isEmpty(note) ? null : note,
                paymentPreference,
                (success, orderId, message) -> runOnUiThread(() -> {
                    if (handled[0]) {
                        return;
                    }
                    handled[0] = true;
                    if (orderCreateTimeoutRunnable != null) {
                        mainHandler.removeCallbacks(orderCreateTimeoutRunnable);
                        orderCreateTimeoutRunnable = null;
                    }
                    if (!success || TextUtils.isEmpty(orderId)) {
                        setSubmitting(false);
                        Toast.makeText(this, message == null ? "Không thể tạo đơn online." : message, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    cartStore.clear();
                    edtOrderNote.setText("");
                    renderCart();

                    if ("vnpay".equals(paymentPreference)) {
                        Intent intent = new Intent(this, ThanhToanKhachActivity.class);
                        intent.putExtra(ThanhToanKhachActivity.EXTRA_ORDER_ID, orderId);
                        intent.putExtra(ThanhToanKhachActivity.EXTRA_DISPLAY_ORDER_CODE, orderId);
                        intent.putExtra(ThanhToanKhachActivity.EXTRA_AMOUNT, itemsTotal(items));
                        intent.putExtra(ThanhToanKhachActivity.EXTRA_CUSTOMER_ONLINE_ONLY, true);
                        intent.putExtra(ThanhToanKhachActivity.EXTRA_INITIAL_PAYMENT_METHOD, "vnpay");
                        startActivity(intent);
                        finish();
                        return;
                    }

                    Toast.makeText(this, "Đơn online đã được tạo. Bạn sẽ thanh toán khi nhận hàng.", Toast.LENGTH_LONG).show();
                    finish();
                })
        );
    }

    private void setSubmitting(boolean submitting) {
        btnPlaceOnlineOrder.setEnabled(!submitting);
        btnPlaceOnlineOrder.setAlpha(submitting ? 0.72f : 1f);
        btnPlaceOnlineOrder.setText(submitting ? "Đang tạo đơn..." : "Đặt đơn online");
    }

    private int itemsTotal(List<CustomerCartItem> items) {
        int total = 0;
        for (CustomerCartItem item : items) {
            total += item.getLineTotal();
        }
        return total;
    }
}
