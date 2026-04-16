package com.example.do_an_hk1_androidstudio;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;

import java.util.List;

public class SanPhamAdapter extends RecyclerView.Adapter<SanPhamAdapter.SanPhamViewHolder> {
    private final List<SanPham> sanPhamList;
    private final Context context;

    public SanPhamAdapter(Context context, List<SanPham> sanPhamList) {
        this.sanPhamList = sanPhamList;
        this.context = context;
    }

    @NonNull
    @Override
    public SanPhamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_layout, parent, false);
        return new SanPhamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SanPhamViewHolder holder, int position) {
        SanPham sp = sanPhamList.get(position);
        holder.bind(sp);
        holder.itemView.setOnClickListener(v -> openProductDetail(sp));
    }

    @Override
    public int getItemCount() {
        return sanPhamList.size();
    }

    private void openProductDetail(SanPham sp) {
        Intent intent = new Intent(context, chitiet_sanpham.class);
        intent.putExtra("productId", sp.getProductId());
        intent.putExtra("Ten", sp.getTen());
        intent.putExtra("Gia", sp.getGia());
        intent.putExtra("hinhAnh", sp.getHinhAnh());
        context.startActivity(intent);
    }

    public static class SanPhamViewHolder extends RecyclerView.ViewHolder {
        ImageButton hinhAnh;
        TextView ten, gia;

        public SanPhamViewHolder(@NonNull View itemView) {
            super(itemView);
            hinhAnh = itemView.findViewById(R.id.imageButton1);
            ten = itemView.findViewById(R.id.tvTen);
            gia = itemView.findViewById(R.id.tvGia);
        }

        public void bind(SanPham sp) {
            ten.setText(sp.getTen());
            gia.setText(formatGia(sp.getGia()));
            Glide.with(itemView.getContext())
                    .load(sp.getHinhAnh())
                    .placeholder(R.drawable.loading_spinner)
                    .error(R.drawable.cfplus)
                    .into(hinhAnh);
        }
    }

    private static String formatGia(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "0đ";
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try {
                return MoneyFormatter.format(Long.parseLong(digits));
            } catch (NumberFormatException ignored) {
            }
        }
        return raw;
    }
}
