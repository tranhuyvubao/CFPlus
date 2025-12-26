package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Fragment_Account extends Fragment {

    private TextView tvHoTen, tvNgaySinh, tvGioiTinh, tvEmail, tvSoDienThoai;

    private Button btnSuaTT, btnTroVe;
    private FirebaseFirestore db;

    public Fragment_Account() {
        // Required empty public constructor
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment__account, container, false);

        // Ánh xạ các view
        tvHoTen = view.findViewById(R.id.tvHoTen);
        tvNgaySinh = view.findViewById(R.id.tvNgaySinh);
        tvGioiTinh = view.findViewById(R.id.tvGioiTinh);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvSoDienThoai = view.findViewById(R.id.tvSoDienThoai);
        btnSuaTT = view.findViewById(R.id.btn_suaTT);
        btnTroVe = view.findViewById(R.id.btnTroVe);

        db = FirebaseFirestore.getInstance();

        // Lấy dữ liệu từ Firestore
        loadNhanVienData();

        // Xử lý sự kiện khi người dùng nhấn nút Sửa thông tin
        btnSuaTT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển hướng đến Fragment_SuaThongTinCaNhan
                Intent intent = new Intent(getActivity(), SuaThongTinCaNhanActivity.class);
                startActivity(intent);
            }
        });
        
        // Xử lý sự kiện khi người dùng nhấn nút Trở về
        btnTroVe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quay lại Fragment_Setting
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });
        
        return view;
    }

    private void loadNhanVienData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            // Nếu chưa đăng nhập, hiển thị email từ Firebase Auth
            tvEmail.setText("Email: Chưa đăng nhập");
            return;
        }
        
        // Lấy dữ liệu theo UID của user hiện tại
        String userUid = currentUser.getUid();
        DocumentReference docRef = db.collection("Người dùng")
                .document(userUid); // Document riêng cho mỗi user

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String hoTen = documentSnapshot.getString("Họ tên NV");
                String gioiTinh = documentSnapshot.getString("Giới tính");
                String email = documentSnapshot.getString("Email");
                String sdt = documentSnapshot.getString("Số điện thoại");

                Timestamp timestamp = documentSnapshot.getTimestamp("Ngày sinh");
                String ngaySinh = "";

                if (timestamp != null) {
                    Date date = timestamp.toDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    ngaySinh = sdf.format(date);
                }
                
                // Hiển thị dữ liệu nếu có
                if (hoTen != null && !hoTen.isEmpty()) {
                    tvHoTen.setText("Họ tên: " + hoTen);
                } else {
                    tvHoTen.setText("Họ tên: Chưa cập nhật");
                }
                
                if (ngaySinh != null && !ngaySinh.isEmpty()) {
                    tvNgaySinh.setText("Ngày sinh: " + ngaySinh);
                } else {
                    tvNgaySinh.setText("Ngày sinh: Chưa cập nhật");
                }
                
                if (gioiTinh != null && !gioiTinh.isEmpty()) {
                    tvGioiTinh.setText("Giới tính: " + gioiTinh);
                } else {
                    tvGioiTinh.setText("Giới tính: Chưa cập nhật");
                }
                
                // Email luôn lấy từ Firebase Auth (chính xác nhất)
                if (currentUser.getEmail() != null) {
                    tvEmail.setText("Email: " + currentUser.getEmail());
                } else if (email != null && !email.isEmpty()) {
                    tvEmail.setText("Email: " + email);
                } else {
                    tvEmail.setText("Email: Chưa cập nhật");
                }
                
                if (sdt != null && !sdt.isEmpty()) {
                    tvSoDienThoai.setText("Số điện thoại: " + sdt);
                } else {
                    tvSoDienThoai.setText("Số điện thoại: Chưa cập nhật");
                }
            } else {
                // Nếu chưa có dữ liệu, hiển thị email từ Firebase Auth và thông báo
                tvEmail.setText("Email: " + currentUser.getEmail());
                tvHoTen.setText("Họ tên: Chưa cập nhật");
                tvNgaySinh.setText("Ngày sinh: Chưa cập nhật");
                tvGioiTinh.setText("Giới tính: Chưa cập nhật");
                tvSoDienThoai.setText("Số điện thoại: Chưa cập nhật");
            }
        }).addOnFailureListener(e -> {
            // Log hoặc xử lý khi lỗi xảy ra
            e.printStackTrace();
            // Vẫn hiển thị email từ Firebase Auth nếu có lỗi
            if (currentUser != null && currentUser.getEmail() != null) {
                tvEmail.setText("Email: " + currentUser.getEmail());
            }
        });
    }
}
