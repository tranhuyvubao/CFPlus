package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;

public class Fragment_Setting extends Fragment {

    private LocalSessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment__setting, container, false);
        sessionManager = new LocalSessionManager(requireContext());

        TextView tvTaiKhoan = view.findViewById(R.id.tvTaiKhoan);
        TextView tvThongTinCuaHang = view.findViewById(R.id.tvThongTinCuaHang);
        TextView tvThongKeHomNay = view.findViewById(R.id.tvThongKeHomNay);
        TextView tvNhanDonOnline = view.findViewById(R.id.tvNhanDonOnline);
        TextView tvNhanDonTaiBan = view.findViewById(R.id.tvNhanDonTaiBan);
        TextView tvQuanLyNhanVien = view.findViewById(R.id.tvQuanLyNhanVien);
        TextView tvQuanLyMon = view.findViewById(R.id.tvQuanLyMon);
        TextView tvQuanLyDanhMuc = view.findViewById(R.id.tvQuanLyDanhMuc);
        TextView tvQuanLyBan = view.findViewById(R.id.tvQuanLyBan);
        TextView tvQuanLyKho = view.findViewById(R.id.tvQuanLyKho);
        TextView tvQuanLyKhuyenMai = view.findViewById(R.id.tvQuanLyKhuyenMai);
        TextView tvSeedFirebase = view.findViewById(R.id.tvSeedFirebase);
        TextView tvDatBan = view.findViewById(R.id.tvDatBan);
        TextView tvDonHangCuaToi = view.findViewById(R.id.tvDonHangCuaToi);
        TextView tvDatMonOnline = view.findViewById(R.id.tvDatMonOnline);
        TextView tvDatMonTaiBan = view.findViewById(R.id.tvDatMonTaiBan);
        TextView tvHoSoKhachHang = view.findViewById(R.id.tvHoSoKhachHang);
        TextView tvChatboxAI = view.findViewById(R.id.tvChatboxAI);
        TextView tvQuanLyKhachHang = view.findViewById(R.id.tvQuanLyKhachHang);
        TextView tvThongKeTongHop = view.findViewById(R.id.tvThongKeTongHop);
        TextView tvDangXuat = view.findViewById(R.id.tvDangXuat);

        tvTaiKhoan.setOnClickListener(v -> {
            Fragment_Account fragmentAccount = new Fragment_Account();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, fragmentAccount)
                    .addToBackStack(null)
                    .commit();
        });

        tvThongTinCuaHang.setOnClickListener(v -> startActivity(new Intent(getActivity(), ThongTinCuaHangActivity.class)));
        tvThongKeHomNay.setOnClickListener(v -> startActivity(new Intent(getActivity(), ThongKeActivity.class)));
        tvNhanDonOnline.setOnClickListener(v -> startActivity(new Intent(getActivity(), NhanDonOnlineActivity.class)));
        tvNhanDonTaiBan.setOnClickListener(v -> startActivity(new Intent(getActivity(), NhanDonTaiBanActivity.class)));

        tvQuanLyNhanVien.setOnClickListener(v -> openForRole("manager", QuanLyNhanVienActivity.class, "Chỉ quản lý mới truy cập được chức năng này"));
        tvQuanLyMon.setOnClickListener(v -> openForRole("manager", QuanLyMonActivity.class, "Chỉ quản lý mới truy cập được chức năng này"));
        tvQuanLyDanhMuc.setOnClickListener(v -> openForRole("manager", QuanLyDanhMucActivity.class, "Chỉ quản lý mới truy cập được chức năng này"));
        tvQuanLyBan.setOnClickListener(v -> openForRole("manager", QuanLyBanActivity.class, "Chỉ quản lý mới truy cập được chức năng này"));
        tvQuanLyKho.setOnClickListener(v -> openForRole("manager", QuanLyKhoActivity.class, "Chỉ quản lý mới truy cập được chức năng này"));
        tvQuanLyKhuyenMai.setOnClickListener(v -> openForRole("manager", QuanLyKhuyenMaiActivity.class, "Chỉ quản lý mới truy cập được chức năng này"));
        tvSeedFirebase.setOnClickListener(v -> openForRole("manager", FirebaseSeederActivity.class, "Chỉ quản lý mới dùng được chức năng này"));
        tvQuanLyKhachHang.setOnClickListener(v -> openForRole("manager", QuanLyKhachHangActivity.class, "Chỉ quản lý mới truy cập được chức năng này"));
        tvThongKeTongHop.setOnClickListener(v -> openForRole("manager", ThongKeTongHopActivity.class, "Chỉ quản lý mới truy cập được chức năng này"));

        tvDatBan.setOnClickListener(v -> openForRole("customer", DatBanActivity.class, "Chức năng này dành cho khách hàng"));
        tvDonHangCuaToi.setOnClickListener(v -> openForRole("customer", DonHangCuaToiActivity.class, "Chức năng này dành cho khách hàng"));
        tvDatMonOnline.setOnClickListener(v -> openForRole("customer", DatMonOnlineActivity.class, "Chức năng này dành cho khách hàng"));
        tvHoSoKhachHang.setOnClickListener(v -> openForRole("customer", HoSoKhachHangActivity.class, "Chức năng này dành cho khách hàng"));
        tvChatboxAI.setOnClickListener(v -> openForRole("customer", ChatboxActivity.class, "Chức năng này dành cho khách hàng"));

        tvDangXuat.setOnClickListener(v -> {
            sessionManager.clear();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        applyRoleVisibility(
                sessionManager.getCurrentUserRole(),
                tvThongKeHomNay,
                tvNhanDonOnline,
                tvNhanDonTaiBan,
                tvQuanLyNhanVien,
                tvQuanLyMon,
                tvQuanLyDanhMuc,
                tvQuanLyBan,
                tvQuanLyKho,
                tvQuanLyKhuyenMai,
                tvSeedFirebase,
                tvDatBan,
                tvDonHangCuaToi,
                tvDatMonOnline,
                tvDatMonTaiBan,
                tvHoSoKhachHang,
                tvChatboxAI,
                tvQuanLyKhachHang,
                tvThongKeTongHop
        );

        return view;
    }

    private void openForRole(String role, Class<?> activityClass, String deniedMessage) {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(getActivity(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!role.equals(sessionManager.getCurrentUserRole())) {
            Toast.makeText(getActivity(), deniedMessage, Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(getActivity(), activityClass));
    }

    private void applyRoleVisibility(String role,
                                     TextView tvThongKeHomNay,
                                     TextView tvNhanDonOnline,
                                     TextView tvNhanDonTaiBan,
                                     TextView tvQuanLyNhanVien,
                                     TextView tvQuanLyMon,
                                     TextView tvQuanLyDanhMuc,
                                     TextView tvQuanLyBan,
                                     TextView tvQuanLyKho,
                                     TextView tvQuanLyKhuyenMai,
                                     TextView tvSeedFirebase,
                                     TextView tvDatBan,
                                     TextView tvDonHangCuaToi,
                                     TextView tvDatMonOnline,
                                     TextView tvDatMonTaiBan,
                                     TextView tvHoSoKhachHang,
                                     TextView tvChatboxAI,
                                     TextView tvQuanLyKhachHang,
                                     TextView tvThongKeTongHop) {
        boolean isCustomer = "customer".equals(role);

        tvThongKeHomNay.setVisibility(View.GONE);
        tvNhanDonOnline.setVisibility(View.GONE);
        tvNhanDonTaiBan.setVisibility(View.GONE);
        tvQuanLyNhanVien.setVisibility(View.GONE);
        tvQuanLyMon.setVisibility(View.GONE);
        tvQuanLyDanhMuc.setVisibility(View.GONE);
        tvQuanLyBan.setVisibility(View.GONE);
        tvQuanLyKho.setVisibility(View.GONE);
        tvQuanLyKhuyenMai.setVisibility(View.GONE);
        tvSeedFirebase.setVisibility(View.GONE);
        tvQuanLyKhachHang.setVisibility(View.GONE);
        tvThongKeTongHop.setVisibility(View.GONE);

        tvDatBan.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        tvDonHangCuaToi.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        tvDatMonOnline.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        tvDatMonTaiBan.setVisibility(View.GONE);
        tvHoSoKhachHang.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        tvChatboxAI.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
    }
}
