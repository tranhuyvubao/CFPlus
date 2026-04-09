package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalUser;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class QuanLyNhanVienActivity extends AppCompatActivity {

    private final List<LocalUser> staffList = new ArrayList<>();
    private StaffAdapter adapter;
    private UserCloudRepository userCloudRepository;
    private TextView tvEmpty;
    private ListenerRegistration staffListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_nhan_vien);
        InsetsHelper.applyActivityRootPadding(this);

        LocalSessionManager sessionManager = new LocalSessionManager(this);
        if (!"manager".equals(sessionManager.getCurrentUserRole())) {
            Toast.makeText(this, "Chỉ quản lý mới truy cập được chức năng này", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userCloudRepository = new UserCloudRepository(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        RecyclerView rvStaff = findViewById(R.id.rvStaff);
        TextView btnAddStaff = findViewById(R.id.btnAddStaff);
        tvEmpty = findViewById(R.id.tvEmptyStaff);
        rvStaff.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StaffAdapter(staffList);
        rvStaff.setAdapter(adapter);

        btnAddStaff.setOnClickListener(v -> showCreateStaffDialog());
        listenStaff();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (staffListener != null) {
            staffListener.remove();
        }
    }

    private void listenStaff() {
        if (staffListener != null) {
            staffListener.remove();
        }
        staffListener = userCloudRepository.listenUsersByRole("staff", users -> {
            staffList.clear();
            staffList.addAll(users);
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(staffList.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showCreateStaffDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_staff, null);
        EditText edtEmail = view.findViewById(R.id.edtStaffEmail);
        EditText edtPassword = view.findViewById(R.id.edtStaffPassword);
        EditText edtFullName = view.findViewById(R.id.edtStaffFullName);
        EditText edtPhone = view.findViewById(R.id.edtStaffPhone);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tạo tài khoản nhân viên")
                .setView(view)
                .setPositiveButton("Tạo", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(listener -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                createStaff(dialog, edtEmail, edtPassword, edtFullName, edtPhone)
        ));
        dialog.show();
    }

    private void createStaff(AlertDialog dialog, EditText edtEmail, EditText edtPassword, EditText edtFullName, EditText edtPhone) {
        String email = valueOf(edtEmail);
        String password = valueOf(edtPassword);
        String fullName = valueOf(edtFullName);
        String phone = valueOf(edtPhone);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        userCloudRepository.createStaff(email, password, fullName, phone, (localUser, message) -> {
            if (localUser == null) {
                Toast.makeText(this, message == null ? "Không tạo được nhân viên" : message, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Tạo nhân viên thành công!", Toast.LENGTH_SHORT).show();
            if (dialog != null) {
                dialog.dismiss();
            }
        });
    }

    private String valueOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private class StaffAdapter extends RecyclerView.Adapter<StaffViewHolder> {

        private final List<LocalUser> data;

        StaffAdapter(List<LocalUser> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff, parent, false);
            return new StaffViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class StaffViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvRole;
        private final Switch swStatus;

        StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStaffName);
            tvEmail = itemView.findViewById(R.id.tvStaffEmail);
            tvRole = itemView.findViewById(R.id.tvStaffRole);
            swStatus = itemView.findViewById(R.id.swStaffStatus);
        }

        void bind(LocalUser user) {
            tvName.setText("Họ tên: " + displayOrDefault(user.getFullName()));
            tvEmail.setText("Email: " + displayOrDefault(user.getEmail()));
            tvRole.setText("Vai trò: " + displayOrDefault(user.getRole()));

            swStatus.setOnCheckedChangeListener(null);
            swStatus.setChecked(user.isActive());
            swStatus.setOnCheckedChangeListener((buttonView, isChecked) -> userCloudRepository.updateUserStatus(user.getUserId(), isChecked, (success, message) -> {
                if (!success) {
                    Toast.makeText(QuanLyNhanVienActivity.this, message == null ? "Lỗi cập nhật trạng thái" : message, Toast.LENGTH_SHORT).show();
                    swStatus.setOnCheckedChangeListener(null);
                    swStatus.setChecked(!isChecked);
                }
            }));
        }

        private String displayOrDefault(String value) {
            return value == null || value.trim().isEmpty() ? "(chưa cập nhật)" : value;
        }
    }
}
