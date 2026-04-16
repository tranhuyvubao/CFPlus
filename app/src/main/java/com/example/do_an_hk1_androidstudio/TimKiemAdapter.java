package com.example.do_an_hk1_androidstudio;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;

import java.util.List;

public class TimKiemAdapter extends BaseAdapter {

    private final Context context;
    private final List<SanPham> sanPhamList;
    private final LayoutInflater inflater;

    public TimKiemAdapter(Context context, List<SanPham> sanPhamList) {
        this.context = context;
        this.sanPhamList = sanPhamList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return sanPhamList.size();
    }

    @Override
    public Object getItem(int i) {
        return sanPhamList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    static class ViewHolder {
        ImageButton imgProduct, btnAddCart;
        TextView tvTen, tvGia;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.timkiem_sanpham, parent, false);
            holder = new ViewHolder();
            holder.imgProduct = convertView.findViewById(R.id.imageButton1);
            holder.tvTen = convertView.findViewById(R.id.tvTen);
            holder.tvGia = convertView.findViewById(R.id.tvGia);
            holder.btnAddCart = convertView.findViewById(R.id.imageButton2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SanPham sanPham = sanPhamList.get(pos);
        holder.tvTen.setText(sanPham.getTen());
        holder.tvGia.setText(formatGia(sanPham.getGia()));

        Glide.with(context)
                .load(sanPham.getHinhAnh())
                .centerCrop()
                .placeholder(R.drawable.loading_spinner)
                .error(R.drawable.cfplus)
                .into(holder.imgProduct);

        View.OnClickListener openDetail = v -> {
            Intent intent = new Intent(context, chitiet_sanpham.class);
            intent.putExtra("productId", sanPham.getProductId());
            intent.putExtra("Ten", sanPham.getTen());
            intent.putExtra("Gia", sanPham.getGia());
            intent.putExtra("hinhAnh", sanPham.getHinhAnh());
            context.startActivity(intent);
        };

        holder.btnAddCart.setOnClickListener(openDetail);
        holder.imgProduct.setOnClickListener(openDetail);
        convertView.setOnClickListener(openDetail);
        return convertView;
    }

    private String formatGia(String raw) {
        try {
            return MoneyFormatter.format(Long.parseLong(raw.replaceAll("[^0-9]", "")));
        } catch (Exception ignored) {
            return raw == null ? "0đ" : raw;
        }
    }
}
