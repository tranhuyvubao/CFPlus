package com.example.do_an_hk1_androidstudio;

public class DonHang {
    public String tenBan, tenSanPham, size, mucDa, hinhThuc, trangThai, hinhAnh;
    public int soLuong, tongTien;
    private String docId; // Lưu document ID để dùng khi sửa/xóa

    public DonHang() {}

    public DonHang(String tenBan, String tenSanPham, String size, String mucDa, int soLuong,
                   int tongTien, String hinhThuc, String trangThai, String hinhAnh) {
        this.tenBan = tenBan;
        this.tenSanPham = tenSanPham;
        this.size = size;
        this.mucDa = mucDa;
        this.soLuong = soLuong;
        this.tongTien = tongTien;
        this.hinhThuc = hinhThuc;
        this.trangThai = trangThai;
        this.hinhAnh = hinhAnh;
    }

    public String getTenBan() {
        return tenBan;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public String getSize() {
        return size;
    }

    public String getMucDa() {
        return mucDa;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public int getTongTien() {
        return tongTien;
    }

    public String getHinhThuc() {
        return hinhThuc;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }
}
