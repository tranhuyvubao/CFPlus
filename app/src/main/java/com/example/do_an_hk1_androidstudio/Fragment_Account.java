package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalUser;
import com.example.do_an_hk1_androidstudio.ui.MembershipTierHelper;
import com.example.do_an_hk1_androidstudio.ui.MoneyFormatter;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Fragment_Account extends Fragment {

    private TextView tvHoTen;
    private TextView tvNgaySinh;
    private TextView tvGioiTinh;
    private TextView tvEmail;
    private TextView tvSoDienThoai;
    private TextView tvProfileTitle;
    private TextView tvMembershipTier;
    private TextView tvMembershipSubtitle;
    private TextView tvMembershipSpent;
    private TextView tvMembershipOrders;
    private TextView tvMembershipPoints;
    private View cardMembership;
    private View sectionCustomerActions;

    private UserCloudRepository userCloudRepository;
    private OrderCloudRepository orderCloudRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration ordersListener;
    private int loyaltyPoint = 0;

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
        tvMembershipTier = view.findViewById(R.id.tvMembershipTier);
        tvMembershipSubtitle = view.findViewById(R.id.tvMembershipSubtitle);
        tvMembershipSpent = view.findViewById(R.id.tvMembershipSpent);
        tvMembershipOrders = view.findViewById(R.id.tvMembershipOrders);
        tvMembershipPoints = view.findViewById(R.id.tvMembershipPoints);
        cardMembership = view.findViewById(R.id.cardMembership);
        sectionCustomerActions = view.findViewById(R.id.sectionCustomerActions);

        View rowOrderHistory = view.findViewById(R.id.rowOrderHistory);
        View rowFavorites = view.findViewById(R.id.rowFavorites);
        View rowAddresses = view.findViewById(R.id.rowAddresses);
        View rowBooking = view.findViewById(R.id.rowBooking);
        View rowEditProfile = view.findViewById(R.id.rowEditProfile);

        userCloudRepository = new UserCloudRepository(requireContext());
        orderCloudRepository = new OrderCloudRepository(requireContext());
        sessionManager = new LocalSessionManager(requireContext());

        rowOrderHistory.setOnClickListener(v -> startActivity(new Intent(getActivity(), DonHangCuaToiActivity.class)));
        rowFavorites.setOnClickListener(v -> startActivity(new Intent(getActivity(), FavoritesActivity.class)));
        rowAddresses.setOnClickListener(v -> startActivity(new Intent(getActivity(), HoSoKhachHangActivity.class)));
        rowBooking.setOnClickListener(v -> startActivity(new Intent(getActivity(), DatBanActivity.class)));
        rowEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), SuaThongTinCaNhanActivity.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            loadUserData();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
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

            boolean isCustomer = isCustomerRole(user.getRole());
            sectionCustomerActions.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
            tvProfileTitle.setText(resolveRoleTitle(user));
            tvHoTen.setText(displayOrDefault(user.getFullName()));
            tvEmail.setText(displayOrDefault(user.getEmail()));
            tvNgaySinh.setText("Ngày sinh: " + formatDate(user.getBirthdayMillis()));
            tvGioiTinh.setText("Giới tính: " + displayOrDefault(user.getGender()));
            tvSoDienThoai.setText("Số điện thoại: " + displayOrDefault(user.getPhone()));
            bindMembership(user);
        });
    }

    private void bindMembership(LocalUser user) {
        boolean isCustomer = isCustomerRole(user.getRole());
        cardMembership.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        if (!isCustomer) {
            return;
        }

        userCloudRepository.getCustomerProfile(user.getUserId(), (profile, message) -> {
            loyaltyPoint = profile == null ? 0 : profile.getLoyaltyPoint();
            listenOrdersForTier(user.getUserId());
        });
    }

    private void listenOrdersForTier(String userId) {
        if (ordersListener != null) {
            ordersListener.remove();
        }
        ordersListener = orderCloudRepository.listenOrdersByCustomer(userId, orders -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> renderMembership(orders));
        });
    }

    private void renderMembership(List<LocalOrder> orders) {
        MembershipTierHelper.Summary summary = MembershipTierHelper.buildSummary(orders, loyaltyPoint);
        tvMembershipTier.setText(summary.tier.label);
        tvMembershipSubtitle.setText(summary.tier.subtitle);
        tvMembershipSpent.setText("Đã chi: " + MoneyFormatter.format(summary.totalSpent));
        tvMembershipOrders.setText("Đơn: " + summary.paidOrderCount);
        tvMembershipPoints.setText("Điểm tích lũy: " + summary.loyaltyPoint);

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{(int) summary.tier.startColor, (int) summary.tier.endColor}
        );
        background.setCornerRadius(dp(30));
        background.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.dashboard_nav_active_bg));
        cardMembership.setBackground(background);
    }

    private void showLoggedOutState() {
        tvProfileTitle.setText("Hồ sơ khách hàng");
        tvEmail.setText("Chưa đăng nhập");
        tvHoTen.setText("Chưa cập nhật");
        tvNgaySinh.setText("Ngày sinh: Chưa cập nhật");
        tvGioiTinh.setText("Giới tính: Chưa cập nhật");
        tvSoDienThoai.setText("Số điện thoại: Chưa cập nhật");
        cardMembership.setVisibility(View.GONE);
        sectionCustomerActions.setVisibility(View.GONE);
    }

    private String resolveRoleTitle(LocalUser user) {
        if ("staff".equalsIgnoreCase(user.getRole())) {
            return "Hồ sơ nhân viên";
        }
        if ("manager".equalsIgnoreCase(user.getRole())) {
            return "Hồ sơ quản lý";
        }
        return "Hồ sơ khách hàng";
    }

    private boolean isCustomerRole(String role) {
        if (role == null) {
            return true;
        }
        String normalized = role.trim().toLowerCase(Locale.US);
        return !"staff".equals(normalized) && !"manager".equals(normalized);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
