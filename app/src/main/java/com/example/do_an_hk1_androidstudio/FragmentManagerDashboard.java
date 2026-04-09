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

import com.example.do_an_hk1_androidstudio.cloud.InventoryCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.PromotionCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalCafeTable;
import com.example.do_an_hk1_androidstudio.local.model.LocalIngredient;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FragmentManagerDashboard extends Fragment {

    private final List<LocalOrder> allOrders = new ArrayList<>();
    private final List<LocalIngredient> allIngredients = new ArrayList<>();
    private int activeTables = 0;
    private int staffCount = 0;
    private int promotionCount = 0;

    private ListenerRegistration ordersListener;
    private ListenerRegistration tablesListener;
    private ListenerRegistration staffListener;
    private ListenerRegistration promotionsListener;
    private ListenerRegistration ingredientsListener;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_manager_dashboard, container, false);
        bindActions(rootView);
        bindSummary(rootView);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeListeners();
        rootView = null;
    }

    private void bindSummary(View view) {
        removeListeners();

        OrderCloudRepository orderRepository = new OrderCloudRepository(requireContext());
        TableCloudRepository tableRepository = new TableCloudRepository(requireContext());
        UserCloudRepository userRepository = new UserCloudRepository(requireContext());
        PromotionCloudRepository promotionRepository = new PromotionCloudRepository(requireContext());
        InventoryCloudRepository inventoryRepository = new InventoryCloudRepository(requireContext());

        ordersListener = orderRepository.listenAllOrders(orders -> {
            allOrders.clear();
            allOrders.addAll(orders);
            renderSummary();
        });
        tablesListener = tableRepository.listenTables(tables -> {
            activeTables = 0;
            for (LocalCafeTable table : tables) {
                if (table.isActive()) {
                    activeTables++;
                }
            }
            renderSummary();
        });
        staffListener = userRepository.listenUsersByRole("staff", users -> {
            staffCount = users.size();
            renderSummary();
        });
        promotionsListener = promotionRepository.listenPromotions(promotions -> {
            promotionCount = promotions.size();
            renderSummary();
        });
        ingredientsListener = inventoryRepository.listenIngredients(ingredients -> {
            allIngredients.clear();
            allIngredients.addAll(ingredients);
            renderSummary();
        });
    }

    private void renderSummary() {
        if (rootView == null) {
            return;
        }
        int paidToday = countPaidToday(allOrders);
        int lowStockCount = countLowStockIngredients(allIngredients);

        ((TextView) rootView.findViewById(R.id.tvManagerMetricRevenue)).setText(String.valueOf(paidToday));
        ((TextView) rootView.findViewById(R.id.tvManagerMetricTables)).setText(String.valueOf(activeTables));
        ((TextView) rootView.findViewById(R.id.tvManagerMetricStaff)).setText(String.valueOf(staffCount));
        ((TextView) rootView.findViewById(R.id.tvManagerMetricPromo)).setText(String.valueOf(promotionCount));
        ((TextView) rootView.findViewById(R.id.tvManagerMetricStock)).setText(String.valueOf(lowStockCount));
    }

    private int countPaidToday(List<LocalOrder> orders) {
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        long from = startOfDay.getTimeInMillis();

        int count = 0;
        for (LocalOrder order : orders) {
            if ("paid".equals(order.getStatus()) && order.getCreatedAtMillis() >= from) {
                count++;
            }
        }
        return count;
    }

    private int countLowStockIngredients(List<LocalIngredient> ingredients) {
        int count = 0;
        for (LocalIngredient ingredient : ingredients) {
            if (ingredient.isActive() && ingredient.getCurrentQty() <= ingredient.getMinStock()) {
                count++;
            }
        }
        return count;
    }

    private void bindActions(View view) {
        setAction(view, R.id.cardThongKeHomNay, ThongKeActivity.class);
        setAction(view, R.id.cardThongKeTongHop, ThongKeTongHopActivity.class);
        setAction(view, R.id.cardQuanLyMon, QuanLyMonActivity.class);
        setAction(view, R.id.cardQuanLyDanhMuc, QuanLyDanhMucActivity.class);
        setAction(view, R.id.cardQuanLyBan, QuanLyBanActivity.class);
        setAction(view, R.id.cardQuanLyKho, QuanLyKhoActivity.class);
        setAction(view, R.id.cardQuanLyKhuyenMai, QuanLyKhuyenMaiActivity.class);
        setAction(view, R.id.cardQuanLyNhanVien, QuanLyNhanVienActivity.class);
        setAction(view, R.id.cardQuanLyKhachHang, QuanLyKhachHangActivity.class);
        setAction(view, R.id.cardSeedDuLieu, FirebaseSeederActivity.class);
    }

    private void setAction(View root, int viewId, Class<?> activityClass) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setOnClickListener(v -> startActivity(new Intent(requireContext(), activityClass)));
        }
    }

    private void removeListeners() {
        if (ordersListener != null) ordersListener.remove();
        if (tablesListener != null) tablesListener.remove();
        if (staffListener != null) staffListener.remove();
        if (promotionsListener != null) promotionsListener.remove();
        if (ingredientsListener != null) ingredientsListener.remove();
    }
}
