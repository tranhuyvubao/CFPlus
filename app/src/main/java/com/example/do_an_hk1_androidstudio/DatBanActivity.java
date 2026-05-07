package com.example.do_an_hk1_androidstudio;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.TableCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatBanActivity extends AppCompatActivity {

    private EditText edtThoiGian;
    private EditText edtSoNguoi;
    private EditText edtGhiChu;
    private TableCloudRepository tableRepository;
    private UserCloudRepository userRepository;
    private LocalSessionManager localSessionManager;
    private TextView tvCustomerInfo;
    private boolean customerInfoReady;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dat_ban);
        InsetsHelper.applyActivityRootPadding(this);

        tableRepository = new TableCloudRepository(this);
        userRepository = new UserCloudRepository(this);
        localSessionManager = new LocalSessionManager(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        tvCustomerInfo = findViewById(R.id.tvCustomerInfo);
        edtThoiGian = findViewById(R.id.edtThoiGian);
        edtSoNguoi = findViewById(R.id.edtSoNguoi);
        edtGhiChu = findViewById(R.id.edtGhiChu);
        TextView btnDatBan = findViewById(R.id.btnDatBan);

        edtThoiGian.setHint("Chọn ngày giờ đặt bàn");
        edtThoiGian.setFocusable(false);
        edtThoiGian.setClickable(true);

        edtThoiGian.setOnClickListener(v -> showDateTimePicker());
        btnDatBan.setOnClickListener(v -> submitReservation());

        bindCustomerInfo();
    }

    private void bindCustomerInfo() {
        String userId = localSessionManager.getCurrentUserId();
        if (TextUtils.isEmpty(userId)) {
            tvCustomerInfo.setText("Bạn chưa đăng nhập. Vui lòng đăng nhập để đặt bàn.");
            customerInfoReady = false;
            return;
        }
        String fullName = localSessionManager.getCurrentUserFullName();
        String email = localSessionManager.getCurrentUserEmail();
        tvCustomerInfo.setText(buildCustomerInfo(fullName, email, null));
        customerInfoReady = isCustomerInfoComplete(fullName, null);
        userRepository.getUserById(userId, (user, message) -> runOnUiThread(() -> {
            if (user == null) {
                return;
            }
            customerInfoReady = isCustomerInfoComplete(user.getFullName(), user.getPhone());
            tvCustomerInfo.setText(buildCustomerInfo(user.getFullName(), user.getEmail(), user.getPhone()));
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

    private void submitReservation() {
        String customerId = localSessionManager.getCurrentUserId();
        if (TextUtils.isEmpty(customerId)) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!customerInfoReady) {
            Toast.makeText(this, "Vui lòng bổ sung họ tên và số điện thoại trước khi đặt bàn.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SuaThongTinCaNhanActivity.class));
            return;
        }

        String thoiGianStr = edtThoiGian.getText().toString().trim();
        String soNguoiStr = edtSoNguoi.getText().toString().trim();
        String ghiChu = edtGhiChu.getText().toString().trim();

        if (TextUtils.isEmpty(thoiGianStr) || TextUtils.isEmpty(soNguoiStr)) {
            Toast.makeText(this, "Vui lòng nhập thời gian và số người", Toast.LENGTH_SHORT).show();
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
                null,
                "Chưa xếp bàn",
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

    private String buildCustomerInfo(String fullName, String email, String phone) {
        return "Khách hàng: " + fallback(fullName, "Chưa cập nhật")
                + "\nSố điện thoại: " + fallback(phone, "Chưa cập nhật")
                + "\nEmail: " + fallback(email, "Chưa cập nhật");
    }

    private String fallback(String value, String defaultValue) {
        return TextUtils.isEmpty(value) ? defaultValue : value.trim();
    }

    private boolean isCustomerInfoComplete(String fullName, String phone) {
        return !TextUtils.isEmpty(fullName) && !TextUtils.isEmpty(phone);
    }
}
