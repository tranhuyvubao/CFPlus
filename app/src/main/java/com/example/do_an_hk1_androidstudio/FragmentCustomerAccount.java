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

import java.util.List;

public class FragmentCustomerAccount extends Fragment {

    private TextView tvName;
    private TextView tvEmail;
    private TextView tvTier;
    private TextView tvTierSubtitle;
    private TextView tvSpent;
    private TextView tvOrders;
    private TextView tvPoints;
    private View cardMembership;

    private UserCloudRepository userRepository;
    private OrderCloudRepository orderRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration ordersListener;
    private int loyaltyPoint = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_account, container, false);

        tvName = view.findViewById(R.id.tvCustomerAccountName);
        tvEmail = view.findViewById(R.id.tvCustomerAccountEmail);
        tvTier = view.findViewById(R.id.tvCustomerTier);
        tvTierSubtitle = view.findViewById(R.id.tvCustomerTierSubtitle);
        tvSpent = view.findViewById(R.id.tvCustomerSpent);
        tvOrders = view.findViewById(R.id.tvCustomerOrders);
        tvPoints = view.findViewById(R.id.tvCustomerPoints);
        cardMembership = view.findViewById(R.id.cardCustomerMembership);

        userRepository = new UserCloudRepository(requireContext());
        orderRepository = new OrderCloudRepository(requireContext());
        sessionManager = new LocalSessionManager(requireContext());

        view.findViewById(R.id.rowCustomerOrders)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), DonHangCuaToiActivity.class)));
        view.findViewById(R.id.rowCustomerFavorites)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), FavoritesActivity.class)));
        view.findViewById(R.id.rowCustomerProfile)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), SuaThongTinCaNhanActivity.class)));
        view.findViewById(R.id.rowCustomerAddresses)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), HoSoKhachHangActivity.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCustomer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
    }

    private void loadCustomer() {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            showLoggedOutState();
            return;
        }

        userRepository.getUserById(userId, (user, message) -> {
            if (!isAdded()) {
                return;
            }
            if (user == null) {
                showLoggedOutState();
                return;
            }
            bindProfile(user);
            bindMembership(user.getUserId());
        });
    }

    private void bindProfile(LocalUser user) {
        tvName.setText(displayOrDefault(user.getFullName()));
        tvEmail.setText(displayOrDefault(user.getEmail()));
    }

    private void bindMembership(String userId) {
        userRepository.getCustomerProfile(userId, (profile, message) -> {
            loyaltyPoint = profile == null ? 0 : profile.getLoyaltyPoint();
            listenOrdersForTier(userId);
        });
    }

    private void listenOrdersForTier(String userId) {
        if (ordersListener != null) {
            ordersListener.remove();
        }
        ordersListener = orderRepository.listenOrdersByCustomer(userId, orders -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> renderMembership(orders));
        });
    }

    private void renderMembership(List<LocalOrder> orders) {
        MembershipTierHelper.Summary summary = MembershipTierHelper.buildSummary(orders, loyaltyPoint);
        tvTier.setText(summary.tier.label);
        tvTierSubtitle.setText(summary.tier.subtitle);
        tvSpent.setText("Đã chi: " + MoneyFormatter.format(summary.totalSpent));
        tvOrders.setText("Đơn: " + summary.paidOrderCount);
        tvPoints.setText("Điểm tích lũy: " + summary.loyaltyPoint);

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{(int) summary.tier.startColor, (int) summary.tier.endColor}
        );
        background.setCornerRadius(dp(30));
        background.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.dashboard_nav_active_bg));
        cardMembership.setBackground(background);
    }

    private void showLoggedOutState() {
        tvName.setText("Chưa đăng nhập");
        tvEmail.setText("Vui lòng đăng nhập lại để xem hồ sơ.");
        tvTier.setText("Đồng");
        tvTierSubtitle.setText("Khởi đầu cho hành trình thưởng thức");
        tvSpent.setText("Đã chi: 0đ");
        tvOrders.setText("Đơn: 0");
        tvPoints.setText("Điểm tích lũy: 0");
    }

    private String displayOrDefault(String value) {
        return value == null || value.trim().isEmpty() ? "Chưa cập nhật" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
