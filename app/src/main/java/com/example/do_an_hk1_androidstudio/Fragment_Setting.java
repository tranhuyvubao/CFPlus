package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

        TextView tvSettingsHint = view.findViewById(R.id.tvSettingsHint);
        TextView tvThongBao = view.findViewById(R.id.tvThongBao);
        TextView tvThongTinCuaHang = view.findViewById(R.id.tvThongTinCuaHang);
        TextView tvDangXuat = view.findViewById(R.id.tvDangXuat);

        tvThongBao.setOnClickListener(v -> startActivity(new Intent(getActivity(), NotificationInboxActivity.class)));
        tvThongTinCuaHang.setOnClickListener(v -> startActivity(new Intent(getActivity(), ThongTinCuaHangActivity.class)));
        tvDangXuat.setOnClickListener(v -> {
            sessionManager.clear();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        applyRoleCopy(sessionManager.getCurrentUserRole(), tvSettingsHint);
        return view;
    }

    private void applyRoleCopy(String role, TextView tvSettingsHint) {
        if ("customer".equals(role)) {
            tvSettingsHint.setText("Menu, giỏ hàng và tài khoản đã có ở các tab riêng. Ở đây chỉ giữ cài đặt cần thiết.");
        } else {
            tvSettingsHint.setText("Các tác vụ vận hành nằm ở dashboard. Ở đây chỉ giữ thông tin ứng dụng và đăng xuất.");
        }
    }
}
