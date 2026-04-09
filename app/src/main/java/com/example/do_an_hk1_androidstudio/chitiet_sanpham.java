package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

public class chitiet_sanpham extends AppCompatActivity {

    private TextView tvGia;
    private TextView tvso;
    private CheckBox checkSizeS;
    private CheckBox checkSizeM;
    private CheckBox checkSizeL;
    private CheckBox check_Da_nhieu;
    private CheckBox check_it_da;
    private CheckBox check_da_rieng;
    private CheckBox check_khong_da;
    private int soLuong = 1;
    private int giaSanPham = 0;
    private String hinhAnh;
    private String productId;
    private TextView tvOptionPreview;
    private OrderCloudRepository orderRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chitiet_sanpham);
        InsetsHelper.applyActivityRootPadding(this);

        orderRepository = new OrderCloudRepository(this);

        ImageView imageButton1 = findViewById(R.id.imageButton1);
        TextView tvTen = findViewById(R.id.tvTen);
        tvGia = findViewById(R.id.tvGia);
        tvso = findViewById(R.id.tvso);
        TextView tvThemgiohang = findViewById(R.id.tvthemgiohang);
        TextView tvQuayLai = findViewById(R.id.tvquaylai);
        tvOptionPreview = findViewById(R.id.tvOptionPreview);
        ImageButton iB_tang = findViewById(R.id.iB_tang);
        ImageButton iB_giam = findViewById(R.id.iB_giam);

        checkSizeS = findViewById(R.id.checkSizeS);
        checkSizeM = findViewById(R.id.checkSizeM);
        checkSizeL = findViewById(R.id.checkSizeL);
        check_Da_nhieu = findViewById(R.id.check_Da_nhieu);
        check_it_da = findViewById(R.id.check_it_da);
        check_da_rieng = findViewById(R.id.check_da_rieng);
        check_khong_da = findViewById(R.id.check_khong_da);

        Intent intent = getIntent();
        String ten = intent.getStringExtra("Ten");
        String gia = intent.getStringExtra("Gia");
        hinhAnh = intent.getStringExtra("hinhAnh");
        productId = intent.getStringExtra("productId");

        tvTen.setText(ten);
        tvso.setText(String.valueOf(soLuong));
        try {
            giaSanPham = Integer.parseInt(gia.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            giaSanPham = 0;
        }
        capNhatSoLuong();

        Glide.with(this)
                .load(hinhAnh)
                .placeholder(R.mipmap.ic_launcher_round)
                .centerCrop()
                .into(imageButton1);

        iB_tang.setOnClickListener(v -> {
            soLuong++;
            capNhatSoLuong();
        });
        iB_giam.setOnClickListener(v -> {
            if (soLuong > 1) {
                soLuong--;
                capNhatSoLuong();
            }
        });

        checkSizeS.setOnCheckedChangeListener((b, c) -> { if (c) { checkSizeM.setChecked(false); checkSizeL.setChecked(false); } updateOptionPreview(); });
        checkSizeM.setOnCheckedChangeListener((b, c) -> { if (c) { checkSizeS.setChecked(false); checkSizeL.setChecked(false); } updateOptionPreview(); });
        checkSizeL.setOnCheckedChangeListener((b, c) -> { if (c) { checkSizeS.setChecked(false); checkSizeM.setChecked(false); } updateOptionPreview(); });

        check_Da_nhieu.setOnCheckedChangeListener((b, c) -> { if (c) uncheckAllIceExcept(check_Da_nhieu); updateOptionPreview(); });
        check_it_da.setOnCheckedChangeListener((b, c) -> { if (c) uncheckAllIceExcept(check_it_da); updateOptionPreview(); });
        check_da_rieng.setOnCheckedChangeListener((b, c) -> { if (c) uncheckAllIceExcept(check_da_rieng); updateOptionPreview(); });
        check_khong_da.setOnCheckedChangeListener((b, c) -> { if (c) uncheckAllIceExcept(check_khong_da); updateOptionPreview(); });

        tvThemgiohang.setOnClickListener(v -> {
            String size = getSelectedSize();
            String da = getSelectedIce();
            if (size == null || da == null) {
                Toast.makeText(this, "Vui lòng chọn size và mức đá", Toast.LENGTH_SHORT).show();
                return;
            }
            showThongTinDatBanDialog(ten, size, da, soLuong, giaSanPham, hinhAnh);
        });

        tvQuayLai.setOnClickListener(v -> {
            Intent backIntent = new Intent(chitiet_sanpham.this, trangchu.class);
            backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(backIntent);
            finish();
        });
        updateOptionPreview();
    }

    private void capNhatSoLuong() {
        tvso.setText(String.valueOf(soLuong));
        int tongTien = giaSanPham * soLuong;
        tvGia.setText("Tổng tiền: " + tongTien + "đ");
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
        if (check_Da_nhieu.isChecked()) return "Đá nhiều";
        if (check_it_da.isChecked()) return "Ít đá";
        if (check_da_rieng.isChecked()) return "Đá riêng";
        if (check_khong_da.isChecked()) return "Không đá";
        return null;
    }

    private void uncheckAllIceExcept(CheckBox keep) {
        for (CheckBox cb : new CheckBox[]{check_Da_nhieu, check_it_da, check_da_rieng, check_khong_da}) {
            if (cb != keep) cb.setChecked(false);
        }
    }

    private void showThongTinDatBanDialog(String tenSanPham,
                                          String size,
                                          String mucDa,
                                          int soLuong,
                                          int gia,
                                          String hinhAnh) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.thongtin_datban, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText editBan = dialogView.findViewById(R.id.editban);
        EditText editSdt = dialogView.findViewById(R.id.editSdt);
        EditText editTenNv = dialogView.findViewById(R.id.edit_tennv);
        RadioGroup rgHT = dialogView.findViewById(R.id.rgHinhThuc);
        TextView btnThem = dialogView.findViewById(R.id.tvthemgiohang);

        LocalSessionManager sessionManager = new LocalSessionManager(this);
        String currentUserId = sessionManager.getCurrentUserId();
        String currentFullName = sessionManager.getCurrentUserFullName();
        if (!TextUtils.isEmpty(currentFullName)) {
            editTenNv.setText(currentFullName);
        }

        btnThem.setOnClickListener(v -> {
            String tenBan = editBan.getText().toString().trim();
            String sdt = editSdt.getText().toString().trim();
            String tenNv = editTenNv.getText().toString().trim();
            int selId = rgHT.getCheckedRadioButtonId();
            String hinhThuc = selId == R.id.mang_ve ? "Mang ve" : "Tai cho";

            if (tenBan.isEmpty() || sdt.isEmpty() || tenNv.isEmpty() || selId == -1) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sdt.length() < 9) {
                Toast.makeText(this, "Số điện thoại chưa hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(productId)) {
                Toast.makeText(this, "Không tìm thấy sản phẩm để đặt", Toast.LENGTH_SHORT).show();
                return;
            }

            String orderType = "Mang ve".equals(hinhThuc) ? "takeaway" : "dine_in";
            String note = mucDa + (TextUtils.isEmpty(tenNv) ? "" : " | NV: " + tenNv);
            orderRepository.createCustomerOrder(
                    orderType,
                    "staff_pos",
                    currentUserId == null ? "guest_staff" : currentUserId,
                    "Mang ve".equals(hinhThuc) ? null : tenBan,
                    tenBan,
                    productId,
                    tenSanPham,
                    gia,
                    soLuong,
                    size,
                    note,
                    hinhAnh,
                    null,
                    (success, message) -> runOnUiThread(() -> {
                        if (!success) {
                            Toast.makeText(this, message == null ? "Không thêm được món." : message, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(this, "Đã thêm món thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        finish();
                    })
            );
        });
    }
}
