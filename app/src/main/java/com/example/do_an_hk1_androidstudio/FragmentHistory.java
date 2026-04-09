package com.example.do_an_hk1_androidstudio;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;
import com.example.do_an_hk1_androidstudio.local.model.LocalOrderItem;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FragmentHistory extends Fragment {

    private final List<DonHang> listDonHang = new ArrayList<>();
    private DonHangAdapter adapter;
    private OnThemHangClickListener callback;
    private TextView tvEmpty;
    private ListenerRegistration historyListener;

    public interface OnThemHangClickListener {
        void onThemHangClick();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnThemHangClickListener) {
            callback = (OnThemHangClickListener) context;
        } else {
            throw new RuntimeException(context + " phải implement OnThemHangClickListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        RecyclerView rvHistory = view.findViewById(R.id.rvHistory);
        Button btnThemHang = view.findViewById(R.id.button_themhang);
        tvEmpty = view.findViewById(R.id.tvHistoryEmpty);

        adapter = new DonHangAdapter(requireContext(), listDonHang);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(adapter);

        btnThemHang.setOnClickListener(v -> {
            if (callback != null) {
                callback.onThemHangClick();
            }
        });

        listenHistory();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (historyListener != null) {
            historyListener.remove();
        }
    }

    private void listenHistory() {
        if (getContext() == null || adapter == null) {
            return;
        }

        LocalSessionManager sessionManager = new LocalSessionManager(requireContext());
        String userId = sessionManager.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            listDonHang.clear();
            adapter.notifyDataSetChanged();
            updateEmptyState();
            return;
        }

        if (historyListener != null) {
            historyListener.remove();
        }
        historyListener = new OrderCloudRepository(requireContext()).listenOrdersByStaff(userId, orders -> {
            listDonHang.clear();
            for (LocalOrder order : orders) {
                for (LocalOrderItem item : order.getItems()) {
                    DonHang donHang = new DonHang(
                            order.getTableName(),
                            item.getProductName(),
                            item.getVariantName() != null ? item.getVariantName() : "Không xác định",
                            item.getNote(),
                            item.getQty(),
                            item.getLineTotal(),
                            mapOrderType(order.getOrderType(), order.getOrderChannel()),
                            mapStatus(order.getStatus()),
                            item.getImageUrl()
                    );
                    donHang.setOrderId(order.getOrderId());
                    listDonHang.add(donHang);
                }
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }
        });
    }

    private void updateEmptyState() {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(listDonHang.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private String mapOrderType(String orderType, String orderChannel) {
        if ("dine_in".equals(orderType)) {
            return "Tại chỗ (" + mapOrderChannel(orderChannel) + ")";
        }
        if ("takeaway".equals(orderType)) {
            return "Mang về";
        }
        if ("online".equals(orderType)) {
            return "Online";
        }
        return "Tại chỗ";
    }

    private String mapOrderChannel(String orderChannel) {
        if ("customer_qr".equals(orderChannel)) return "QR";
        if ("customer_app".equals(orderChannel)) return "App";
        if ("staff_pos".equals(orderChannel)) return "Nhân viên";
        return "Khác";
    }

    private String mapStatus(String status) {
        if ("paid".equals(status)) return "Đã thanh toán";
        if ("cancelled".equals(status)) return "Đã hủy";
        return "Chưa thanh toán";
    }
}
