package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

import com.example.do_an_hk1_androidstudio.cloud.InventoryCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalIngredient;
import com.example.do_an_hk1_androidstudio.local.model.LocalStockTransaction;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuanLyKhoActivity extends AppCompatActivity {

    private enum Mode { INGREDIENTS, TRANSACTIONS }

    private final List<LocalIngredient> ingredients = new ArrayList<>();
    private final List<LocalStockTransaction> transactions = new ArrayList<>();
    private InventoryCloudRepository inventoryRepository;
    private LocalSessionManager sessionManager;
    private IngredientAdapter ingredientAdapter;
    private TransactionAdapter transactionAdapter;
    private TextView tabIngredients;
    private TextView tabTransactions;
    private TextView btnAdd;
    private TextView tvEmpty;
    private RecyclerView rv;
    private Mode mode = Mode.INGREDIENTS;
    private ListenerRegistration ingredientsListener;
    private ListenerRegistration transactionsListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_kho);
        InsetsHelper.applyActivityRootPadding(this);

        sessionManager = new LocalSessionManager(this);
        if (!"manager".equals(sessionManager.getCurrentUserRole())) {
            Toast.makeText(this, "Chỉ quản lý mới truy cập được", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inventoryRepository = new InventoryCloudRepository(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        tabIngredients = findViewById(R.id.tabIngredients);
        tabTransactions = findViewById(R.id.tabTransactions);
        btnAdd = findViewById(R.id.btnAddKho);
        tvEmpty = findViewById(R.id.tvEmptyKho);
        rv = findViewById(R.id.rvKho);

        rv.setLayoutManager(new LinearLayoutManager(this));
        ingredientAdapter = new IngredientAdapter();
        transactionAdapter = new TransactionAdapter();

        tabIngredients.setOnClickListener(v -> switchMode(Mode.INGREDIENTS));
        tabTransactions.setOnClickListener(v -> switchMode(Mode.TRANSACTIONS));
        btnAdd.setOnClickListener(v -> onAdd());

        listenIngredients();
        listenTransactions();
        switchMode(Mode.INGREDIENTS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ingredientsListener != null) ingredientsListener.remove();
        if (transactionsListener != null) transactionsListener.remove();
    }

    private void listenIngredients() {
        if (ingredientsListener != null) ingredientsListener.remove();
        ingredientsListener = inventoryRepository.listenIngredients(fetched -> {
            ingredients.clear();
            ingredients.addAll(fetched);
            ingredientAdapter.notifyDataSetChanged();
            if (mode == Mode.INGREDIENTS) {
                updateEmptyState();
            }
        });
    }

    private void listenTransactions() {
        if (transactionsListener != null) transactionsListener.remove();
        transactionsListener = inventoryRepository.listenRecentTransactions(50, fetched -> {
            transactions.clear();
            transactions.addAll(fetched);
            transactionAdapter.notifyDataSetChanged();
            if (mode == Mode.TRANSACTIONS) {
                updateEmptyState();
            }
        });
    }

    private void switchMode(Mode newMode) {
        mode = newMode;
        if (mode == Mode.INGREDIENTS) {
            tabIngredients.setSelected(true);
            tabTransactions.setSelected(false);
            btnAdd.setText("Thêm nguyên liệu");
            rv.setAdapter(ingredientAdapter);
        } else {
            tabIngredients.setSelected(false);
            tabTransactions.setSelected(true);
            btnAdd.setText("Tạo phiếu");
            rv.setAdapter(transactionAdapter);
        }
        updateEmptyState();
    }

    private void onAdd() {
        if (mode == Mode.INGREDIENTS) {
            showAddOrEditIngredientDialog(null);
        } else {
            showAddTransactionDialog();
        }
    }

    private void updateEmptyState() {
        if (tvEmpty == null) {
            return;
        }
        boolean isEmpty = mode == Mode.INGREDIENTS ? ingredients.isEmpty() : transactions.isEmpty();
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            tvEmpty.setText(mode == Mode.INGREDIENTS
                    ? "Chưa có nguyên liệu nào. Bạn có thể thêm mới ngay ở đây."
                    : "Chưa có phiếu kho nào. Tạo phiếu nhập/xuất để bắt đầu theo dõi.");
        }
    }

    private void showAddOrEditIngredientDialog(@Nullable LocalIngredient ingredient) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_ingredient, null);
        EditText edtName = view.findViewById(R.id.edtIngName);
        EditText edtUnit = view.findViewById(R.id.edtIngUnit);
        EditText edtCurrent = view.findViewById(R.id.edtIngCurrentQty);
        EditText edtMin = view.findViewById(R.id.edtIngMinStock);
        SwitchCompat swActive = view.findViewById(R.id.swIngActive);

        if (ingredient != null) {
            edtName.setText(ingredient.getName());
            edtUnit.setText(ingredient.getUnit());
            edtCurrent.setText(String.valueOf(ingredient.getCurrentQty()));
            edtMin.setText(String.valueOf(ingredient.getMinStock()));
            swActive.setChecked(ingredient.isActive());
        } else {
            swActive.setChecked(true);
            edtCurrent.setText("0");
            edtMin.setText("0");
        }

        new AlertDialog.Builder(this)
                .setTitle(ingredient == null ? "Thêm nguyên liệu" : "Sửa nguyên liệu")
                .setView(view)
                .setPositiveButton("Lưu", (d, which) -> {
                    String name = edtName.getText().toString().trim();
                    String unit = edtUnit.getText().toString().trim();
                    String currentStr = edtCurrent.getText().toString().trim();
                    String minStr = edtMin.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(unit)) {
                        Toast.makeText(this, "Vui lòng nhập tên và đơn vị", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double currentQty;
                    double minStock;
                    try {
                        currentQty = Double.parseDouble(currentStr);
                        minStock = Double.parseDouble(minStr);
                    } catch (Exception e) {
                        Toast.makeText(this, "Số lượng không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    inventoryRepository.saveIngredient(
                            ingredient == null ? null : ingredient.getIngredientId(),
                            name,
                            unit,
                            currentQty,
                            minStock,
                            swActive.isChecked(),
                            (success, message) -> runOnUiThread(() -> {
                                if (!success) {
                                    Toast.makeText(this, message == null ? "Không thể lưu nguyên liệu." : message, Toast.LENGTH_SHORT).show();
                                }
                            })
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddTransactionDialog() {
        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Chưa có nguyên liệu để tạo phiếu", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_stock_transaction, null);
        Spinner spIngredient = view.findViewById(R.id.spTxnIngredient);
        Spinner spType = view.findViewById(R.id.spTxnType);
        EditText edtQty = view.findViewById(R.id.edtTxnQty);
        EditText edtNote = view.findViewById(R.id.edtTxnNote);

        List<String> labels = new ArrayList<>();
        for (LocalIngredient ingredient : ingredients) {
            labels.add(ingredient.getName() + " (" + ingredient.getIngredientId() + ")");
        }
        ArrayAdapter<String> ingredientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        ingredientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIngredient.setAdapter(ingredientAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"in", "out", "adjust"}
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Tạo phiếu kho")
                .setView(view)
                .setPositiveButton("Lưu", (d, which) -> {
                    int ingredientPosition = spIngredient.getSelectedItemPosition();
                    if (ingredientPosition < 0 || ingredientPosition >= ingredients.size()) {
                        return;
                    }

                    double qty;
                    try {
                        qty = Double.parseDouble(edtQty.getText().toString().trim());
                    } catch (Exception e) {
                        Toast.makeText(this, "Số lượng không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (qty <= 0) {
                        Toast.makeText(this, "Số lượng phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LocalIngredient ingredient = ingredients.get(ingredientPosition);
                    inventoryRepository.createTransaction(
                            ingredient.getIngredientId(),
                            String.valueOf(spType.getSelectedItem()),
                            qty,
                            edtNote.getText().toString().trim(),
                            sessionManager.getCurrentUserId(),
                            (success, message) -> runOnUiThread(() -> {
                                if (!success) {
                                    Toast.makeText(this, message == null ? "Lỗi tạo phiếu kho." : message, Toast.LENGTH_SHORT).show();
                                }
                            })
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private class IngredientAdapter extends RecyclerView.Adapter<IngredientVH> {
        @NonNull
        @Override
        public IngredientVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient, parent, false);
            return new IngredientVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IngredientVH holder, int position) {
            holder.bind(ingredients.get(position));
        }

        @Override
        public int getItemCount() {
            return ingredients.size();
        }
    }

    private class IngredientVH extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvUnit;
        private final TextView tvQty;
        private final TextView tvMin;
        private final TextView tvActive;
        private final TextView btnEdit;
        private final TextView btnDelete;

        IngredientVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvIngName);
            tvUnit = itemView.findViewById(R.id.tvIngUnit);
            tvQty = itemView.findViewById(R.id.tvIngQty);
            tvMin = itemView.findViewById(R.id.tvIngMin);
            tvActive = itemView.findViewById(R.id.tvIngActive);
            btnEdit = itemView.findViewById(R.id.btnIngEdit);
            btnDelete = itemView.findViewById(R.id.btnIngDelete);
        }

        void bind(LocalIngredient ingredient) {
            tvName.setText(ingredient.getName());
            tvUnit.setText("Đơn vị: " + ingredient.getUnit());
            tvQty.setText("Tồn: " + ingredient.getCurrentQty());
            tvMin.setText("Tối thiểu: " + ingredient.getMinStock());
            tvActive.setText(ingredient.isActive() ? "Đang sử dụng" : "Tạm ẩn");

            btnEdit.setOnClickListener(v -> showAddOrEditIngredientDialog(ingredient));
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(QuanLyKhoActivity.this)
                    .setTitle("Xóa nguyên liệu")
                    .setMessage("Bạn có chắc muốn xóa \"" + ingredient.getName() + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> inventoryRepository.deleteIngredient(
                            ingredient.getIngredientId(),
                            (success, message) -> runOnUiThread(() -> {
                                if (!success) {
                                    Toast.makeText(QuanLyKhoActivity.this, message == null ? "Lỗi xóa nguyên liệu." : message, Toast.LENGTH_SHORT).show();
                                }
                            })
                    ))
                    .setNegativeButton("Hủy", null)
                    .show());
        }
    }

    private class TransactionAdapter extends RecyclerView.Adapter<TransactionVH> {
        @NonNull
        @Override
        public TransactionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_transaction, parent, false);
            return new TransactionVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TransactionVH holder, int position) {
            holder.bind(transactions.get(position));
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }
    }

    private class TransactionVH extends RecyclerView.ViewHolder {
        private final TextView tvType;
        private final TextView tvIngId;
        private final TextView tvQty;
        private final TextView tvTime;
        private final TextView tvNote;

        TransactionVH(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvTxnType);
            tvIngId = itemView.findViewById(R.id.tvTxnIngredient);
            tvQty = itemView.findViewById(R.id.tvTxnQty);
            tvTime = itemView.findViewById(R.id.tvTxnTime);
            tvNote = itemView.findViewById(R.id.tvTxnNote);
        }

        void bind(LocalStockTransaction transaction) {
            tvType.setText("Loại: " + mapTransactionType(transaction.getType()));
            tvIngId.setText("Nguyên liệu: " + transaction.getIngredientId());
            tvQty.setText("Số lượng: " + transaction.getQty());
            tvTime.setText("Thời gian: " + formatDate(transaction.getCreatedAtMillis()));
            tvNote.setText("Ghi chú: " + (transaction.getNote() == null ? "-" : transaction.getNote()));
        }
    }

    private String mapTransactionType(String type) {
        if ("in".equals(type)) return "Nhập kho";
        if ("out".equals(type)) return "Xuất kho";
        if ("adjust".equals(type)) return "Điều chỉnh";
        return type == null ? "-" : type;
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(millis));
    }
}
