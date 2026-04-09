package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
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

public class QuanLyKhachHangActivity extends AppCompatActivity {

    private final List<LocalUser> customers = new ArrayList<>();
    private CustomerAdapter adapter;
    private UserCloudRepository userRepository;
    private TextView tvEmpty;
    private ListenerRegistration customersListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_khach_hang);
        InsetsHelper.applyActivityRootPadding(this);

        LocalSessionManager sessionManager = new LocalSessionManager(this);
        if (!"manager".equals(sessionManager.getCurrentUserRole())) {
            Toast.makeText(this, "Chỉ quản lý mới truy cập được", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository = new UserCloudRepository(this);

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        RecyclerView rvCustomers = findViewById(R.id.rvCustomers);
        tvEmpty = findViewById(R.id.tvEmptyCustomers);
        rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerAdapter();
        rvCustomers.setAdapter(adapter);

        listenCustomers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (customersListener != null) {
            customersListener.remove();
        }
    }

    private void listenCustomers() {
        if (customersListener != null) {
            customersListener.remove();
        }
        customersListener = userRepository.listenUsersByRole("customer", users -> {
            customers.clear();
            customers.addAll(users);
            adapter.notifyDataSetChanged();
            if (tvEmpty != null) {
                tvEmpty.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showEditCustomerDialog(LocalUser user) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_customer, null);
        EditText edtName = view.findViewById(R.id.edtCustomerName);
        EditText edtPhone = view.findViewById(R.id.edtCustomerPhone);
        EditText edtEmail = view.findViewById(R.id.edtCustomerEmail);

        edtName.setText(user.getFullName());
        edtPhone.setText(user.getPhone());
        edtEmail.setText(user.getEmail());

        new AlertDialog.Builder(this)
                .setTitle("Sửa khách hàng")
                .setView(view)
                .setPositiveButton("Lưu", (d, which) -> userRepository.updateUserProfile(
                        user.getUserId(),
                        edtName.getText().toString().trim(),
                        user.getGender(),
                        edtEmail.getText().toString().trim(),
                        edtPhone.getText().toString().trim(),
                        user.getBirthdayMillis(),
                        (success, message) -> runOnUiThread(() -> Toast.makeText(
                                this,
                                success ? "Đã cập nhật khách hàng." : (message == null ? "Không cập nhật được khách hàng." : message),
                                Toast.LENGTH_SHORT
                        ).show())
                ))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private class CustomerAdapter extends RecyclerView.Adapter<CustomerVH> {
        @NonNull
        @Override
        public CustomerVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
            return new CustomerVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomerVH holder, int position) {
            holder.bind(customers.get(position));
        }

        @Override
        public int getItemCount() {
            return customers.size();
        }
    }

    private class CustomerVH extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvPhone;
        private final TextView btnEdit;
        private final Switch swStatus;

        CustomerVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            tvEmail = itemView.findViewById(R.id.tvCustomerEmail);
            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            swStatus = itemView.findViewById(R.id.swCustomerStatus);
            btnEdit = itemView.findViewById(R.id.btnCustomerEdit);
        }

        void bind(LocalUser user) {
            tvName.setText("Họ tên: " + safe(user.getFullName(), "(chưa cập nhật)"));
            tvEmail.setText("Email: " + safe(user.getEmail(), ""));
            tvPhone.setText("Số điện thoại: " + safe(user.getPhone(), ""));
            swStatus.setOnCheckedChangeListener(null);
            swStatus.setChecked(user.isActive());
            swStatus.setOnCheckedChangeListener((buttonView, isChecked) -> userRepository.updateUserStatus(
                    user.getUserId(),
                    isChecked,
                    (success, message) -> runOnUiThread(() -> {
                        if (!success) {
                            Toast.makeText(QuanLyKhachHangActivity.this, message == null ? "Lỗi cập nhật trạng thái." : message, Toast.LENGTH_SHORT).show();
                            swStatus.setChecked(!isChecked);
                        }
                    })
            ));
            btnEdit.setOnClickListener(v -> showEditCustomerDialog(user));
        }

        private String safe(String value, String fallback) {
            return TextUtils.isEmpty(value) ? fallback : value;
        }
    }
}
