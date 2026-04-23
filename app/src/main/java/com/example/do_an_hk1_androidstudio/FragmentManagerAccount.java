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

public class FragmentManagerAccount extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_account, container, false);
        LocalSessionManager sessionManager = new LocalSessionManager(requireContext());

        TextView tvName = view.findViewById(R.id.tvManagerAccountName);
        TextView tvEmail = view.findViewById(R.id.tvManagerAccountEmail);
        tvName.setText(displayOrDefault(sessionManager.getCurrentUserFullName(), "Quản lý"));
        tvEmail.setText(displayOrDefault(sessionManager.getCurrentUserEmail(), "Chưa cập nhật email"));

        view.findViewById(R.id.rowManagerAnalytics)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), ThongKeTongHopActivity.class)));
        view.findViewById(R.id.rowManagerProducts)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), QuanLyMonActivity.class)));
        view.findViewById(R.id.rowManagerBanners)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), ManagerBannersActivity.class)));
        view.findViewById(R.id.rowManagerStock)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), QuanLyKhoActivity.class)));
        view.findViewById(R.id.rowManagerStaff)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), QuanLyNhanVienActivity.class)));
        view.findViewById(R.id.rowManagerLogs)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), OrderActionLogActivity.class)));
        view.findViewById(R.id.rowManagerStoreSettings)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), ManagerStoreSettingsActivity.class)));
        view.findViewById(R.id.rowManagerEditProfile)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), SuaThongTinCaNhanActivity.class)));

        return view;
    }

    private String displayOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
