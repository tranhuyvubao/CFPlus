package com.example.do_an_hk1_androidstudio;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FragmentHistory extends Fragment {

    private RecyclerView rvHistory;
    private DonHangAdapter adapter;
    private List<DonHang> listDonHang;
    private Button btnThemHang;

    private OnThemHangClickListener callback;

    // Interface để gọi về Activity
    public interface OnThemHangClickListener {
        void onThemHangClick();
    }

    // Gắn interface khi Fragment được attach vào Activity
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnThemHangClickListener) {
            callback = (OnThemHangClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " phải implement OnThemHangClickListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        btnThemHang = view.findViewById(R.id.button_themhang);

        listDonHang = new ArrayList<>();
        adapter = new DonHangAdapter(getContext(), listDonHang);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(adapter);

        // Gọi dữ liệu từ Firestore - chỉ lấy đơn hàng của user hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FragmentHistory", "User chưa đăng nhập");
            return view;
        }
        
        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance()
                .collection("Đơn hàng")
                .document("Giỏ hàng")
                .collection("Sản phẩm")
                .whereEqualTo("userId", userId) // Chỉ lấy đơn hàng của user hiện tại
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listDonHang.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String tenBan = doc.getString("Tên bàn");
                        String tenSanPham = doc.getString("Tên sản phẩm");
                        String size = doc.contains("Size") ? doc.getString("Size") : "Không xác định";
                        String da = doc.getString("Mức đá");
                        Long soLuongLong = doc.getLong("Số lượng");
                        int soLuong = soLuongLong != null ? soLuongLong.intValue() : 0;
                        Long tongTienLong = doc.getLong("Tổng tiền");
                        int tongTien = tongTienLong != null ? tongTienLong.intValue() : 0;
                        String hinhThuc = doc.getString("hình thức");
                        String trangThai = doc.getString("trangthaithanhtoan");
                        String hinhAnh = doc.getString("hinhAnh");
                        String docId = doc.getId(); // Lưu document ID để dùng khi sửa/xóa

                        DonHang donHang = new DonHang(tenBan, tenSanPham, size, da, soLuong, tongTien, hinhThuc, trangThai, hinhAnh);
                        donHang.setDocId(docId); // Lưu document ID
                        listDonHang.add(donHang);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("FragmentHistory", "Lỗi tải dữ liệu", e));

        // Gọi về Activity khi click nút
        btnThemHang.setOnClickListener(v -> {
            if (callback != null) {
                callback.onThemHangClick();
            }
        });

        return view;
    }
}
