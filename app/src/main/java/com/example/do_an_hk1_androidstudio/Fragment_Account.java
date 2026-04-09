package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Fragment_Account extends Fragment {

    private TextView tvHoTen;
    private TextView tvNgaySinh;
    private TextView tvGioiTinh;
    private TextView tvEmail;
    private TextView tvSoDienThoai;
    private TextView tvProfileTitle;
    private UserCloudRepository userCloudRepository;
    private LocalSessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment__account, container, false);

        tvHoTen = view.findViewById(R.id.tvHoTen);
        tvNgaySinh = view.findViewById(R.id.tvNgaySinh);
        tvGioiTinh = view.findViewById(R.id.tvGioiTinh);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvSoDienThoai = view.findViewById(R.id.tvSoDienThoai);
        tvProfileTitle = view.findViewById(R.id.tvProfileTitle);
        Button btnSuaTT = view.findViewById(R.id.btn_suaTT);
        Button btnTroVe = view.findViewById(R.id.btnTroVe);

        userCloudRepository = new UserCloudRepository(requireContext());
        sessionManager = new LocalSessionManager(requireContext());

        loadUserData();

        btnSuaTT.setOnClickListener(v -> startActivity(new Intent(getActivity(), SuaThongTinCaNhanActivity.class)));
        btnTroVe.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            loadUserData();
        }
    }

    private void loadUserData() {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            showLoggedOutState();
            return;
        }

        userCloudRepository.getUserById(userId, (user, message) -> {
            if (!isAdded()) {
                return;
            }
            if (user == null) {
                showLoggedOutState();
                return;
            }

            tvProfileTitle.setText(resolveRoleTitle(user));
            tvHoTen.setText(displayOrDefault(user.getFullName()));
            tvNgaySinh.setText(formatDate(user.getBirthdayMillis()));
            tvGioiTinh.setText(displayOrDefault(user.getGender()));
            tvEmail.setText(displayOrDefault(user.getEmail()));
            tvSoDienThoai.setText(displayOrDefault(user.getPhone()));
        });
    }

    private void showLoggedOutState() {
        tvProfileTitle.setText("Thông tin cá nhân");
        tvEmail.setText("Chưa đăng nhập");
        tvHoTen.setText("Chưa cập nhật");
        tvNgaySinh.setText("Chưa cập nhật");
        tvGioiTinh.setText("Chưa cập nhật");
        tvSoDienThoai.setText("Chưa cập nhật");
    }

    private String resolveRoleTitle(LocalUser user) {
        if ("staff".equalsIgnoreCase(user.getRole())) {
            return "Hồ sơ nhân viên";
        }
        if ("manager".equalsIgnoreCase(user.getRole())) {
            return "Hồ sơ quản lý";
        }
        if ("customer".equalsIgnoreCase(user.getRole())) {
            return "Hồ sơ khách hàng";
        }
        return "Thông tin cá nhân";
    }

    private String displayOrDefault(String value) {
        return value == null || value.trim().isEmpty() ? "Chưa cập nhật" : value;
    }

    private String formatDate(Long millis) {
        if (millis == null) {
            return "Chưa cập nhật";
        }
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }
}
