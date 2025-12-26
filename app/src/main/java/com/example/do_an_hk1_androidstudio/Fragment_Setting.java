package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.do_an_hk1_androidstudio.Fragment_Account;
import com.example.do_an_hk1_androidstudio.MainActivity;
import com.example.do_an_hk1_androidstudio.R;
import com.example.do_an_hk1_androidstudio.ThongTinCuaHangActivity;
import com.google.firebase.auth.FirebaseAuth;

public class Fragment_Setting extends Fragment {

    private TextView tvTaiKhoan, tvThongTinCuaHang, tvDangXuat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment__setting, container, false);

        tvTaiKhoan = view.findViewById(R.id.tvTaiKhoan);
        tvThongTinCuaHang = view.findViewById(R.id.tvThongTinCuaHang);
        tvDangXuat = view.findViewById(R.id.tvDangXuat);

        // 1. Mở Fragment_Account
        tvTaiKhoan.setOnClickListener(v -> {
            Fragment_Account fragmentAccount = new Fragment_Account();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, fragmentAccount) // thay fragment_container bằng id FrameLayout của bạn
                    .addToBackStack(null)
                    .commit();
        });

        // 2. Mở activity chứa giao diện thongtin_cuahang.xml
        tvThongTinCuaHang.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ThongTinCuaHangActivity.class);
            startActivity(intent);
        });

        // 3. Đăng xuất
        tvDangXuat.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear stack
            startActivity(intent);
        });

        return view;
    }
}
