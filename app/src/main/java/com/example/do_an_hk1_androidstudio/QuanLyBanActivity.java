package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalCafeTable;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class QuanLyBanActivity extends AppCompatActivity {

    private final List<LocalCafeTable> tables = new ArrayList<>();
    private TableAdapter adapter;
    private TableCloudRepository tableRepository;
    private LocalSessionManager localSessionManager;
    private ListenerRegistration tableListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_ban);
        InsetsHelper.applyActivityRootPadding(this);

        localSessionManager = new LocalSessionManager(this);
        tableRepository = new TableCloudRepository(this);

        if (!"manager".equals(localSessionManager.getCurrentUserRole())) {
            Toast.makeText(this, "Chỉ quản lý mới truy cập được", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        RecyclerView rvTables = findViewById(R.id.rvTables);
        TextView btnAdd = findViewById(R.id.btnAddTable);

        rvTables.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TableAdapter();
        rvTables.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showAddOrEditDialog(null));
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadTables();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tableListener != null) {
            tableListener.remove();
            tableListener = null;
        }
    }

    private void loadTables() {
        if (tableListener != null) {
            tableListener.remove();
        }
        tableListener = tableRepository.listenTables(fetchedTables -> runOnUiThread(() -> {
            tables.clear();
            tables.addAll(fetchedTables);
            adapter.notifyDataSetChanged();
        }));
    }

    private void showAddOrEditDialog(@Nullable LocalCafeTable table) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_table, null);
        EditText edtName = view.findViewById(R.id.edtTableName);
        EditText edtCode = view.findViewById(R.id.edtTableCode);
        TextView tvCodeLabel = view.findViewById(R.id.tvTableCodeLabel);
        EditText edtArea = view.findViewById(R.id.edtTableArea);
        TextView tvStatusLabel = view.findViewById(R.id.tvTableStatusLabel);
        Spinner spStatus = view.findViewById(R.id.spTableStatus);
        SwitchCompat swActive = view.findViewById(R.id.swTableActive);
        boolean isEditMode = table != null;
        boolean isTakeawayTable = table != null && TableCloudRepository.TAKEAWAY_TABLE_ID.equals(table.getTableId());

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Trống", "Đang dùng", "Đã đặt"}
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(statusAdapter);

        if (isEditMode) {
            edtName.setText(table.getName());
            edtCode.setText(table.getCode());
            edtArea.setText(table.getArea());
            int idx = 0;
            if ("occupied".equals(table.getStatus())) {
                idx = 1;
            } else if ("reserved".equals(table.getStatus())) {
                idx = 2;
            }
            spStatus.setSelection(idx);
            swActive.setChecked(table.isActive());
        } else {
            edtCode.setText("Tự động sinh khi lưu");
            swActive.setChecked(true);
            spStatus.setSelection(0);
        }

        edtCode.setEnabled(false);
        edtCode.setFocusable(false);
        edtCode.setClickable(false);

        int editOnlyVisibility = isEditMode ? View.VISIBLE : View.GONE;
        tvCodeLabel.setVisibility(editOnlyVisibility);
        edtCode.setVisibility(editOnlyVisibility);
        tvStatusLabel.setVisibility(editOnlyVisibility);
        spStatus.setVisibility(editOnlyVisibility);
        swActive.setVisibility(editOnlyVisibility);

        if (isTakeawayTable) {
            edtName.setEnabled(false);
            edtArea.setEnabled(false);
            spStatus.setEnabled(false);
            swActive.setEnabled(false);
        }

        String title = isEditMode ? "Sửa bàn" : "Thêm bàn";
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton == null) {
                return;
            }
            if (isTakeawayTable) {
                positiveButton.setEnabled(false);
                return;
            }
            positiveButton.setOnClickListener(v -> {
                String name = edtName.getText().toString().trim();
                String area = edtArea.getText().toString().trim();
                String status = mapStatusValue(spStatus.getSelectedItemPosition());

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "Tên bàn không được trống", Toast.LENGTH_SHORT).show();
                    return;
                }

                tableRepository.saveTable(
                        table == null ? null : table.getTableId(),
                        name,
                        isEditMode ? table.getCode() : null,
                        area,
                        isEditMode ? status : "free",
                        !isEditMode || swActive.isChecked(),
                        (success, message) -> runOnUiThread(() -> {
                            if (!success) {
                                Toast.makeText(this, message == null ? "Không thể lưu bàn" : message, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            dialog.dismiss();
                        })
                );
            });
        });
        dialog.show();
    }

    private String mapStatusValue(int position) {
        if (position == 1) {
            return "occupied";
        }
        if (position == 2) {
            return "reserved";
        }
        return "free";
    }

    private String mapStatusLabel(String status) {
        if ("occupied".equals(status)) {
            return "Đang dùng";
        }
        if ("reserved".equals(status)) {
            return "Đã đặt";
        }
        return "Trống";
    }

    private class TableAdapter extends RecyclerView.Adapter<TableVH> {
        @NonNull
        @Override
        public TableVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table, parent, false);
            return new TableVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TableVH holder, int position) {
            holder.bind(tables.get(position));
        }

        @Override
        public int getItemCount() {
            return tables.size();
        }
    }

    private class TableVH extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvCode;
        private final TextView tvArea;
        private final TextView tvStatus;
        private final TextView tvActive;
        private final TextView btnQr;
        private final TextView btnEdit;
        private final TextView btnDelete;

        TableVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTableName);
            tvCode = itemView.findViewById(R.id.tvTableCode);
            tvArea = itemView.findViewById(R.id.tvTableArea);
            tvStatus = itemView.findViewById(R.id.tvTableStatus);
            tvActive = itemView.findViewById(R.id.tvTableActive);
            btnQr = itemView.findViewById(R.id.btnTableQr);
            btnEdit = itemView.findViewById(R.id.btnTableEdit);
            btnDelete = itemView.findViewById(R.id.btnTableDelete);
        }

        void bind(LocalCafeTable table) {
            tvName.setText(table.getName());
            tvCode.setText("Mã bàn: " + table.getCode());
            tvArea.setText("Khu vực: " + (TextUtils.isEmpty(table.getArea()) ? "-" : table.getArea()));
            tvStatus.setText("Trạng thái: " + mapStatusLabel(table.getStatus()));
            tvActive.setText(table.isActive() ? "Đang sử dụng" : "Tạm ẩn");
            boolean isTakeawayTable = TableCloudRepository.TAKEAWAY_TABLE_ID.equals(table.getTableId());

            btnQr.setOnClickListener(v -> {
                Intent intent = new Intent(QuanLyBanActivity.this, TableQrActivity.class);
                intent.putExtra(TableQrActivity.EXTRA_TABLE_NAME, table.getName());
                intent.putExtra(TableQrActivity.EXTRA_TABLE_CODE, table.getCode());
                startActivity(intent);
            });
            btnEdit.setEnabled(!isTakeawayTable);
            btnDelete.setEnabled(!isTakeawayTable);
            btnEdit.setAlpha(isTakeawayTable ? 0.4f : 1f);
            btnDelete.setAlpha(isTakeawayTable ? 0.4f : 1f);
            btnEdit.setOnClickListener(v -> {
                if (isTakeawayTable) {
                    Toast.makeText(QuanLyBanActivity.this, "Bàn Take away không được sửa", Toast.LENGTH_SHORT).show();
                    return;
                }
                showAddOrEditDialog(table);
            });
            btnDelete.setOnClickListener(v -> {
                if (isTakeawayTable) {
                    Toast.makeText(QuanLyBanActivity.this, "Bàn Take away không được xóa", Toast.LENGTH_SHORT).show();
                    return;
                }
                tableRepository.deleteTable(
                        table.getTableId(),
                        (success, message) -> runOnUiThread(() -> {
                            if (!success) {
                                Toast.makeText(QuanLyBanActivity.this, message == null ? "Lỗi xóa bàn" : message, Toast.LENGTH_SHORT).show();
                            }
                        })
                );
            });
        }
    }
}
