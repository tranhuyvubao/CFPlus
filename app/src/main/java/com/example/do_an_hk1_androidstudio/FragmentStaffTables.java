package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.OrderCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.model.LocalCafeTable;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FragmentStaffTables extends Fragment {

    private final List<LocalCafeTable> tables = new ArrayList<>();
    private ListenerRegistration tableListener;
    private ListenerRegistration orderListener;
    private TableCloudRepository tableRepository;
    private OrderCloudRepository orderRepository;
    private TableGridAdapter adapter;
    private TextView tvEmpty;
    private final Set<String> tablesWithOpenOrders = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_tables, container, false);
        tableRepository = new TableCloudRepository(requireContext());
        orderRepository = new OrderCloudRepository(requireContext());

        RecyclerView rvTables = view.findViewById(R.id.rvStaffTables);
        tvEmpty = view.findViewById(R.id.tvStaffTablesEmpty);

        rvTables.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new TableGridAdapter();
        rvTables.setAdapter(adapter);
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
                    if (table.isActive()) {
                        tables.add(table);
                    }
                }
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(tables.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void loadOpenOrders() {
        if (orderListener != null) {
            orderListener.remove();
        }
        orderListener = orderRepository.listenOnlineOrders(fetchedOrders -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                tablesWithOpenOrders.clear();
                fetchedOrders.forEach(order -> {
                    String status = order.getStatus();
                    if (("created".equals(status) || "confirmed".equals(status)) && order.getTableId() != null) {
                        tablesWithOpenOrders.add(order.getTableId());
                    }
                });
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        });
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
            tvName.setText(table.getName());
            if (tablesWithOpenOrders.contains(table.getTableId())) {
                tvStatus.setText("Có khách");
            } else {
                tvStatus.setText(mapStatus(table.getStatus()));
            }
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
