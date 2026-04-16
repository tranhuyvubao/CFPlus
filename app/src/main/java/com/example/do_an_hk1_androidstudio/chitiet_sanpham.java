package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.local.CustomerCartStore;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;

public class chitiet_sanpham extends AppCompatActivity {

    private TextView tvGia;
    private TextView tvSo;
    private TextView tvOptionPreview;
    private TextView tvCartCount;
    private CheckBox checkSizeS;
    private CheckBox checkSizeM;
    private CheckBox checkSizeL;
    private CheckBox checkDaNhieu;
    private CheckBox checkItDa;
    private CheckBox checkDaRieng;
    private CheckBox checkKhongDa;

    private int soLuong = 1;
    private int giaSanPham = 0;
    private String hinhAnh;
    private String productId;
    private String tenSanPham;
    private CustomerCartStore cartStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chitiet_sanpham);
        InsetsHelper.applyActivityRootPadding(this);

        cartStore = new CustomerCartStore(this);

        ImageView imageButton1 = findViewById(R.id.imageButton1);
        TextView tvTen = findViewById(R.id.tvTen);
        tvGia = findViewById(R.id.tvGia);
        tvSo = findViewById(R.id.tvso);
        tvOptionPreview = findViewById(R.id.tvOptionPreview);
        tvCartCount = findViewById(R.id.tvCartCount);
        TextView tvThemGioHang = findViewById(R.id.tvthemgiohang);
        TextView tvQuayLai = findViewById(R.id.tvquaylai);
        TextView btnBackTop = findViewById(R.id.btnBackTop);
        TextView btnOpenCart = findViewById(R.id.btnOpenCart);
        ImageButton iBTang = findViewById(R.id.iB_tang);
        ImageButton iBGiam = findViewById(R.id.iB_giam);

        checkSizeS = findViewById(R.id.checkSizeS);
        checkSizeM = findViewById(R.id.checkSizeM);
        checkSizeL = findViewById(R.id.checkSizeL);
        checkDaNhieu = findViewById(R.id.check_Da_nhieu);
        checkItDa = findViewById(R.id.check_it_da);
        checkDaRieng = findViewById(R.id.check_da_rieng);
        checkKhongDa = findViewById(R.id.check_khong_da);

        Intent intent = getIntent();
        tenSanPham = intent.getStringExtra("Ten");
        String gia = intent.getStringExtra("Gia");
        hinhAnh = intent.getStringExtra("hinhAnh");
        productId = intent.getStringExtra("productId");

        tvTen.setText(tenSanPham);
        tvSo.setText(String.valueOf(soLuong));
        try {
            giaSanPham = Integer.parseInt(gia == null ? "0" : gia.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            giaSanPham = 0;
        }
        capNhatSoLuong();

        Glide.with(this)
                .load(hinhAnh)
                .placeholder(R.drawable.cfplus)
                .error(R.drawable.cfplus)
                .fitCenter()
                .into(imageButton1);

        iBTang.setOnClickListener(v -> {
            soLuong++;
            capNhatSoLuong();
        });
        iBGiam.setOnClickListener(v -> {
            if (soLuong > 1) {
                soLuong--;
                capNhatSoLuong();
            }
        });

        checkSizeS.setOnCheckedChangeListener((b, c) -> {
            if (c) {
                checkSizeM.setChecked(false);
                checkSizeL.setChecked(false);
            }
            updateOptionPreview();
        });
        checkSizeM.setOnCheckedChangeListener((b, c) -> {
            if (c) {
                checkSizeS.setChecked(false);
                checkSizeL.setChecked(false);
            }
            updateOptionPreview();
        });
        checkSizeL.setOnCheckedChangeListener((b, c) -> {
            if (c) {
                checkSizeS.setChecked(false);
                checkSizeM.setChecked(false);
            }
            updateOptionPreview();
        });

        checkDaNhieu.setOnCheckedChangeListener((b, c) -> {
            if (c) uncheckAllIceExcept(checkDaNhieu);
            updateOptionPreview();
        });
        checkItDa.setOnCheckedChangeListener((b, c) -> {
            if (c) uncheckAllIceExcept(checkItDa);
            updateOptionPreview();
        });
        checkDaRieng.setOnCheckedChangeListener((b, c) -> {
            if (c) uncheckAllIceExcept(checkDaRieng);
            updateOptionPreview();
        });
        checkKhongDa.setOnCheckedChangeListener((b, c) -> {
            if (c) uncheckAllIceExcept(checkKhongDa);
            updateOptionPreview();
        });

        tvThemGioHang.setOnClickListener(v -> addToCart());
        tvQuayLai.setOnClickListener(v -> finish());
        btnBackTop.setOnClickListener(v -> finish());
        btnOpenCart.setOnClickListener(v -> openCart());
        updateOptionPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindCartCount();
    }

    private void addToCart() {
        if (TextUtils.isEmpty(productId)) {
            Toast.makeText(this, "Không tìm thấy sản phẩm để thêm vào giỏ.", Toast.LENGTH_SHORT).show();
            return;
        }
        String size = getSelectedSize();
        String da = getSelectedIce();
        if (size == null || da == null) {
            Toast.makeText(this, "Vui lòng chọn size và mức đá.", Toast.LENGTH_SHORT).show();
            return;
        }
        cartStore.addItem(productId, tenSanPham, giaSanPham, soLuong, size, da, null, hinhAnh);
        bindCartCount();
        Toast.makeText(this, "Đã thêm vào giỏ hàng online.", Toast.LENGTH_SHORT).show();
    }

    private void openCart() {
        startActivity(new Intent(this, DatMonOnlineActivity.class));
    }

    private void bindCartCount() {
        int count = cartStore.getItemCount();
        tvCartCount.setVisibility(count > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        tvCartCount.setText(String.valueOf(count));
    }

    private void capNhatSoLuong() {
        tvSo.setText(String.valueOf(soLuong));
        int tongTien = giaSanPham * soLuong;
        tvGia.setText("Tổng tiền: " + MoneyFormatter.format(tongTien));
        updateOptionPreview();
    }

    private void updateOptionPreview() {
        if (tvOptionPreview == null) {
            return;
        }
        String size = getSelectedSize();
        String da = getSelectedIce();
        String preview = "Lựa chọn: "
                + (size == null ? "chưa chọn size" : "size " + size)
                + " | "
                + (da == null ? "chưa chọn mức đá" : da.toLowerCase())
                + " | SL " + soLuong;
        tvOptionPreview.setText(preview);
    }

    private String getSelectedSize() {
        if (checkSizeS.isChecked()) return "S";
        if (checkSizeM.isChecked()) return "M";
        if (checkSizeL.isChecked()) return "L";
        return null;
    }

    private String getSelectedIce() {
        if (checkDaNhieu.isChecked()) return "Đá bình thường";
        if (checkItDa.isChecked()) return "Ít đá";
        if (checkDaRieng.isChecked()) return "Đá riêng";
        if (checkKhongDa.isChecked()) return "Không đá";
        return null;
    }

    private void uncheckAllIceExcept(CheckBox keep) {
        for (CheckBox cb : new CheckBox[]{checkDaNhieu, checkItDa, checkDaRieng, checkKhongDa}) {
            if (cb != keep) {
                cb.setChecked(false);
            }
        }
    }
}
