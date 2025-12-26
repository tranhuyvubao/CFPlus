package com.example.do_an_hk1_androidstudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class DonHangAdapter extends RecyclerView.Adapter<DonHangAdapter.DonHangViewHolder> {

    private List<DonHang> list;
    private Context context;

    public DonHangAdapter(Context context, List<DonHang> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public DonHangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.listview_history, parent, false);
        return new DonHangViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonHangViewHolder holder, int position) {
        DonHang donHang = list.get(position);

        holder.tenban.setText("Tên bàn: " + donHang.tenBan);
        holder.tvTen.setText("Tên sản phẩm: " + donHang.tenSanPham);
        holder.tvSize.setText("Size: " + donHang.size);
        holder.tvDa.setText("Mức đá: " + donHang.mucDa);
        holder.tvSoLuong.setText("Số lượng: " + donHang.soLuong);
        holder.tvGia.setText("Tổng tiền: " + donHang.tongTien + "đ");
        holder.tvHinhThuc.setText("Hình thức: " + donHang.hinhThuc);
        holder.tvTrangThai.setText("Trạng thái: " + donHang.trangThai);

        // Đổi màu trạng thái: xanh (#00C853) nếu "Đã thanh toán", đỏ nếu "Chưa thanh toán"
        if ("Đã thanh toán".equals(donHang.trangThai)) {
            holder.tvTrangThai.setTextColor(0xFF00C853); // Màu xanh giống checkbox trong dialog
        } else {
            holder.tvTrangThai.setTextColor(0xFFDD2C00); // Màu đỏ như trong layout
        }

        Glide.with(context).load(donHang.hinhAnh).into(holder.imageButton);

        // Xử lý nút Sửa
        holder.btnEdit.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.sua_trangthai_thanhtoan, null);
            CheckBox checkBox = dialogView.findViewById(R.id.checkBox);
            checkBox.setChecked("Đã thanh toán".equals(donHang.getTrangThai()));

            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setTitle("Cập nhật trạng thái thanh toán")
                    .setPositiveButton("Lưu", (dialogInterface, i) -> {
                        boolean daThanhToan = checkBox.isChecked();
                        String newTrangThai = daThanhToan ? "Đã thanh toán" : "Chưa thanh toán";

                        // Cập nhật Firestore - sử dụng document ID trực tiếp
                        if (donHang.getDocId() != null && !donHang.getDocId().isEmpty()) {
                            FirebaseFirestore.getInstance()
                                    .collection("Đơn hàng")
                                    .document("Giỏ hàng")
                                    .collection("Sản phẩm")
                                    .document(donHang.getDocId())
                                    .update("trangthaithanhtoan", newTrangThai)
                                    .addOnSuccessListener(aVoid -> {
                                        // Cập nhật UI
                                        donHang.trangThai = newTrangThai;
                                        notifyItemChanged(holder.getAdapterPosition());
                                    });
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .create();

            dialog.show();
        });

        // Xử lý nút Xóa
        holder.btnDelete.setOnClickListener(v -> {
            // Xóa trực tiếp bằng document ID
            if (donHang.getDocId() != null && !donHang.getDocId().isEmpty()) {
                FirebaseFirestore.getInstance()
                        .collection("Đơn hàng")
                        .document("Giỏ hàng")
                        .collection("Sản phẩm")
                        .document(donHang.getDocId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            list.remove(holder.getAdapterPosition());
                            notifyItemRemoved(holder.getAdapterPosition());
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class DonHangViewHolder extends RecyclerView.ViewHolder {
        TextView tenban, tvTen, tvSize, tvDa, tvSoLuong, tvGia, tvHinhThuc, tvTrangThai;
        ImageButton imageButton;
        TextView btnEdit, btnDelete;

        public DonHangViewHolder(@NonNull View itemView) {
            super(itemView);
            tenban = itemView.findViewById(R.id.tenban);
            tvTen = itemView.findViewById(R.id.tvTen);
            tvSize = itemView.findViewById(R.id.tvSize);
            tvDa = itemView.findViewById(R.id.tvmucda);
            tvSoLuong = itemView.findViewById(R.id.tvslsp);
            tvGia = itemView.findViewById(R.id.tvGia);
            tvHinhThuc = itemView.findViewById(R.id.tvhinhthuc);
            tvTrangThai = itemView.findViewById(R.id.tvtrangthai_thanhtoan);
            imageButton = itemView.findViewById(R.id.imageButton1);
            btnEdit = itemView.findViewById(R.id.edit);
            btnDelete = itemView.findViewById(R.id.deleted);
        }
    }
}
