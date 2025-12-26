package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class SuaThongTinCaNhanActivity extends AppCompatActivity {

    EditText edtHoTen, edtNgaySinh, edtGioiTinh, edtEmail, edtSoDienThoai;
    TextView tvQuayLai;
    Button btnLuu;
    FirebaseFirestore db;
    FirebaseUser user;
    DocumentReference nhanVienRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sua_thongtincanhan);

        // Ánh xạ view
        edtHoTen = findViewById(R.id.edtHoTen);
        edtNgaySinh = findViewById(R.id.edtNgaySinh);
        edtGioiTinh = findViewById(R.id.edtGioiTinh);
        edtEmail = findViewById(R.id.edtEmail);
        edtSoDienThoai = findViewById(R.id.edtSoDienThoai);
        btnLuu = findViewById(R.id.btn_luu);
        tvQuayLai = findViewById(R.id.tvquaylaiTTnv);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            edtEmail.setText(user.getEmail()); // Hiển thị email
            // Lưu dữ liệu theo UID của user hiện tại
            String userUid = user.getUid();
            nhanVienRef = db.collection("Người dùng").document(userUid);

            // Load dữ liệu từ Firestore
            loadDataFromFirestore();

            // Lưu thay đổi
            btnLuu.setOnClickListener(v -> saveDataToFirestore());
        }
        //Quay lại fragment accout
        tvQuayLai.setOnClickListener(v -> finish());
    }

    private void loadDataFromFirestore() {
        nhanVienRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                edtHoTen.setText(doc.getString("Họ tên NV"));
                edtGioiTinh.setText(doc.getString("Giới tính"));
                edtSoDienThoai.setText(doc.getString("Số điện thoại"));

                Object ngaySinhObj = doc.get("Ngày sinh");
                if (ngaySinhObj instanceof Timestamp) {
                    Timestamp ts = (Timestamp) ngaySinhObj;
                    String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate());
                    edtNgaySinh.setText(formatted);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Không tải được dữ liệu", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveDataToFirestore() {
        String hoTen = edtHoTen.getText().toString();
        String ngaySinh = edtNgaySinh.getText().toString();
        String gioiTinh = edtGioiTinh.getText().toString();
        String email = edtEmail.getText().toString();
        String sdt = edtSoDienThoai.getText().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("Họ tên NV", hoTen);
        data.put("Giới tính", gioiTinh);
        data.put("Email", email);
        data.put("Số điện thoại", sdt);

        try {
            Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(ngaySinh);
            data.put("Ngày sinh", date);
        } catch (Exception e) {
            Toast.makeText(this, "Định dạng ngày không đúng!", Toast.LENGTH_SHORT).show();
            return;
        }

        nhanVienRef.set(data).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
            finish(); // Quay lại Fragment trước
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi khi cập nhật!", Toast.LENGTH_SHORT).show();
        });
    }
}
