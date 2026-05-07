package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalCafeTable;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FragmentStaffTables extends Fragment {

    private final List<LocalCafeTable> tables = new ArrayList<>();
    private final Set<String> tablesWithOpenOrders = new HashSet<>();

    private ListenerRegistration tableListener;
    private ListenerRegistration orderListener;
    private TableCloudRepository tableRepository;
    private OrderCloudRepository orderRepository;
    private TableGridAdapter adapter;
    private TextView tvEmpty;
    private TextView btnTakeaway;
    private int takeawayOpenOrderCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_tables, container, false);
        tableRepository = new TableCloudRepository(requireContext());
        orderRepository = new OrderCloudRepository(requireContext());

        RecyclerView rvTables = view.findViewById(R.id.rvStaffTables);
        tvEmpty = view.findViewById(R.id.tvStaffTablesEmpty);
        View btnReservations = view.findViewById(R.id.btnStaffReservations);
        View btnBarMode = view.findViewById(R.id.btnStaffBarMode);
        View btnSupportChat = view.findViewById(R.id.btnStaffSupportChat);
        btnTakeaway = view.findViewById(R.id.btnStaffTakeaway);

        rvTables.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new TableGridAdapter();
        rvTables.setAdapter(adapter);

        btnReservations.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), XuLyDatBanActivity.class)));
        btnTakeaway.setOnClickListener(v -> openTakeawayBill());
        btnBarMode.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), KdsBarActivity.class)));
        btnSupportChat.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), StaffSupportThreadsActivity.class)));
        renderTakeawayButton();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadTables();
        loadOpenOrders();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (tableListener != null) {
            tableListener.remove();
            tableListener = null;
        }
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }

    private void loadTables() {
        if (tableListener != null) {
            tableListener.remove();
        }
        tableListener = tableRepository.listenTables(fetchedTables -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                tables.clear();
                for (LocalCafeTable table : fetchedTables) {
                    if (table.isActive() && !TableCloudRepository.TAKEAWAY_TABLE_ID.equals(table.getTableId())) {
                        tables.add(table);
                    }
                }
                sortTables();
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(tables.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void loadOpenOrders() {
        if (orderListener != null) {
            orderListener.remove();
        }
        orderListener = orderRepository.listenAllOrders(fetchedOrders -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                tablesWithOpenOrders.clear();
                takeawayOpenOrderCount = 0;
                fetchedOrders.forEach(order -> {
                    String status = order.getStatus();
                    if (("created".equals(status) || "confirmed".equals(status)) && order.getTableId() != null) {
                        if (TableCloudRepository.TAKEAWAY_TABLE_ID.equals(order.getTableId())) {
                            takeawayOpenOrderCount++;
                        } else {
                            tablesWithOpenOrders.add(order.getTableId());
                        }
                    }
                });
                renderTakeawayButton();
                sortTables();
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void openTakeawayBill() {
        Intent intent = new Intent(requireContext(), HoaDonBanActivity.class);
        intent.putExtra(HoaDonBanActivity.EXTRA_TABLE_ID, TableCloudRepository.TAKEAWAY_TABLE_ID);
        intent.putExtra(HoaDonBanActivity.EXTRA_TABLE_NAME, "Mang về");
        startActivity(intent);
    }

    private void renderTakeawayButton() {
        if (btnTakeaway == null) {
            return;
        }
        btnTakeaway.setText(takeawayOpenOrderCount > 0
                ? "Mang về (" + takeawayOpenOrderCount + ")"
                : "Mang về");
    }

    private void sortTables() {
        tables.sort(Comparator
                .comparingInt(this::getPriority)
                .thenComparing(LocalCafeTable::getName, String.CASE_INSENSITIVE_ORDER));
    }

    private int getPriority(LocalCafeTable table) {
        if (tablesWithOpenOrders.contains(table.getTableId())) {
            return 0;
        }
        if ("occupied".equals(table.getStatus())) {
            return 1;
        }
        if ("reserved".equals(table.getStatus())) {
            return 2;
        }
        return 3;
    }

    private String mapStatus(String status) {
        if ("occupied".equals(status)) {
            return "Đang dùng";
        }
        if ("reserved".equals(status)) {
            return "Đã đặt";
        }
        return "Trống";
    }

    private int getStatusBackgroundRes(String displayStatus) {
        switch (displayStatus) {
            case "Có khách":
            case "Đang dùng":
                return R.drawable.app_chip_status_occupied;
            case "Đã đặt":
                return R.drawable.app_chip_status_reserved;
            default:
                return R.drawable.app_chip_status_empty;
        }
    }

    private int getStatusTextColor(String displayStatus) {
        switch (displayStatus) {
            case "Có khách":
            case "Đang dùng":
                return R.color.dashboard_success;
            case "Đã đặt":
                return R.color.dashboard_warning;
            default:
                return R.color.dashboard_primary;
        }
    }

    private class TableGridAdapter extends RecyclerView.Adapter<TableGridViewHolder> {
        @NonNull
        @Override
        public TableGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff_table, parent, false);
            return new TableGridViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TableGridViewHolder holder, int position) {
            holder.bind(tables.get(position));
        }

        @Override
        public int getItemCount() {
            return tables.size();
        }
    }

    private class TableGridViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvStatus;
        private final TextView tvArea;

        TableGridViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStaffTableName);
            tvStatus = itemView.findViewById(R.id.tvStaffTableStatus);
            tvArea = itemView.findViewById(R.id.tvStaffTableArea);
        }

        void bind(LocalCafeTable table) {
            String displayStatus = tablesWithOpenOrders.contains(table.getTableId()) ? "Có khách" : mapStatus(table.getStatus());
            Drawable background = ContextCompat.getDrawable(requireContext(), getStatusBackgroundRes(displayStatus));

            tvName.setText(table.getName());
            tvStatus.setText(displayStatus);
            tvStatus.setBackground(background);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), getStatusTextColor(displayStatus)));
            tvArea.setText(TextUtils.isEmpty(table.getArea()) ? table.getCode() : table.getArea());

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HoaDonBanActivity.class);
                intent.putExtra(HoaDonBanActivity.EXTRA_TABLE_ID, table.getTableId());
                intent.putExtra(HoaDonBanActivity.EXTRA_TABLE_NAME, table.getName());
                startActivity(intent);
            });
        }
    }
}
