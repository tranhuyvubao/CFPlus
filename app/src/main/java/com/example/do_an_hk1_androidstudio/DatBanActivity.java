package com.example.do_an_hk1_androidstudio;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalCafeTable;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatBanActivity extends AppCompatActivity {

    private EditText edtTenBan;
    private EditText edtThoiGian;
    private EditText edtSoNguoi;
    private EditText edtGhiChu;
    private String selectedTableId;
    private TableCloudRepository tableRepository;
    private LocalSessionManager localSessionManager;
    private final List<LocalCafeTable> activeTables = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dat_ban);
        InsetsHelper.applyActivityRootPadding(this);

        tableRepository = new TableCloudRepository(this);
        localSessionManager = new LocalSessionManager(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        edtTenBan = findViewById(R.id.edtTenBan);
        TextView btnChonBan = findViewById(R.id.btnChonBan);
        edtThoiGian = findViewById(R.id.edtThoiGian);
        edtSoNguoi = findViewById(R.id.edtSoNguoi);
        edtGhiChu = findViewById(R.id.edtGhiChu);
        TextView btnDatBan = findViewById(R.id.btnDatBan);

        edtThoiGian.setHint("Chọn ngày giờ đặt bàn");
        edtThoiGian.setFocusable(false);
        edtThoiGian.setClickable(true);

        btnChonBan.setOnClickListener(v -> showPickTableDialog());
        edtThoiGian.setOnClickListener(v -> showDateTimePicker());
        btnDatBan.setOnClickListener(v -> submitReservation());

        loadActiveTables();
    }

    private void loadActiveTables() {
        tableRepository.getActiveTables((tables, message) -> runOnUiThread(() -> {
            activeTables.clear();
            for (LocalCafeTable table : tables) {
                if (table.isActive()) {
                    activeTables.add(table);
                }
            }
        }));
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        String current = edtThoiGian.getText().toString().trim();
        if (!TextUtils.isEmpty(current)) {
            try {
                Date date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(current);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> new TimePickerDialog(
                        DatBanActivity.this,
                        (TimePicker timeView, int hourOfDay, int minute) -> {
                            Calendar picked = Calendar.getInstance();
                            picked.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                            picked.set(Calendar.MILLISECOND, 0);
                            edtThoiGian.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(picked.getTime()));
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                ).show(),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showPickTableDialog() {
        if (activeTables.isEmpty()) {
            Toast.makeText(this, "Chưa có bàn nào trong hệ thống", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[activeTables.size()];
        for (int i = 0; i < activeTables.size(); i++) {
            LocalCafeTable table = activeTables.get(i);
            labels[i] = table.getName() + " (" + normalizeStatus(table.getStatus()) + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn bàn")
                .setItems(labels, (dialog, which) -> {
                    LocalCafeTable table = activeTables.get(which);
                    selectedTableId = table.getTableId();
                    edtTenBan.setText(table.getName());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitReservation() {
        String customerId = localSessionManager.getCurrentUserId();
        if (TextUtils.isEmpty(customerId)) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String tenBan = edtTenBan.getText().toString().trim();
        String thoiGianStr = edtThoiGian.getText().toString().trim();
        String soNguoiStr = edtSoNguoi.getText().toString().trim();
        String ghiChu = edtGhiChu.getText().toString().trim();

        if (TextUtils.isEmpty(tenBan) || TextUtils.isEmpty(thoiGianStr) || TextUtils.isEmpty(soNguoiStr)) {
            Toast.makeText(this, "Vui lòng nhập bàn, thời gian và số người", Toast.LENGTH_SHORT).show();
            return;
        }

        int peopleCount;
        try {
            peopleCount = Integer.parseInt(soNguoiStr);
        } catch (Exception e) {
            Toast.makeText(this, "Số người không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (peopleCount <= 0) {
            Toast.makeText(this, "Số người phải lớn hơn 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Date reserveDate;
        try {
            reserveDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(thoiGianStr);
        } catch (Exception e) {
            Toast.makeText(this, "Thời gian không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (reserveDate == null) {
            Toast.makeText(this, "Thời gian không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        tableRepository.createReservation(
                customerId,
                selectedTableId,
                tenBan,
                reserveDate.getTime(),
                peopleCount,
                ghiChu,
                (success, message) -> runOnUiThread(() -> {
                    if (!success) {
                        Toast.makeText(this, message == null ? "Lỗi đặt bàn" : message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(this, "Đặt bàn thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
        );
    }

    private String normalizeStatus(String status) {
        if ("occupied".equals(status)) return "Đang dùng";
        if ("reserved".equals(status)) return "Đã đặt";
        return "Trống";
    }
}
