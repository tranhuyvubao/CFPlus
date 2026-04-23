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

import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;

public class FragmentStaffAccount extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_account, container, false);
        LocalSessionManager sessionManager = new LocalSessionManager(requireContext());

        TextView tvName = view.findViewById(R.id.tvStaffAccountName);
        TextView tvEmail = view.findViewById(R.id.tvStaffAccountEmail);
        tvName.setText(displayOrDefault(sessionManager.getCurrentUserFullName(), "Nhân viên"));
        tvEmail.setText(displayOrDefault(sessionManager.getCurrentUserEmail(), "Chưa cập nhật email"));

        view.findViewById(R.id.rowStaffOnlineOrders)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), NhanDonOnlineActivity.class)));
        view.findViewById(R.id.rowStaffTables)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), NhanDonTaiBanActivity.class)));
        view.findViewById(R.id.rowStaffReservations)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), XuLyDatBanActivity.class)));
        view.findViewById(R.id.rowStaffTakeaway)
                .setOnClickListener(v -> openTakeawayBill());
        view.findViewById(R.id.rowStaffBar)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), KdsBarActivity.class)));
        view.findViewById(R.id.rowStaffStockIn)
                .setOnClickListener(v -> openInventoryAction(QuanLyKhoActivity.ACTION_STOCK_IN));
        view.findViewById(R.id.rowStaffShiftUsage)
                .setOnClickListener(v -> openInventoryAction(QuanLyKhoActivity.ACTION_SHIFT_USAGE));
        view.findViewById(R.id.rowStaffShiftAttendance)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), ShiftCheckInActivity.class)));
        view.findViewById(R.id.rowStaffEditProfile)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), SuaThongTinCaNhanActivity.class)));

        return view;
    }

    private String displayOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void openTakeawayBill() {
        Intent intent = new Intent(requireContext(), HoaDonBanActivity.class);
        intent.putExtra(HoaDonBanActivity.EXTRA_TABLE_ID, TableCloudRepository.TAKEAWAY_TABLE_ID);
        intent.putExtra(HoaDonBanActivity.EXTRA_TABLE_NAME, "Mang về");
        startActivity(intent);
    }

    private void openInventoryAction(String action) {
        Intent intent = new Intent(requireContext(), QuanLyKhoActivity.class);
        intent.putExtra(QuanLyKhoActivity.EXTRA_STAFF_ACTION, action);
        startActivity(intent);
    }
}
