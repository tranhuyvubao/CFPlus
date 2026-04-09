package com.example.do_an_hk1_androidstudio;

import android.app.DatePickerDialog;
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

import com.example.do_an_hk1_androidstudio.cloud.PromotionCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalPromotion;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuanLyKhuyenMaiActivity extends AppCompatActivity {

    private final List<LocalPromotion> promos = new ArrayList<>();
    private PromoAdapter adapter;
    private PromotionCloudRepository promotionRepository;
    private TextView tvEmpty;
    private ListenerRegistration promosListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_khuyen_mai);
        InsetsHelper.applyActivityRootPadding(this);

        LocalSessionManager sessionManager = new LocalSessionManager(this);
        if (!"manager".equals(sessionManager.getCurrentUserRole())) {
            Toast.makeText(this, "Chỉ quản lý mới truy cập được", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        promotionRepository = new PromotionCloudRepository(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        RecyclerView rvPromos = findViewById(R.id.rvPromos);
        TextView btnAddPromo = findViewById(R.id.btnAddPromo);
        tvEmpty = findViewById(R.id.tvEmptyPromos);

        rvPromos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PromoAdapter();
        rvPromos.setAdapter(adapter);

        btnAddPromo.setOnClickListener(v -> showAddOrEditDialog(null));
        listenPromotions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (promosListener != null) {
            promosListener.remove();
        }
    }

    private void listenPromotions() {
        if (promosListener != null) {
            promosListener.remove();
        }
        promosListener = promotionRepository.listenPromotions(fetched -> {
            promos.clear();
            promos.addAll(fetched);
            adapter.notifyDataSetChanged();
            if (tvEmpty != null) {
                tvEmpty.setVisibility(promos.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private static String fmtDate(@Nullable Long millis) {
        if (millis == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }

    private void showAddOrEditDialog(@Nullable LocalPromotion promo) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_promotion, null);
        EditText edtCode = view.findViewById(R.id.edtPromoCode);
        Spinner spType = view.findViewById(R.id.spPromoType);
        EditText edtValue = view.findViewById(R.id.edtPromoValue);
        EditText edtMinOrder = view.findViewById(R.id.edtPromoMinOrder);
        EditText edtMaxDiscount = view.findViewById(R.id.edtPromoMaxDiscount);
        View layoutDateRange = view.findViewById(R.id.layoutPromoDateRange);
        EditText edtStart = view.findViewById(R.id.edtPromoStart);
        EditText edtEnd = view.findViewById(R.id.edtPromoEnd);
        SwitchCompat swHasLimit = view.findViewById(R.id.swPromoHasLimit);
        SwitchCompat swActive = view.findViewById(R.id.swPromoActive);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Phần trăm", "Số tiền cố định"}
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        bindDatePicker(edtStart);
        bindDatePicker(edtEnd);

        if (promo != null) {
            edtCode.setText(promo.getCode());
            spType.setSelection("fixed".equals(promo.getType()) ? 1 : 0);
            edtValue.setText(String.valueOf((int) promo.getValue()));
            edtMinOrder.setText(String.valueOf(promo.getMinOrder()));
            edtMaxDiscount.setText(promo.getMaxDiscount() != null ? String.valueOf(promo.getMaxDiscount()) : "");
            edtStart.setText(fmtDate(promo.getStartDateMillis()));
            edtEnd.setText(fmtDate(promo.getEndDateMillis()));
            swHasLimit.setChecked(promo.getStartDateMillis() != null || promo.getEndDateMillis() != null);
            swActive.setChecked(promo.isActive());
        } else {
            swHasLimit.setChecked(false);
            swActive.setChecked(true);
            edtMinOrder.setText("0");
        }

        swHasLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutDateRange.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                edtStart.setText("");
                edtEnd.setText("");
            }
        });
        layoutDateRange.setVisibility(swHasLimit.isChecked() ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(promo == null ? "Thêm mã giảm giá" : "Sửa mã giảm giá")
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = edtCode.getText().toString().trim();
            String type = spType.getSelectedItemPosition() == 1 ? "fixed" : "percent";
            String valueStr = edtValue.getText().toString().trim();
            String minOrderStr = edtMinOrder.getText().toString().trim();
            String maxDiscountStr = edtMaxDiscount.getText().toString().trim();
            boolean hasLimit = swHasLimit.isChecked();
            String startStr = hasLimit ? edtStart.getText().toString().trim() : "";
            String endStr = hasLimit ? edtEnd.getText().toString().trim() : "";

            if (TextUtils.isEmpty(code) || TextUtils.isEmpty(valueStr)) {
                Toast.makeText(this, "Vui lòng nhập mã và giá trị giảm", Toast.LENGTH_SHORT).show();
                return;
            }

            double value;
            int minOrder;
            Integer maxDiscount = null;
            try {
                value = Double.parseDouble(valueStr);
                minOrder = TextUtils.isEmpty(minOrderStr) ? 0 : Integer.parseInt(minOrderStr);
                if (!TextUtils.isEmpty(maxDiscountStr)) {
                    maxDiscount = Integer.parseInt(maxDiscountStr);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            Long startMillis = parseDate(startStr);
            Long endMillis = parseDate(endStr);
            if (hasLimit && (startMillis == null || endMillis == null)) {
                Toast.makeText(this, "Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startMillis != null && endMillis != null && endMillis < startMillis) {
                Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
                return;
            }

            promotionRepository.savePromotion(
                    promo == null ? null : promo.getPromotionId(),
                    code,
                    type,
                    value,
                    minOrder,
                    maxDiscount,
                    startMillis,
                    endMillis,
                    swActive.isChecked(),
                    (success, message) -> runOnUiThread(() -> {
                        if (!success) {
                            Toast.makeText(this, message == null ? "Không thể lưu khuyến mãi." : message, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dialog.dismiss();
                        Toast.makeText(this, promo == null ? "Đã thêm mã giảm giá." : "Đã cập nhật mã giảm giá.", Toast.LENGTH_SHORT).show();
                    })
            );
        }));
        dialog.show();
    }

    private void bindDatePicker(EditText target) {
        target.setOnClickListener(v -> showDatePicker(target));
        target.setFocusable(false);
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        Long currentValue = parseDate(target.getText().toString().trim());
        if (currentValue != null) {
            calendar.setTimeInMillis(currentValue);
        }

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    target.setText(fmtDate(selected.getTimeInMillis()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    @Nullable
    private Long parseDate(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(value);
            return date == null ? null : date.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private class PromoAdapter extends RecyclerView.Adapter<PromoVH> {
        @NonNull
        @Override
        public PromoVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion, parent, false);
            return new PromoVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PromoVH holder, int position) {
            holder.bind(promos.get(position));
        }

        @Override
        public int getItemCount() {
            return promos.size();
        }
    }

    private class PromoVH extends RecyclerView.ViewHolder {
        private final TextView tvCode;
        private final TextView tvTypeValue;
        private final TextView tvRule;
        private final TextView tvActive;
        private final TextView btnEdit;
        private final TextView btnDelete;

        PromoVH(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvPromoCode);
            tvTypeValue = itemView.findViewById(R.id.tvPromoTypeValue);
            tvRule = itemView.findViewById(R.id.tvPromoRule);
            tvActive = itemView.findViewById(R.id.tvPromoActive);
            btnEdit = itemView.findViewById(R.id.btnPromoEdit);
            btnDelete = itemView.findViewById(R.id.btnPromoDelete);
        }

        void bind(LocalPromotion promo) {
            tvCode.setText("Mã: " + promo.getCode());
            tvTypeValue.setText("Giảm: " + ((int) promo.getValue()) + ("fixed".equals(promo.getType()) ? "đ" : "%"));
            String timeRule = promo.getStartDateMillis() == null && promo.getEndDateMillis() == null
                    ? "Không thời hạn"
                    : fmtDate(promo.getStartDateMillis()) + " - " + fmtDate(promo.getEndDateMillis());
            tvRule.setText("Đơn tối thiểu: " + promo.getMinOrder()
                    + " | Tối đa: " + (promo.getMaxDiscount() != null ? promo.getMaxDiscount() : "-")
                    + " | " + timeRule);
            tvActive.setText(promo.isActive() ? "Đang hoạt động" : "Tạm ẩn");

            btnEdit.setOnClickListener(v -> showAddOrEditDialog(promo));
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(QuanLyKhuyenMaiActivity.this)
                    .setTitle("Xóa khuyến mãi")
                    .setMessage("Bạn có chắc muốn xóa mã \"" + promo.getCode() + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> promotionRepository.deletePromotion(
                            promo.getPromotionId(),
                            (success, message) -> runOnUiThread(() -> {
                                if (!success) {
                                    Toast.makeText(QuanLyKhuyenMaiActivity.this, message == null ? "Lỗi xóa khuyến mãi." : message, Toast.LENGTH_SHORT).show();
                                }
                            })
                    ))
                    .setNegativeButton("Hủy", null)
                    .show());
        }
    }
}
