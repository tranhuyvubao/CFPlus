package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.graphics.Color;
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

    public static final String EXTRA_STAFF_ACTION = "staff_inventory_action";
    public static final String ACTION_STOCK_IN = "stock_in";
    public static final String ACTION_SHIFT_USAGE = "shift_usage";

    private enum Mode { INGREDIENTS, TRANSACTIONS, SHIFT_USAGE }
    private static final String[] INGREDIENT_UNITS = {
            "túi", "chai", "hộp", "gói", "lon", "cái", "kg", "g", "l", "ml"
    };

    private final List<LocalIngredient> ingredients = new ArrayList<>();
    private final List<LocalStockTransaction> transactions = new ArrayList<>();
    private InventoryCloudRepository inventoryRepository;
    private LocalSessionManager sessionManager;
    private IngredientAdapter ingredientAdapter;
    private TransactionAdapter transactionAdapter;
    private TextView tabIngredients;
    private TextView tabTransactions;
    private TextView tabShiftUsage;
    private TextView btnAdd;
    private TextView btnStockInReport;
    private TextView btnShiftUsageReport;
    private View layoutStockTabs;
    private View staffStockActions;
    private TextView tvEmpty;
    private RecyclerView rv;
    private Mode mode = Mode.INGREDIENTS;
    private String currentRole;
    private String pendingStaffAction;
    private String staffInventoryAction = ACTION_STOCK_IN;
    private ListenerRegistration ingredientsListener;
    private ListenerRegistration transactionsListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_kho);
        InsetsHelper.applyActivityRootPadding(this);

        sessionManager = new LocalSessionManager(this);
        currentRole = sessionManager.getCurrentUserRole();
        pendingStaffAction = getIntent().getStringExtra(EXTRA_STAFF_ACTION);
        if (ACTION_SHIFT_USAGE.equals(pendingStaffAction)) {
            staffInventoryAction = ACTION_SHIFT_USAGE;
        } else if (ACTION_STOCK_IN.equals(pendingStaffAction)) {
            staffInventoryAction = ACTION_STOCK_IN;
        }
        if (!"manager".equals(currentRole) && !"staff".equals(currentRole)) {
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
        tabShiftUsage = findViewById(R.id.tabShiftUsage);
        btnAdd = findViewById(R.id.btnAddKhoFab);
        btnStockInReport = findViewById(R.id.btnStockInReport);
        btnShiftUsageReport = findViewById(R.id.btnShiftUsageReport);
        layoutStockTabs = findViewById(R.id.layoutStockTabs);
        staffStockActions = findViewById(R.id.staffStockActions);
        tvEmpty = findViewById(R.id.tvEmptyKho);
        rv = findViewById(R.id.rvKho);

        if (tabShiftUsage != null) {
            tabShiftUsage.setVisibility(View.GONE);
        }
        btnStockInReport.setVisibility(View.GONE);
        btnShiftUsageReport.setVisibility(View.GONE);

        rv.setLayoutManager(new LinearLayoutManager(this));
        ingredientAdapter = new IngredientAdapter();
        transactionAdapter = new TransactionAdapter();

        tabIngredients.setOnClickListener(v -> switchMode(Mode.INGREDIENTS));
        tabTransactions.setOnClickListener(v -> switchMode(Mode.TRANSACTIONS));
        tabShiftUsage.setOnClickListener(v -> switchMode(Mode.SHIFT_USAGE));
        btnAdd.setOnClickListener(v -> showCreateActionSheet());
        btnStockInReport.setOnClickListener(v -> selectStaffInventoryAction(ACTION_STOCK_IN));
        btnShiftUsageReport.setOnClickListener(v -> selectStaffInventoryAction(ACTION_SHIFT_USAGE));

        configureRoleUi();
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
            maybeApplyPendingStaffAction();
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
            if (mode == Mode.TRANSACTIONS || mode == Mode.SHIFT_USAGE) {
                updateEmptyState();
            }
        });
    }

    private void switchMode(Mode newMode) {
        mode = newMode;
        applyTabHighlight();
        boolean staff = "staff".equals(currentRole);
        if (mode == Mode.INGREDIENTS) {
            tabIngredients.setSelected(true);
            tabTransactions.setSelected(false);
            tabShiftUsage.setSelected(false);
            btnAdd.setText("Thêm nguyên liệu");
            btnAdd.setVisibility(staff ? View.GONE : View.VISIBLE);
            btnAdd.setText("+");
            rv.setAdapter(ingredientAdapter);
        } else {
            if (mode == Mode.SHIFT_USAGE) {
                tabIngredients.setSelected(false);
                tabTransactions.setSelected(false);
                tabShiftUsage.setSelected(true);
                btnAdd.setText("Báo cáo tiêu hao");
                btnAdd.setVisibility(staff ? View.GONE : View.VISIBLE);
                btnAdd.setText("+");
                rv.setAdapter(transactionAdapter);
                transactionAdapter.notifyDataSetChanged();
                updateEmptyState();
                return;
            }
            tabIngredients.setSelected(false);
            tabTransactions.setSelected(true);
            tabShiftUsage.setSelected(false);
            btnAdd.setText("Tạo phiếu");
            btnAdd.setVisibility(staff ? View.GONE : View.VISIBLE);
            btnAdd.setText("+");
            rv.setAdapter(transactionAdapter);
        }
        updateEmptyState();
    }

    private void onAdd() {
        if (mode == Mode.INGREDIENTS) {
            showAddOrEditIngredientDialog(null);
        } else if (mode == Mode.SHIFT_USAGE) {
            showShiftUsageDialog();
        } else {
            showAddTransactionDialog();
        }
    }

    private void updateEmptyState() {
        if (tvEmpty == null) {
            return;
        }
        boolean isEmpty = mode == Mode.INGREDIENTS ? ingredients.isEmpty() : getDisplayedTransactions().isEmpty();
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            if ("staff".equals(currentRole)) {
                tvEmpty.setText(mode == Mode.INGREDIENTS
                        ? "Chưa có nguyên liệu nào. Quản lý cần tạo nguyên liệu trước để nhân viên báo cáo."
                        : "Chưa có báo cáo kho nào.");
                return;
            }
            tvEmpty.setText(mode == Mode.INGREDIENTS
                    ? "Chưa có nguyên liệu nào. Bạn có thể thêm mới ngay ở đây."
                    : "Chưa có phiếu kho nào. Tạo phiếu nhập/xuất để bắt đầu theo dõi.");
        }
    }
    private void configureRoleUi() {
        boolean staff = "staff".equals(currentRole);
        if (layoutStockTabs != null) {
            layoutStockTabs.setVisibility(staff ? View.GONE : View.VISIBLE);
        }
        if (staffStockActions != null) {
            staffStockActions.setVisibility(staff ? View.VISIBLE : View.GONE);
        }
        if (btnAdd != null && staff) {
            btnAdd.setVisibility(View.GONE);
        }
        if (btnStockInReport != null) {
            btnStockInReport.setVisibility(staff ? View.VISIBLE : View.GONE);
        }
        if (btnShiftUsageReport != null) {
            btnShiftUsageReport.setVisibility(staff ? View.VISIBLE : View.GONE);
        }
        applyStaffActionHighlight();
    }

    private void selectStaffInventoryAction(String action) {
        if (!ACTION_SHIFT_USAGE.equals(action)) {
            staffInventoryAction = ACTION_STOCK_IN;
        } else {
            staffInventoryAction = ACTION_SHIFT_USAGE;
        }
        switchMode(Mode.INGREDIENTS);
        applyStaffActionHighlight();
        ingredientAdapter.notifyDataSetChanged();
    }

    private void maybeApplyPendingStaffAction() {
        if (TextUtils.isEmpty(pendingStaffAction)) {
            return;
        }
        String action = pendingStaffAction;
        pendingStaffAction = null;
        selectStaffInventoryAction(action);
    }

    private void applyStaffActionHighlight() {
        if (btnStockInReport == null || btnShiftUsageReport == null) {
            return;
        }
        boolean stockIn = ACTION_STOCK_IN.equals(staffInventoryAction);
        btnStockInReport.setBackgroundResource(stockIn ? R.drawable.manager_primary_pill : R.drawable.manager_back_chip);
        btnShiftUsageReport.setBackgroundResource(stockIn ? R.drawable.manager_back_chip : R.drawable.manager_primary_pill);
        btnStockInReport.setTextColor(stockIn ? Color.WHITE : getColor(R.color.dashboard_primary));
        btnShiftUsageReport.setTextColor(stockIn ? getColor(R.color.dashboard_primary) : Color.WHITE);
    }

    private void showCreateActionSheet() {
        if ("staff".equals(currentRole)) {
            new AlertDialog.Builder(this)
                    .setTitle("Tạo báo cáo kho")
                    .setItems(new String[]{"Nhập hàng", "Báo cáo tiêu hao cuối ca"}, (dialog, which) -> {
                        if (which == 0) {
                            selectStaffInventoryAction(ACTION_STOCK_IN);
                        } else {
                            selectStaffInventoryAction(ACTION_SHIFT_USAGE);
                        }
                    })
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Tạo trong kho")
                .setItems(new String[]{"Thêm nguyên liệu"}, (dialog, which) -> showAddOrEditIngredientDialog(null))
                .show();
    }

    private void applyTabHighlight() {
        if (tabIngredients == null || tabTransactions == null) {
            return;
        }
        boolean ingredientsSelected = mode == Mode.INGREDIENTS;
        boolean transactionsSelected = mode == Mode.TRANSACTIONS;
        boolean shiftUsageSelected = mode == Mode.SHIFT_USAGE;
        tabIngredients.setBackgroundResource(ingredientsSelected ? R.drawable.manager_primary_pill : R.drawable.manager_back_chip);
        tabTransactions.setBackgroundResource(transactionsSelected ? R.drawable.manager_primary_pill : R.drawable.manager_back_chip);
        tabIngredients.setTextColor(ingredientsSelected ? Color.WHITE : getColor(R.color.dashboard_primary));
        tabTransactions.setTextColor(transactionsSelected ? Color.WHITE : getColor(R.color.dashboard_primary));
        if (tabShiftUsage != null) {
            tabShiftUsage.setBackgroundResource(shiftUsageSelected ? R.drawable.manager_primary_pill : R.drawable.manager_back_chip);
            tabShiftUsage.setTextColor(shiftUsageSelected ? Color.WHITE : getColor(R.color.dashboard_primary));
        }
    }

    private int resolveUnitIndex(@Nullable String unit) {
        if (TextUtils.isEmpty(unit)) {
            return 0;
        }
        for (int index = 0; index < INGREDIENT_UNITS.length; index++) {
            if (INGREDIENT_UNITS[index].equalsIgnoreCase(unit.trim())) {
                return index;
            }
        }
        return 0;
    }

    private void showAddOrEditIngredientDialog(@Nullable LocalIngredient ingredient) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_ingredient, null);
        EditText edtName = view.findViewById(R.id.edtIngName);
        Spinner spUnit = view.findViewById(R.id.spIngUnit);
        EditText edtCurrent = view.findViewById(R.id.edtIngCurrentQty);
        EditText edtMin = view.findViewById(R.id.edtIngMinStock);
        SwitchCompat swActive = view.findViewById(R.id.swIngActive);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, INGREDIENT_UNITS);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        if (ingredient != null) {
            edtName.setText(ingredient.getName());
            spUnit.setSelection(resolveUnitIndex(ingredient.getUnit()));
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
                    String unit = String.valueOf(spUnit.getSelectedItem());
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

    private void showStockInDialog() {
        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Chưa có nguyên liệu để báo cáo nhập hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_stock_transaction, null);
        Spinner spIngredient = view.findViewById(R.id.spTxnIngredient);
        Spinner spType = view.findViewById(R.id.spTxnType);
        EditText edtQty = view.findViewById(R.id.edtTxnQty);
        EditText edtNote = view.findViewById(R.id.edtTxnNote);

        List<String> labels = new ArrayList<>();
        for (LocalIngredient ingredient : ingredients) {
            labels.add(ingredient.getName() + " - tồn " + formatQty(ingredient.getCurrentQty()) + " " + ingredient.getUnit());
        }
        ArrayAdapter<String> ingredientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        ingredientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIngredient.setAdapter(ingredientAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Nhập hàng"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);
        spType.setEnabled(false);
        edtQty.setHint("Số lượng nhận được, ví dụ: 3 túi");
        edtNote.setHint("Ghi chú nhà cung cấp / mã hóa đơn nếu có");

        new AlertDialog.Builder(this)
                .setTitle("Báo cáo nhập hàng")
                .setView(view)
                .setPositiveButton("Lưu nhập hàng", (d, which) -> {
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
                    String note = edtNote.getText().toString().trim();
                    if (TextUtils.isEmpty(note)) {
                        note = "Nhân viên báo cáo nhập hàng";
                    }
                    inventoryRepository.createTransaction(
                            ingredient.getIngredientId(),
                            "in",
                            qty,
                            note,
                            sessionManager.getCurrentUserId(),
                            (success, message) -> runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(this, "Đã cộng kho " + formatQty(qty) + " " + ingredient.getUnit(), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, message == null ? "Lỗi lưu nhập hàng." : message, Toast.LENGTH_SHORT).show();
                                }
                            })
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showShiftUsageDialog() {
        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Chưa có nguyên liệu để báo cáo cuối ca", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_stock_transaction, null);
        Spinner spIngredient = view.findViewById(R.id.spTxnIngredient);
        Spinner spType = view.findViewById(R.id.spTxnType);
        EditText edtQty = view.findViewById(R.id.edtTxnQty);
        EditText edtNote = view.findViewById(R.id.edtTxnNote);

        List<String> labels = new ArrayList<>();
        for (LocalIngredient ingredient : ingredients) {
            labels.add(ingredient.getName() + " - tồn " + formatQty(ingredient.getCurrentQty()) + " " + ingredient.getUnit());
        }
        ArrayAdapter<String> ingredientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        ingredientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIngredient.setAdapter(ingredientAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Tiêu hao cuối ca"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);
        spType.setEnabled(false);
        edtQty.setHint("Số lượng đã dùng, ví dụ: 1 túi");
        edtNote.setHint("Ghi chú ca, ví dụ: ca tối dùng pha matcha");

        new AlertDialog.Builder(this)
                .setTitle("Báo cáo tiêu hao cuối ca")
                .setView(view)
                .setPositiveButton("Lưu báo cáo", (d, which) -> {
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
                    String note = edtNote.getText().toString().trim();
                    if (TextUtils.isEmpty(note)) {
                        note = "Báo cáo cuối ca";
                    }
                    inventoryRepository.createTransaction(
                            ingredient.getIngredientId(),
                            "shift_usage",
                            qty,
                            note,
                            sessionManager.getCurrentUserId(),
                            (success, message) -> runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(this, "Đã ghi nhận tiêu hao " + formatQty(qty) + " " + ingredient.getUnit(), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, message == null ? "Lỗi lưu báo cáo cuối ca." : message, Toast.LENGTH_SHORT).show();
                                }
                            })
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showStaffStockReportDialog(@NonNull LocalIngredient ingredient, @NonNull String action) {
        boolean stockIn = ACTION_STOCK_IN.equals(action);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_staff_stock_report, null);
        TextView tvIngredient = view.findViewById(R.id.tvReportIngredient);
        TextView tvCurrentStock = view.findViewById(R.id.tvReportCurrentStock);
        EditText edtQty = view.findViewById(R.id.edtReportQty);
        EditText edtNote = view.findViewById(R.id.edtReportNote);

        tvIngredient.setText(ingredient.getName());
        tvCurrentStock.setText("Tồn hiện tại: " + formatQty(ingredient.getCurrentQty()) + " " + ingredient.getUnit());
        edtQty.setHint(stockIn
                ? "Số lượng nhận được, ví dụ: 3 " + ingredient.getUnit()
                : "Số lượng đã dùng, ví dụ: 2 " + ingredient.getUnit());
        edtNote.setHint(stockIn
                ? "Ghi chú bắt buộc: nhà cung cấp / mã hóa đơn"
                : "Ghi chú bắt buộc: ca làm / lý do tiêu hao");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(stockIn ? "Báo cáo nhập hàng" : "Báo cáo tiêu hao")
                .setView(view)
                .setPositiveButton(stockIn ? "Lưu nhập hàng" : "Lưu báo cáo", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String qtyText = edtQty.getText().toString().trim();
            String note = edtNote.getText().toString().trim();

            if (TextUtils.isEmpty(qtyText)) {
                edtQty.setError("Bắt buộc nhập số lượng");
                edtQty.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(note)) {
                edtNote.setError("Bắt buộc nhập ghi chú");
                edtNote.requestFocus();
                return;
            }

            double qty;
            try {
                qty = Double.parseDouble(qtyText);
            } catch (Exception e) {
                edtQty.setError("Số lượng không hợp lệ");
                edtQty.requestFocus();
                return;
            }
            if (qty <= 0) {
                edtQty.setError("Số lượng phải lớn hơn 0");
                edtQty.requestFocus();
                return;
            }

            inventoryRepository.createTransaction(
                    ingredient.getIngredientId(),
                    stockIn ? "in" : "shift_usage",
                    qty,
                    note,
                    sessionManager.getCurrentUserId(),
                    (success, message) -> runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(
                                    this,
                                    (stockIn ? "Đã cộng kho " : "Đã ghi nhận tiêu hao ")
                                            + formatQty(qty) + " " + ingredient.getUnit(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(
                                    this,
                                    message == null ? "Không thể lưu báo cáo kho." : message,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    })
            );
        }));

        dialog.show();
    }

    private List<LocalStockTransaction> getDisplayedTransactions() {
        if (mode != Mode.SHIFT_USAGE) {
            return transactions;
        }
        List<LocalStockTransaction> usage = new ArrayList<>();
        for (LocalStockTransaction transaction : transactions) {
            if ("shift_usage".equals(transaction.getType())) {
                usage.add(transaction);
            }
        }
        return usage;
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
            tvQty.setText("Tồn: " + formatQty(ingredient.getCurrentQty()) + " " + ingredient.getUnit());
            tvMin.setText("Tối thiểu: " + formatQty(ingredient.getMinStock()) + " " + ingredient.getUnit());
            tvActive.setText(ingredient.isActive() ? "Đang sử dụng" : "Tạm ẩn");

            if ("staff".equals(currentRole)) {
                boolean stockIn = ACTION_STOCK_IN.equals(staffInventoryAction);
                tvActive.setText(stockIn ? "Nhập hàng cho nguyên liệu này" : "Báo cáo tiêu hao nguyên liệu này");
                btnEdit.setText(stockIn ? "Nhập hàng" : "Báo cáo dùng");
                btnEdit.setBackgroundResource(R.drawable.manager_primary_pill);
                btnEdit.setTextColor(Color.WHITE);
                btnEdit.setOnClickListener(v -> showStaffStockReportDialog(ingredient, staffInventoryAction));
                btnDelete.setVisibility(View.GONE);
                return;
            }

            btnDelete.setVisibility(View.VISIBLE);
            btnEdit.setText("Sửa");
            btnEdit.setBackgroundResource(R.drawable.manager_accent_pill);
            btnEdit.setTextColor(getColor(R.color.dashboard_accent));
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
            holder.bind(getDisplayedTransactions().get(position));
        }

        @Override
        public int getItemCount() {
            return getDisplayedTransactions().size();
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
        if ("shift_usage".equals(type)) return "Tiêu hao cuối ca";
        return type == null ? "-" : type;
    }

    private String formatQty(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(millis));
    }
}
