package com.example.do_an_hk1_androidstudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;

import java.util.List;

public class DonHangAdapter extends RecyclerView.Adapter<DonHangAdapter.DonHangViewHolder> {

    private final List<DonHang> list;
    private final Context context;
    private final OrderCloudRepository orderRepository;

    public DonHangAdapter(Context context, List<DonHang> list) {
        this.context = context;
        this.list = list;
        this.orderRepository = new OrderCloudRepository(context);
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

        holder.tenban.setText("Tên bàn: " + safe(donHang.tenBan, "-"));
        holder.tvTen.setText("Tên sản phẩm: " + safe(donHang.tenSanPham, "-"));
        holder.tvSize.setText("Size: " + safe(donHang.size, "-"));
        holder.tvDa.setText("Mức đá/Ghi chú: " + safe(donHang.mucDa, "-"));
        holder.tvSoLuong.setText("Số lượng: " + donHang.soLuong);
        holder.tvGia.setText("Tổng tiền: " + donHang.tongTien + "đ");
        holder.tvHinhThuc.setText("Hình thức: " + safe(donHang.hinhThuc, "-"));
        holder.tvTrangThai.setText("Trạng thái: " + safe(donHang.trangThai, "Chưa thanh toán"));

        if ("Đã thanh toán".equals(donHang.trangThai)) {
            holder.tvTrangThai.setTextColor(0xFF00C853);
        } else if ("Đã hủy".equals(donHang.trangThai)) {
            holder.tvTrangThai.setTextColor(0xFF757575);
        } else {
            holder.tvTrangThai.setTextColor(0xFFDD2C00);
        }

        Glide.with(context).load(donHang.hinhAnh).into(holder.imageButton);

        holder.btnEdit.setOnClickListener(v -> showEditPaymentDialog(holder, donHang));
        holder.btnDelete.setOnClickListener(v -> deleteOrder(holder, donHang));
        holder.btnChuyenBan.setOnClickListener(v -> showMoveTableDialog(holder, donHang));
    }

    private void showEditPaymentDialog(@NonNull DonHangViewHolder holder, @NonNull DonHang donHang) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.sua_trangthai_thanhtoan, null);
        CheckBox checkBox = dialogView.findViewById(R.id.checkBox);
        checkBox.setChecked("Đã thanh toán".equals(donHang.getTrangThai()));

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle("Cập nhật trạng thái thanh toán")
                .setPositiveButton("Lưu", (dialogInterface, i) -> {
                    if (donHang.getOrderId() == null || donHang.getOrderId().trim().isEmpty()) {
                        return;
                    }

                    boolean paid = checkBox.isChecked();
                    orderRepository.setOrderPaymentStatus(
                            donHang.getOrderId(),
                            donHang.tongTien,
                            paid,
                            (success, message) -> {
                                if (!success) {
                                    Toast.makeText(context, message == null ? "Không cập nhật được trạng thái." : message, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                donHang.trangThai = paid ? "Đã thanh toán" : "Chưa thanh toán";
                                notifyItemChanged(holder.getBindingAdapterPosition());
                            }
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteOrder(@NonNull DonHangViewHolder holder, @NonNull DonHang donHang) {
        if (donHang.getOrderId() == null || donHang.getOrderId().trim().isEmpty()) {
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Hủy đơn")
                .setMessage("Bạn có chắc muốn hủy đơn của món \"" + safe(donHang.tenSanPham, "-") + "\" không?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> orderRepository.cancelOrder(
                        donHang.getOrderId(),
                        (success, message) -> {
                            if (!success) {
                                Toast.makeText(context, message == null ? "Không hủy được đơn." : message, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            donHang.trangThai = "Đã hủy";
                            notifyItemChanged(holder.getBindingAdapterPosition());
                        }
                ))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showMoveTableDialog(@NonNull DonHangViewHolder holder, @NonNull DonHang donHang) {
        if (donHang.getOrderId() == null || donHang.getOrderId().trim().isEmpty()) {
            return;
        }

        EditText input = new EditText(context);
        input.setHint("Nhập tên bàn mới");
        if (donHang.tenBan != null) {
            input.setText(donHang.tenBan);
        }

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Chuyển bàn")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String tenBanMoi = input.getText().toString().trim();
                    if (tenBanMoi.isEmpty()) {
                        return;
                    }

                    orderRepository.updateOrderTable(
                            donHang.getOrderId(),
                            tenBanMoi,
                            (success, message) -> {
                                if (!success) {
                                    Toast.makeText(context, message == null ? "Không chuyển được bàn." : message, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                donHang.tenBan = tenBanMoi;
                                notifyItemChanged(holder.getBindingAdapterPosition());
                            }
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public static class DonHangViewHolder extends RecyclerView.ViewHolder {
        TextView tenban;
        TextView tvTen;
        TextView tvSize;
        TextView tvDa;
        TextView tvSoLuong;
        TextView tvGia;
        TextView tvHinhThuc;
        TextView tvTrangThai;
        ImageButton imageButton;
        TextView btnEdit;
        TextView btnChuyenBan;
        TextView btnDelete;

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
            btnChuyenBan = itemView.findViewById(R.id.chuyenban);
            btnDelete = itemView.findViewById(R.id.deleted);
        }
    }
}
