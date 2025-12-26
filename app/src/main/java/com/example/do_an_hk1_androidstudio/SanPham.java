package com.example.do_an_hk1_androidstudio;

public class SanPham {
    private String Ten;
    private String Gia;
    private String GiamGia;
    private String hinhAnh;

    public SanPham() {}

    // Constructor KHÔNG có giảm giá
    public SanPham(String ten, String gia, String hinhAnh) {
        this.Ten = ten;
        this.Gia = gia;
        this.hinhAnh = hinhAnh;
        this.GiamGia = ""; // hoặc null
    }

    // Constructor CÓ giảm giá nếu bạn cần dùng
    public SanPham(String ten, String gia, String hinhAnh, String giamGia) {
        this.Ten = ten;
        this.Gia = gia;
        this.hinhAnh = hinhAnh;
        this.GiamGia = giamGia;
    }

    public String getTen() {
        return Ten;
    }

    public String getGia() {
        return Gia;
    }

    public String getGiamGia() {
        return GiamGia;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }
}

