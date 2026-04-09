package com.example.do_an_hk1_androidstudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalProduct;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

public class DatMonTaiBanActivity extends AppCompatActivity {

    private static final String TABLE_QR_PREFIX = "CFPLUS_TABLE:";

    private EditText edtTenMon;
    private EditText edtTableCode;
    private EditText edtGia;
    private EditText edtSoLuong;
    private EditText edtSize;
    private EditText edtGhiChu;
    private EditText edtImageUrl;
    private String selectedProductId;
    private final List<LocalProduct> activeProducts = new ArrayList<>();
    private CatalogCloudRepository catalogRepository;
    private OrderCloudRepository orderRepository;
    private TableCloudRepository tableRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration productsListener;
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    startQrScanner();
                } else {
                    Toast.makeText(this, "Báº¡n cáº§n cáº¥p quyá»n camera Ä‘á»ƒ quÃ©t QR bÃ n", Toast.LENGTH_SHORT).show();
                }
            });
    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(new ScanContract(), this::handleScanResult);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dat_mon_tai_ban);
        InsetsHelper.applySystemBarsPadding(findViewById(R.id.scrollContent));

        catalogRepository = new CatalogCloudRepository(this);
        orderRepository = new OrderCloudRepository(this);
        tableRepository = new TableCloudRepository(this);
        sessionManager = new LocalSessionManager(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        Toast.makeText(this, "Goi mon tai ban chi dung qua web/QR. Vui long quet ma tren ban.", Toast.LENGTH_LONG).show();
        finish();

        edtTableCode = findViewById(R.id.edtTableCode);
        edtTenMon = findViewById(R.id.edtTenMon);
        TextView btnChonMonTuMenu = findViewById(R.id.btnChonMonTuMenu);
        edtGia = findViewById(R.id.edtGiaMon);
        edtSoLuong = findViewById(R.id.edtSoLuongMon);
        edtSize = findViewById(R.id.edtSizeMon);
        edtGhiChu = findViewById(R.id.edtGhiChuMon);
        edtImageUrl = findViewById(R.id.edtImageUrlMon);
        TextView btnScanTableQr = findViewById(R.id.btnScanTableQr);
        TextView btnDatMon = findViewById(R.id.btnDatMonOnline);

        btnChonMonTuMenu.setOnClickListener(v -> showPickProductDialog());
        btnScanTableQr.setOnClickListener(v -> ensureCameraPermissionAndScan());
        btnDatMon.setOnClickListener(v -> submitTableOrder());

        listenProducts();
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

    private void ensureCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startQrScanner();
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("ÄÆ°a camera vÃ o mÃ£ QR trÃªn bÃ n");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.addExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, true);
        qrScanLauncher.launch(options);
    }

    private void handleScanResult(ScanIntentResult result) {
        if (result == null || result.getContents() == null) {
            return;
        }
        String tableCode = parseTableCode(result.getContents());
        if (tableCode == null) {
            Toast.makeText(this, "QR nÃ y khÃ´ng pháº£i mÃ£ bÃ n cá»§a quÃ¡n", Toast.LENGTH_SHORT).show();
            return;
        }
        edtTableCode.setText(tableCode);
        Toast.makeText(this, "ÄÃ£ nháº­n mÃ£ bÃ n: " + tableCode, Toast.LENGTH_SHORT).show();
    }

    private String parseTableCode(String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim();
        if (!normalized.startsWith(TABLE_QR_PREFIX)) {
            try {
                android.net.Uri uri = android.net.Uri.parse(normalized);
                String tableValue = uri.getQueryParameter("table");
                if (!TextUtils.isEmpty(tableValue)) {
                    return tableValue.trim().toUpperCase();
                }
            } catch (Exception ignored) {
            }
            return null;
        }
        String tableCode = normalized.substring(TABLE_QR_PREFIX.length()).trim();
        return TextUtils.isEmpty(tableCode) ? null : tableCode.toUpperCase();
    }

    private void showPickProductDialog() {
        if (activeProducts.isEmpty()) {
            Toast.makeText(this, "ChÆ°a cÃ³ mÃ³n trong menu", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        for (LocalProduct product : activeProducts) {
            labels.add(product.getName() + " - " + product.getBasePrice() + "Ä‘");
        }

        new AlertDialog.Builder(this)
                .setTitle("Chá»n mÃ³n")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    LocalProduct product = activeProducts.get(which);
                    selectedProductId = product.getProductId();
                    edtTenMon.setText(product.getName());
                    edtGia.setText(String.valueOf(product.getBasePrice()));
                    edtImageUrl.setText(product.getImageUrl() != null ? product.getImageUrl() : "");
                })
                .setNegativeButton("Há»§y", null)
                .show();
    }

    private void submitTableOrder() {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lÃ²ng Ä‘Äƒng nháº­p", Toast.LENGTH_SHORT).show();
            return;
        }

        String tenMon = edtTenMon.getText().toString().trim();
        String tableCode = edtTableCode.getText().toString().trim();
        String giaStr = edtGia.getText().toString().trim();
        String soLuongStr = edtSoLuong.getText().toString().trim();
        String size = edtSize.getText().toString().trim();
        String ghiChu = edtGhiChu.getText().toString().trim();
        String imageUrl = edtImageUrl.getText().toString().trim();

        if (TextUtils.isEmpty(tableCode) || TextUtils.isEmpty(tenMon) || TextUtils.isEmpty(giaStr) || TextUtils.isEmpty(soLuongStr)) {
            Toast.makeText(this, "Vui lÃ²ng nháº­p mÃ£ bÃ n, tÃªn mÃ³n, giÃ¡ vÃ  sá»‘ lÆ°á»£ng", Toast.LENGTH_SHORT).show();
            return;
        }

        int gia;
        int soLuong;
        try {
            gia = Integer.parseInt(giaStr);
            soLuong = Integer.parseInt(soLuongStr);
        } catch (Exception e) {
            Toast.makeText(this, "GiÃ¡ hoáº·c sá»‘ lÆ°á»£ng khÃ´ng há»£p lá»‡", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gia < 0 || soLuong <= 0) {
            Toast.makeText(this, "GiÃ¡ pháº£i lá»›n hÆ¡n hoáº·c báº±ng 0 vÃ  sá»‘ lÆ°á»£ng pháº£i lá»›n hÆ¡n 0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(size) && !size.matches("(?i)[sml]")) {
            Toast.makeText(this, "Size chá»‰ nháº­n S, M hoáº·c L", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(imageUrl) && !Patterns.WEB_URL.matcher(imageUrl).matches()) {
            Toast.makeText(this, "ÄÆ°á»ng dáº«n áº£nh khÃ´ng há»£p lá»‡", Toast.LENGTH_SHORT).show();
            return;
        }

        tableRepository.findTableByCode(tableCode, table -> runOnUiThread(() -> {
            if (table == null || !table.isActive()) {
                Toast.makeText(this, "MÃ£ bÃ n khÃ´ng há»£p lá»‡", Toast.LENGTH_SHORT).show();
                return;
            }
            orderRepository.createTableQrOrder(
                    userId,
                    table.getTableId(),
                    table.getName(),
                    selectedProductId,
                    tenMon,
                    gia,
                    soLuong,
                    TextUtils.isEmpty(size) ? null : size.toUpperCase(),
                    TextUtils.isEmpty(ghiChu) ? null : ghiChu,
                    TextUtils.isEmpty(imageUrl) ? null : imageUrl,
                    (success, message) -> runOnUiThread(() -> {
                        if (!success) {
                            Toast.makeText(this, message == null ? "KhÃ´ng gá»­i Ä‘Æ°á»£c Ä‘Æ¡n lÃªn há»‡ thá»‘ng" : message, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(this, "Gá»­i mÃ³n táº¡i " + table.getName() + " thÃ nh cÃ´ng!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
            );
        }));
    }
}

