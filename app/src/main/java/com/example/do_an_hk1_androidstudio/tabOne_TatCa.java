package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class tabOne_TatCa extends Fragment {

    LinearLayout linearCafe, linearTraSua, linearMatcha, linearTopping;
    FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_one__tatca, container, false);

        linearCafe = view.findViewById(R.id.linearCafe);
        linearTraSua = view.findViewById(R.id.linearTraSua);
        linearMatcha = view.findViewById(R.id.linearMatcha);
        linearTopping = view.findViewById(R.id.linearTopping);

        db = FirebaseFirestore.getInstance();

        loadSanPham("CaFe", "Cafe", linearCafe, inflater);
        loadSanPham("Trà sữa", "trasua", linearTraSua, inflater);
        loadSanPham("Matcha", "matcha", linearMatcha, inflater);
        loadSanPham("Topping", "topping", linearTopping, inflater);

        return view;
    }

    private void loadSanPham(String docName, String collectionName, LinearLayout layout, LayoutInflater inflater) {
        db.collection("SanPham")
                .document(docName)
                .collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        String ten = snapshot.getString("Ten");
                        String gia = snapshot.getString("Gia");
                        String hinh = snapshot.getString("hinhAnh");

                        View itemView = inflater.inflate(R.layout.listview_layout, null);

                        ImageButton img = itemView.findViewById(R.id.imageButton1);
                        TextView tvTen = itemView.findViewById(R.id.tvTen);
                        TextView tvGia = itemView.findViewById(R.id.tvGia);

                        tvTen.setText(ten);
                        tvGia.setText(gia + "đ");
                        Glide.with(getContext()).load(hinh).into(img);

                        // 👉 Bắt sự kiện click để mở chi tiết
                        // Truyền thông tin sản phẩm qua Intent
                        itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(getActivity(), chitiet_sanpham.class);
                            intent.putExtra("Ten", ten);  // Chỉnh sửa thành "ten"
                            intent.putExtra("Gia", gia);  // Chỉnh sửa thành "gia"
                            intent.putExtra("hinhAnh", hinh);  // Đảm bảo tên tham số khớp
                            startActivity(intent);
                        });


                        layout.addView(itemView);
                    }
                })
                .addOnFailureListener(e -> {
                    // Log lỗi nếu cần
                });
    }
}
