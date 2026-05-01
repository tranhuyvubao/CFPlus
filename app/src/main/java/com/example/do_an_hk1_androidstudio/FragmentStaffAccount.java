package com.example.do_an_hk1_androidstudio;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.room.CfPlusLocalDatabase;
import com.example.do_an_hk1_androidstudio.ui.NotificationCenter;

public class FragmentStaffAccount extends Fragment {
    private LocalSessionManager sessionManager;
    private TextView tvNotificationBadge;
    private final BroadcastReceiver inboxUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            bindUnreadBadge();
        }
    };
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> bindUnreadBadge());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_account, container, false);
        sessionManager = new LocalSessionManager(requireContext());

        TextView tvName = view.findViewById(R.id.tvStaffAccountName);
        TextView tvEmail = view.findViewById(R.id.tvStaffAccountEmail);
        tvNotificationBadge = view.findViewById(R.id.tvStaffNotificationBadge);
        tvName.setText(displayOrDefault(sessionManager.getCurrentUserFullName(), "Nhân viên"));
        tvEmail.setText(displayOrDefault(sessionManager.getCurrentUserEmail(), "Chưa cập nhật email"));

        view.findViewById(R.id.btnStaffNotifications).setOnClickListener(v -> {
            requestNotificationPermissionIfNeeded();
            startActivity(new Intent(requireContext(), NotificationInboxActivity.class));
        });

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

    @Override
    public void onStart() {
        super.onStart();
        requireContext().registerReceiver(
                inboxUpdatedReceiver,
                new IntentFilter(NotificationCenter.ACTION_INBOX_UPDATED),
                Context.RECEIVER_NOT_EXPORTED
        );
        bindUnreadBadge();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    public void onStop() {
        requireContext().unregisterReceiver(inboxUpdatedReceiver);
        super.onStop();
    }

    private String displayOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void bindUnreadBadge() {
        if (tvNotificationBadge == null) {
            return;
        }
        String userId = sessionManager.getCurrentUserId();
        int unreadCount = userId == null
                ? 0
                : CfPlusLocalDatabase.getInstance(requireContext()).notificationInboxDao().countUnreadForUser(userId);
        if (unreadCount <= 0) {
            tvNotificationBadge.setVisibility(View.GONE);
            return;
        }
        tvNotificationBadge.setVisibility(View.VISIBLE);
        tvNotificationBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
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
