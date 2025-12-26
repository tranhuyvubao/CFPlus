package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class chitiet_sanpham extends AppCompatActivity {

    private ImageView imageButton1;
    private TextView tvTen, tvGia, tvso, tvThemgiohang, tvQuayLai;
    private ImageButton iB_tang, iB_giam;
    private CheckBox checkSizeS, checkSizeM, checkSizeL;
    private CheckBox check_Da_nhieu, check_it_da, check_da_rieng, check_khong_da;

    private int soLuong = 1;
    private int giaSanPham = 0;
    // Biến lưu URL hình để truyền tiếp
    private String hinhAnh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chitiet_sanpham);

        // Ánh xạ view
        imageButton1    = findViewById(R.id.imageButton1);
        tvTen           = findViewById(R.id.tvTen);
        tvGia           = findViewById(R.id.tvGia);
        tvso            = findViewById(R.id.tvso);
        tvThemgiohang   = findViewById(R.id.tvthemgiohang);
        tvQuayLai       = findViewById(R.id.tvquaylai);
        iB_tang         = findViewById(R.id.iB_tang);
        iB_giam         = findViewById(R.id.iB_giam);

        checkSizeS      = findViewById(R.id.checkSizeS);
        checkSizeM      = findViewById(R.id.checkSizeM);
        checkSizeL      = findViewById(R.id.checkSizeL);

        check_Da_nhieu  = findViewById(R.id.check_Da_nhieu);
        check_it_da     = findViewById(R.id.check_it_da);
        check_da_rieng  = findViewById(R.id.check_da_rieng);
        check_khong_da  = findViewById(R.id.check_khong_da);

        // Nhận dữ liệu từ Intent
        Intent intent = getIntent();
        String ten      = intent.getStringExtra("Ten");
        String gia      = intent.getStringExtra("Gia");
        hinhAnh         = intent.getStringExtra("hinhAnh");

        // Thiết lập ban đầu
        tvTen.setText(ten);
        tvso.setText(String.valueOf(soLuong));
        try {
            giaSanPham = Integer.parseInt(gia.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            giaSanPham = 0;
        }
        capNhatSoLuong();

        // Load ảnh
        Glide.with(this)
                .load(hinhAnh)
                .centerCrop()
                .into(imageButton1);

        // Xử lý tăng/giảm số lượng
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

        // Chọn size (chỉ 1)
        checkSizeS.setOnCheckedChangeListener((b, c) -> {
            if (c) { checkSizeM.setChecked(false); checkSizeL.setChecked(false); }
        });
        checkSizeM.setOnCheckedChangeListener((b, c) -> {
            if (c) { checkSizeS.setChecked(false); checkSizeL.setChecked(false); }
        });
        checkSizeL.setOnCheckedChangeListener((b, c) -> {
            if (c) { checkSizeS.setChecked(false); checkSizeM.setChecked(false); }
        });

        // Chọn đá (chỉ 1)
        check_Da_nhieu.setOnCheckedChangeListener((b, c) -> { if (c) uncheckAllIceExcept(check_Da_nhieu); });
        check_it_da.setOnCheckedChangeListener((b, c)    -> { if (c) uncheckAllIceExcept(check_it_da); });
        check_da_rieng.setOnCheckedChangeListener((b, c) -> { if (c) uncheckAllIceExcept(check_da_rieng); });
        check_khong_da.setOnCheckedChangeListener((b, c) -> { if (c) uncheckAllIceExcept(check_khong_da); });

        // Thêm vào giỏ hàng -> mở dialog thông tin đặt bàn
        tvThemgiohang.setOnClickListener(v -> {
            String size = getSelectedSize();
            String da   = getSelectedIce();
            if (size == null || da == null) {
                Toast.makeText(this, "Vui lòng chọn size và mức đá", Toast.LENGTH_SHORT).show();
                return;
            }
            // Truyền cả hinhAnh vào dialog
            showThongTinDatBanDialog(
                    ten, size, da, soLuong,
                    giaSanPham, giaSanPham * soLuong,
                    hinhAnh
            );
        });

        // Xử lý nút trở về trang chủ
        tvQuayLai.setOnClickListener(v -> {
            Intent backIntent = new Intent(chitiet_sanpham.this, trangchu.class);
            backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(backIntent);
            finish(); // Đóng activity hiện tại
        });
    }

    private void capNhatSoLuong() {
        tvso.setText(String.valueOf(soLuong));
        int tongTien = giaSanPham * soLuong;
        tvGia.setText("Tổng tiền: " + tongTien + "đ");
    }

    private String getSelectedSize() {
        if (checkSizeS.isChecked()) return "S";
        if (checkSizeM.isChecked()) return "M";
        if (checkSizeL.isChecked()) return "L";
        return null;
    }

    private String getSelectedIce() {
        if (check_Da_nhieu.isChecked()) return "Đá nhiều";
        if (check_it_da.isChecked())    return "Ít đá";
        if (check_da_rieng.isChecked()) return "Đá riêng";
        if (check_khong_da.isChecked()) return "Không đá";
        return null;
    }

    private void uncheckAllIceExcept(CheckBox keep) {
        for (CheckBox cb : new CheckBox[]{check_Da_nhieu, check_it_da, check_da_rieng, check_khong_da}) {
            if (cb != keep) cb.setChecked(false);
        }
    }

    // ĐÃ SỬA: thêm tham số hinhAnh
    private void showThongTinDatBanDialog(String tenSanPham,
                                          String size,
                                          String mucDa,
                                          int soLuong,
                                          int gia,
                                          int tongTien,
                                          String hinhAnh) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.thongtin_datban, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText editBan   = dialogView.findViewById(R.id.editban);
        EditText editSdt   = dialogView.findViewById(R.id.editSdt);
        EditText editTenNv = dialogView.findViewById(R.id.edit_tennv);
        RadioGroup rgHT    = dialogView.findViewById(R.id.rgHinhThuc);
        TextView btnThem   = dialogView.findViewById(R.id.tvthemgiohang);

        // Tự động load tên nhân viên từ Firestore
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userUid = user.getUid();
            FirebaseFirestore.getInstance()
                    .collection("Người dùng")
                    .document(userUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String hoTen = documentSnapshot.getString("Họ tên NV");
                            if (hoTen != null && !hoTen.isEmpty()) {
                                editTenNv.setText(hoTen);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Nếu có lỗi, để trống để user tự nhập
                    });
        }

        btnThem.setOnClickListener(v -> {
            String tenBan = editBan.getText().toString().trim();
            String sdt    = editSdt.getText().toString().trim();
            String tenNv  = editTenNv.getText().toString().trim();
            int   selId   = rgHT.getCheckedRadioButtonId();
            String hinhThuc = selId == R.id.mang_ve ? "Mang về" : "Tại chỗ";

            if (tenBan.isEmpty() || sdt.isEmpty() || tenNv.isEmpty() || selId == -1) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("Tên bàn", tenBan);
            data.put("Số điện thoại", sdt);
            data.put("Tên nhân viên", tenNv);
            data.put("hình thức", hinhThuc);
            data.put("Tên sản phẩm", tenSanPham);
            data.put("Số lượng", soLuong);
            data.put("Size", size);
            data.put("Mức đá", mucDa);
            data.put("Giá", gia);
            data.put("Tổng tiền", tongTien);
            data.put("hinhAnh", hinhAnh);
            data.put("trangthaithanhtoan", "Chưa thanh toán");
            
            // Thêm UID của user hiện tại để phân biệt đơn hàng của từng user
            if (user != null) {
                data.put("userId", user.getUid());
            }

            FirebaseFirestore.getInstance()
                    .collection("Đơn hàng")
                    .document("Giỏ hàng")
                    .collection("Sản phẩm")
                    .add(data)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Thêm vào giỏ thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi lưu dữ liệu!", Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
