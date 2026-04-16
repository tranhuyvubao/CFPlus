package com.example.do_an_hk1_androidstudio;

public class SanPham {
    private final String productId;
    private final String ten;
    private final String gia;
    private final String giamGia;
    private final String hinhAnh;

    public SanPham(String productId, String ten, String gia, String hinhAnh) {
        this(productId, ten, gia, hinhAnh, "");
    }

    public SanPham(String productId, String ten, String gia, String hinhAnh, String giamGia) {
        this.productId = productId;
        this.ten = ten;
        this.gia = gia;
        this.hinhAnh = hinhAnh;
        this.giamGia = giamGia == null ? "" : giamGia;
    }

    public String getProductId() {
        return productId;
    }

    public String getTen() {
        return ten;
    }

    public String getGia() {
        return gia;
    }

    public String getGiamGia() {
        return giamGia;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }
}
