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

import java.util.List;

public class SanPhamAdapter extends RecyclerView.Adapter<SanPhamAdapter.SanPhamViewHolder> {
    private List<SanPham> sanPhamList;
    private Context context;

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
        holder.ten.setText(sp.getTen());
        holder.gia.setText(sp.getGia());

        Glide.with(context)
                .load(sp.getHinhAnh())
                .placeholder(R.drawable.loading_spinner)
                .error(R.drawable.cfplus)
                .into(holder.hinhAnh);
        holder.bind(sp);

        // Sự kiện click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, chitiet_sanpham.class);
            intent.putExtra("Ten", sp.getTen());
            intent.putExtra("Gia", sp.getGia());
            intent.putExtra("hinhAnh", sp.getHinhAnh());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return sanPhamList.size();
    }

    public static class SanPhamViewHolder extends RecyclerView.ViewHolder {
        ImageButton hinhAnh, nutThem;
        TextView ten, gia;

        public SanPhamViewHolder(@NonNull View itemView) {
            super(itemView);
            hinhAnh = itemView.findViewById(R.id.imageButton1);
            nutThem = itemView.findViewById(R.id.imageButton2);
            ten = itemView.findViewById(R.id.tvTen);
            gia = itemView.findViewById(R.id.tvGia);
        }

        public void bind(SanPham sp) {
            ten.setText(sp.getTen());
            gia.setText(sp.getGia());
            Glide.with(itemView.getContext()).load(sp.getHinhAnh()).into(hinhAnh);
        }
    }
}
