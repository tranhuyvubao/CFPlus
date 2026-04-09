package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalCustomerAddress;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class DatMonOnlineActivity extends AppCompatActivity {

    private EditText edtTenMon;
    private EditText edtGia;
    private EditText edtSoLuong;
    private EditText edtSize;
    private EditText edtGhiChu;
    private EditText edtImageUrl;
    private TextView tvSelectedAddress;
    private String selectedProductId;
    private final List<LocalProduct> activeProducts = new ArrayList<>();
    private CatalogCloudRepository catalogRepository;
    private OrderCloudRepository orderRepository;
    private LocalSessionManager sessionManager;
    private UserCloudRepository userRepository;
    private LocalCustomerAddress selectedAddress;
    private List<LocalCustomerAddress> currentAddresses = new ArrayList<>();
    private ListenerRegistration productsListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dat_mon_online);
        InsetsHelper.applySystemBarsPadding(findViewById(R.id.scrollContent));

        catalogRepository = new CatalogCloudRepository(this);
        orderRepository = new OrderCloudRepository(this);
        userRepository = new UserCloudRepository(this);
        sessionManager = new LocalSessionManager(this);

        View tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        TextView btnChooseAddress = findViewById(R.id.btnChooseAddress);
        edtTenMon = findViewById(R.id.edtTenMon);
        TextView btnChonMonTuMenu = findViewById(R.id.btnChonMonTuMenu);
        edtGia = findViewById(R.id.edtGiaMon);
        edtSoLuong = findViewById(R.id.edtSoLuongMon);
        edtSize = findViewById(R.id.edtSizeMon);
        edtGhiChu = findViewById(R.id.edtGhiChuMon);
        edtImageUrl = findViewById(R.id.edtImageUrlMon);
        TextView btnDatMon = findViewById(R.id.btnDatMonOnline);

        btnChooseAddress.setOnClickListener(v -> showAddressChooser());
        btnChonMonTuMenu.setOnClickListener(v -> showPickProductDialog());
        btnDatMon.setOnClickListener(v -> submitOnlineOrder());

        listenProducts();
        loadAddresses();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productsListener != null) {
            productsListener.remove();
        }
    }

    private void listenProducts() {
        if (productsListener != null) {
            productsListener.remove();
        }
        productsListener = catalogRepository.listenProducts(products -> {
            activeProducts.clear();
            for (LocalProduct product : products) {
                if (product.isActive()) {
                    activeProducts.add(product);
                }
            }
        });
    }

    private void loadAddresses() {
        String customerId = sessionManager.getCurrentUserId();
        if (customerId == null) {
            return;
        }
        userRepository.getCustomerAddresses(customerId, (addresses, message) -> runOnUiThread(() -> {
            currentAddresses = new ArrayList<>(addresses);
            selectedAddress = null;
            for (LocalCustomerAddress address : addresses) {
                if (address.isDefault()) {
                    selectedAddress = address;
                    break;
                }
            }
            if (selectedAddress == null && !addresses.isEmpty()) {
                selectedAddress = addresses.get(0);
            }
            bindSelectedAddress();
        }));
    }

    private void bindSelectedAddress() {
        if (selectedAddress == null) {
            tvSelectedAddress.setText("Chưa có địa chỉ. Vui lòng thêm địa chỉ trong hồ sơ khách hàng.");
            return;
        }
        tvSelectedAddress.setText(
                selectedAddress.getLabel()
                        + "\n"
                        + selectedAddress.getRecipientName()
                        + " • "
                        + selectedAddress.getPhone()
                        + "\n"
                        + selectedAddress.buildDisplayAddress()
        );
    }

    private void showAddressChooser() {
        if (currentAddresses.isEmpty()) {
            Toast.makeText(this, "Bạn chưa có địa chỉ nào. Hãy cập nhật hồ sơ khách hàng trước.", Toast.LENGTH_LONG).show();
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
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showPickProductDialog() {
        if (activeProducts.isEmpty()) {
            Toast.makeText(this, "Chưa có món trong menu.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        for (LocalProduct product : activeProducts) {
            labels.add(product.getName() + " - " + product.getBasePrice() + "đ");
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn món")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    LocalProduct product = activeProducts.get(which);
                    selectedProductId = product.getProductId();
                    edtTenMon.setText(product.getName());
                    edtGia.setText(String.valueOf(product.getBasePrice()));
                    edtImageUrl.setText(product.getImageUrl() != null ? product.getImageUrl() : "");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitOnlineOrder() {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAddress == null) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        String tenMon = edtTenMon.getText().toString().trim();
        String giaStr = edtGia.getText().toString().trim();
        String soLuongStr = edtSoLuong.getText().toString().trim();
        String size = edtSize.getText().toString().trim();
        String ghiChu = edtGhiChu.getText().toString().trim();
        String imageUrl = edtImageUrl.getText().toString().trim();

        if (TextUtils.isEmpty(tenMon) || TextUtils.isEmpty(giaStr) || TextUtils.isEmpty(soLuongStr)) {
            Toast.makeText(this, "Vui lòng nhập tên món, giá và số lượng.", Toast.LENGTH_SHORT).show();
            return;
        }

        int gia;
        int soLuong;
        try {
            gia = Integer.parseInt(giaStr);
            soLuong = Integer.parseInt(soLuongStr);
        } catch (Exception e) {
            Toast.makeText(this, "Giá hoặc số lượng không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gia < 0 || soLuong <= 0) {
            Toast.makeText(this, "Giá phải lớn hơn hoặc bằng 0 và số lượng phải lớn hơn 0.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(size) && !size.matches("(?i)[sml]")) {
            Toast.makeText(this, "Size chỉ nhận S, M hoặc L.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(imageUrl) && !Patterns.WEB_URL.matcher(imageUrl).matches()) {
            Toast.makeText(this, "Đường dẫn ảnh không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        orderRepository.createOnlineAppOrder(
                userId,
                selectedProductId,
                tenMon,
                gia,
                soLuong,
                TextUtils.isEmpty(size) ? null : size.toUpperCase(),
                TextUtils.isEmpty(ghiChu) ? null : ghiChu,
                TextUtils.isEmpty(imageUrl) ? null : imageUrl,
                selectedAddress,
                (success, message) -> runOnUiThread(() -> {
                    if (!success) {
                        Toast.makeText(this, message == null ? "Không gửi được đơn lên hệ thống." : message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(this, "Đặt món online thành công.", Toast.LENGTH_SHORT).show();
                    finish();
                })
        );
    }
}
