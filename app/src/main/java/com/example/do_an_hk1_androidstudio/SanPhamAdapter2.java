package com.example.do_an_hk1_androidstudio;

public class SanPhamAdapter2 {
    //private List<SanPham> sanPhamList;
    //    private Context context;
    //
    //    public SanPhamAdapter(Context context, List<SanPham> sanPhamList) {
    //        this.sanPhamList = sanPhamList;
    //        this.context = context;
    //    }
    //
    //    @NonNull
    //    @Override
    //    public SanPhamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    //        // Sửa tại đây: dùng parent.getContext() để đảm bảo context luôn đúng
    //        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_layout, parent, false);
    //        return new SanPhamViewHolder(view);
    //    }
    //
    //    @Override
    //    public void onBindViewHolder(@NonNull SanPhamViewHolder holder, int position) {
    //        SanPham sp = sanPhamList.get(position);
    //        holder.ten.setText(sp.getTen());
    //        holder.gia.setText(sp.getGia());
    //        holder.giamGia.setText(sp.getGiamGia());
    //
    //        Glide.with(context)
    //                .load(sp.getHinhAnh())
    //                .placeholder(R.drawable.loading_spinner) // ảnh tạm khi đang tải
    //                .error(R.drawable.cfplus) // ảnh hiển thị khi lỗi
    //                .into(holder.hinhAnh);
    //        holder.bind(sp);
    //
    //        // Sự kiện click
    //        holder.itemView.setOnClickListener(v -> {
    //            Intent intent = new Intent(context, chitiet_sanpham.class);
    //            intent.putExtra("Ten", sp.getTen());
    //            intent.putExtra("Gia", sp.getGia());
    //            intent.putExtra("GiamGia", sp.getGiamGia());
    //            intent.putExtra("hinhAnh", sp.getHinhAnh());
    //            context.startActivity(intent);
    //        });
    //    }
    //
    //
    //
    //
    //    @Override
    //    public int getItemCount() {
    //        return sanPhamList.size();
    //    }
    //
    //    public static class SanPhamViewHolder extends RecyclerView.ViewHolder {
    //        ImageButton hinhAnh, nutThem;
    //        TextView ten, gia, giamGia;
    //
    //        public SanPhamViewHolder(@NonNull View itemView) {
    //            super(itemView);
    //            hinhAnh = itemView.findViewById(R.id.imageButton1);
    //            nutThem = itemView.findViewById(R.id.imageButton2);
    //            ten = itemView.findViewById(R.id.tvTen);
    //            gia = itemView.findViewById(R.id.tvGia);
    //            giamGia = itemView.findViewById(R.id.tvGiamGia);
    //        }
    //        public void bind(SanPham sp) {
    //            ten.setText(sp.getTen());
    //            giamGia.setText(sp.getGia());
    //            // Sử dụng thư viện như Glide để load ảnh
    //            Glide.with(itemView.getContext()).load(sp.getHinhAnh()).into(hinhAnh);
    //        }
    //    }
}
