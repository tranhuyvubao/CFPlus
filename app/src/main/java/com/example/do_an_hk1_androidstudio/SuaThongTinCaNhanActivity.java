package com.example.do_an_hk1_androidstudio;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalUser;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SuaThongTinCaNhanActivity extends AppCompatActivity {

    private EditText edtHoTen;
    private EditText edtNgaySinh;
    private EditText edtGioiTinh;
    private EditText edtEmail;
    private EditText edtSoDienThoai;
    private UserCloudRepository userCloudRepository;
    private LocalSessionManager sessionManager;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sua_thongtincanhan);
        InsetsHelper.applyActivityRootPadding(this);

        edtHoTen = findViewById(R.id.edtHoTen);
        edtNgaySinh = findViewById(R.id.edtNgaySinh);
        edtGioiTinh = findViewById(R.id.edtGioiTinh);
        edtEmail = findViewById(R.id.edtEmail);
        edtSoDienThoai = findViewById(R.id.edtSoDienThoai);
        Button btnLuu = findViewById(R.id.btn_luu);
        TextView tvQuayLai = findViewById(R.id.tvquaylaiTTnv);

        userCloudRepository = new UserCloudRepository(this);
        sessionManager = new LocalSessionManager(this);
        userId = sessionManager.getCurrentUserId();

        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCloudData();
        bindDatePicker();
        btnLuu.setOnClickListener(v -> saveCloudData());
        tvQuayLai.setOnClickListener(v -> finish());
    }

    private void bindDatePicker() {
        edtNgaySinh.setFocusable(false);
        edtNgaySinh.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            String current = valueOf(edtNgaySinh);
            if (!current.isEmpty()) {
                try {
                    Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(current);
                    if (date != null) {
                        calendar.setTime(date);
                    }
                } catch (Exception ignored) {
                }
            }
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, dayOfMonth);
                        edtNgaySinh.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(picked.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void loadCloudData() {
        userCloudRepository.getUserById(userId, (user, message) -> {
            if (user == null) {
                Toast.makeText(this, message == null ? "Không tải được dữ liệu" : message, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            bindUser(user);
        });
    }

    private void bindUser(LocalUser user) {
        edtHoTen.setText(user.getFullName() == null ? "" : user.getFullName());
        edtGioiTinh.setText(user.getGender() == null ? "" : user.getGender());
        edtSoDienThoai.setText(user.getPhone() == null ? "" : user.getPhone());
        edtEmail.setText(user.getEmail() == null ? "" : user.getEmail());
        if (user.getBirthdayMillis() != null) {
            edtNgaySinh.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(user.getBirthdayMillis())));
        }
    }

    private void saveCloudData() {
        Long birthdayMillis = null;
        String ngaySinh = valueOf(edtNgaySinh);
        if (!ngaySinh.isEmpty()) {
            try {
                Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(ngaySinh);
                if (date != null) {
                    birthdayMillis = date.getTime();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Định dạng ngày phải là dd/MM/yyyy", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        userCloudRepository.updateUserProfile(
                userId,
                valueOf(edtHoTen),
                valueOf(edtGioiTinh),
                valueOf(edtEmail),
                valueOf(edtSoDienThoai),
                birthdayMillis,
                (success, message) -> {
                    if (!success) {
                        Toast.makeText(this, message == null ? "Lỗi khi cập nhật" : message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    userCloudRepository.getUserById(userId, (user, error) -> {
                        if (user != null) {
                            sessionManager.saveUser(user);
                        }
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
        );
    }

    private String valueOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
