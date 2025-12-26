package com.example.do_an_hk1_androidstudio;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TimKiemAdapter extends BaseAdapter {

    private Context context;
    private List<SanPham> sanPhamList;
    private LayoutInflater inflater;

    public TimKiemAdapter(Context context, List<SanPham> sanPhamList) {
        this.context = context;
        this.sanPhamList = sanPhamList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override public int getCount() { return sanPhamList.size(); }
    @Override public Object getItem(int i) { return sanPhamList.get(i); }
    @Override public long getItemId(int i) { return i; }

    static class ViewHolder {
        ImageButton imgProduct, btnAddCart;
        TextView tvTen, tvGia;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.timkiem_sanpham, parent, false);
            h = new ViewHolder();
            h.imgProduct = convertView.findViewById(R.id.imageButton1);
            h.tvTen      = convertView.findViewById(R.id.tvTen);
            h.tvGia      = convertView.findViewById(R.id.tvGia);
            h.btnAddCart = convertView.findViewById(R.id.imageButton2);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        SanPham sp = sanPhamList.get(pos);
        // Decode HTML entities nếu có (ví dụ: &#273; -> đ, &#225; -> á)
        String tenSanPham = sp.getTen();
        if (tenSanPham != null && tenSanPham.contains("&#")) {
            // Nếu có HTML entities, decode chúng
            tenSanPham = Html.fromHtml(tenSanPham, Html.FROM_HTML_MODE_LEGACY).toString();
        }
        h.tvTen.setText(tenSanPham);

        try {
            int g = Integer.parseInt(sp.getGia());
            String fmt = NumberFormat.getNumberInstance(Locale.US).format(g) + " VNĐ";
            h.tvGia.setText(fmt);
        } catch (Exception e) {
            h.tvGia.setText(sp.getGia() + " VNĐ");
        }

        Glide.with(context)
                .load(sp.getHinhAnh())
                .centerCrop()
                .into(h.imgProduct);

        // Nút "+" cũng mở chi tiết sản phẩm để đặt hàng
        h.btnAddCart.setOnClickListener(v -> {
            Intent it = new Intent(context, chitiet_sanpham.class);
            it.putExtra("Ten", sp.getTen());
            it.putExtra("Gia", sp.getGia());
            it.putExtra("hinhAnh", sp.getHinhAnh());
            context.startActivity(it);
        });
        
        // Click vào toàn bộ item để mở chi tiết sản phẩm
        convertView.setOnClickListener(v -> {
            Intent it = new Intent(context, chitiet_sanpham.class);
            it.putExtra("Ten", sp.getTen());
            it.putExtra("Gia", sp.getGia());
            it.putExtra("hinhAnh", sp.getHinhAnh());
            context.startActivity(it);
        });
        
        // Click vào hình ảnh cũng mở chi tiết
        h.imgProduct.setOnClickListener(v -> {
            Intent it = new Intent(context, chitiet_sanpham.class);
            it.putExtra("Ten", sp.getTen());
            it.putExtra("Gia", sp.getGia());
            it.putExtra("hinhAnh", sp.getHinhAnh());
            context.startActivity(it);
        });
        
        // Đảm bảo ImageButton có thể click được
        h.imgProduct.setClickable(true);
        h.imgProduct.setFocusable(true);

        return convertView;
    }
}
