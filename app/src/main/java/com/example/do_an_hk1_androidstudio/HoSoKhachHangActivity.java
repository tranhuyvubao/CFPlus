package com.example.do_an_hk1_androidstudio;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an_hk1_androidstudio.cloud.UserCloudRepository;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.LocalCustomerAddress;
import com.example.do_an_hk1_androidstudio.ui.AddressCatalog;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;

import java.util.List;

public class HoSoKhachHangActivity extends AppCompatActivity {

    private LinearLayout addressContainer;
    private TextView tvAddressEmpty;
    private UserCloudRepository userCloudRepository;
    private LocalSessionManager sessionManager;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ho_so_khach_hang);
        InsetsHelper.applyStatusBarPadding(findViewById(R.id.headerHoSoKhachHang));
        InsetsHelper.applyNavigationBarPadding(findViewById(R.id.rootHoSoKhachHang));

        View tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        addressContainer = findViewById(R.id.addressContainer);
        tvAddressEmpty = findViewById(R.id.tvAddressEmpty);
        TextView btnAddAddress = findViewById(R.id.btnAddAddress);

        userCloudRepository = new UserCloudRepository(this);
        sessionManager = new LocalSessionManager(this);
        userId = sessionManager.getCurrentUserId();

        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        renderAddresses();
        btnAddAddress.setOnClickListener(v -> showAddressDialog(null));
    }

    private void renderAddresses() {
        addressContainer.removeAllViews();
        userCloudRepository.getCustomerAddresses(userId, (addresses, message) -> {
            addressContainer.removeAllViews();
            tvAddressEmpty.setVisibility(addresses.isEmpty() ? View.VISIBLE : View.GONE);

            LayoutInflater inflater = LayoutInflater.from(this);
            for (LocalCustomerAddress address : addresses) {
                View itemView = inflater.inflate(R.layout.item_customer_address, addressContainer, false);
                TextView tvLabel = itemView.findViewById(R.id.tvAddressLabel);
                TextView tvDefault = itemView.findViewById(R.id.tvAddressDefault);
                TextView tvRecipient = itemView.findViewById(R.id.tvAddressRecipient);
                TextView tvFull = itemView.findViewById(R.id.tvAddressFull);
                TextView btnEdit = itemView.findViewById(R.id.btnEditAddress);
                TextView btnDelete = itemView.findViewById(R.id.btnDeleteAddress);

                tvLabel.setText(address.getLabel());
                tvDefault.setVisibility(address.isDefault() ? View.VISIBLE : View.GONE);
                tvRecipient.setText(address.getRecipientName() + " • " + address.getPhone());
                tvFull.setText(address.buildDisplayAddress());

                btnEdit.setOnClickListener(v -> showAddressDialog(address));
                btnDelete.setOnClickListener(v -> userCloudRepository.deleteCustomerAddress(userId, address.getAddressId(), (success, deleteMessage) -> {
                    Toast.makeText(this,
                            success ? "Đã xóa địa chỉ." : (deleteMessage == null ? "Không thể xóa địa chỉ." : deleteMessage),
                            Toast.LENGTH_SHORT).show();
                    if (success) {
                        renderAddresses();
                    }
                }));

                addressContainer.addView(itemView);
            }
        });
    }

    private void showAddressDialog(@Nullable LocalCustomerAddress editingAddress) {
        userCloudRepository.getCustomerAddresses(userId, (addresses, message) -> {
            if (editingAddress == null && addresses.size() >= 3) {
                Toast.makeText(this, "Chỉ được lưu tối đa 3 địa chỉ.", Toast.LENGTH_SHORT).show();
                return;
            }

            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_customer_address, null, false);
            EditText edtLabel = dialogView.findViewById(R.id.edtAddressLabel);
            EditText edtRecipientName = dialogView.findViewById(R.id.edtRecipientName);
            EditText edtRecipientPhone = dialogView.findViewById(R.id.edtRecipientPhone);
            AutoCompleteTextView autoCountry = dialogView.findViewById(R.id.autoCountry);
            AutoCompleteTextView autoProvince = dialogView.findViewById(R.id.autoProvince);
            AutoCompleteTextView autoDistrict = dialogView.findViewById(R.id.autoDistrict);
            AutoCompleteTextView autoWard = dialogView.findViewById(R.id.autoWard);
            EditText edtDetailAddress = dialogView.findViewById(R.id.edtDetailAddress);
            CheckBox checkDefaultAddress = dialogView.findViewById(R.id.checkDefaultAddress);

            bindDropdown(autoCountry, AddressCatalog.getCountries());

            autoCountry.setOnItemClickListener((parent, view, position, id) -> {
                bindDropdown(autoProvince, AddressCatalog.getProvinces(autoCountry.getText().toString().trim()));
                autoProvince.setText("", false);
                autoDistrict.setText("", false);
                autoWard.setText("", false);
                bindDropdown(autoDistrict, java.util.Collections.emptyList());
                bindDropdown(autoWard, java.util.Collections.emptyList());
            });

            autoProvince.setOnItemClickListener((parent, view, position, id) -> {
                bindDropdown(autoDistrict, AddressCatalog.getDistricts(autoCountry.getText().toString().trim(), autoProvince.getText().toString().trim()));
                autoDistrict.setText("", false);
                autoWard.setText("", false);
                bindDropdown(autoWard, java.util.Collections.emptyList());
            });

            autoDistrict.setOnItemClickListener((parent, view, position, id) -> {
                bindDropdown(autoWard, AddressCatalog.getWards(autoCountry.getText().toString().trim(), autoProvince.getText().toString().trim(), autoDistrict.getText().toString().trim()));
                autoWard.setText("", false);
            });

            if (editingAddress != null) {
                edtLabel.setText(editingAddress.getLabel());
                edtRecipientName.setText(editingAddress.getRecipientName());
                edtRecipientPhone.setText(editingAddress.getPhone());
                autoCountry.setText(editingAddress.getCountry(), false);
                bindDropdown(autoProvince, AddressCatalog.getProvinces(editingAddress.getCountry()));
                autoProvince.setText(editingAddress.getProvince(), false);
                bindDropdown(autoDistrict, AddressCatalog.getDistricts(editingAddress.getCountry(), editingAddress.getProvince()));
                autoDistrict.setText(editingAddress.getDistrict(), false);
                bindDropdown(autoWard, AddressCatalog.getWards(editingAddress.getCountry(), editingAddress.getProvince(), editingAddress.getDistrict()));
                autoWard.setText(editingAddress.getWard(), false);
                edtDetailAddress.setText(editingAddress.getDetailAddress());
                checkDefaultAddress.setChecked(editingAddress.isDefault());
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(editingAddress == null ? "Thêm địa chỉ" : "Cập nhật địa chỉ")
                    .setView(dialogView)
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Lưu", null)
                    .create();

            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String label = edtLabel.getText().toString().trim();
                String recipientName = edtRecipientName.getText().toString().trim();
                String phone = edtRecipientPhone.getText().toString().trim();
                String country = autoCountry.getText().toString().trim();
                String province = autoProvince.getText().toString().trim();
                String district = autoDistrict.getText().toString().trim();
                String ward = autoWard.getText().toString().trim();
                String detail = edtDetailAddress.getText().toString().trim();

                if (TextUtils.isEmpty(label) || TextUtils.isEmpty(recipientName) || TextUtils.isEmpty(phone)
                        || TextUtils.isEmpty(country) || TextUtils.isEmpty(province) || TextUtils.isEmpty(district)
                        || TextUtils.isEmpty(ward) || TextUtils.isEmpty(detail)) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin địa chỉ.", Toast.LENGTH_SHORT).show();
                    return;
                }

                userCloudRepository.saveCustomerAddress(
                        userId,
                        editingAddress == null ? null : editingAddress.getAddressId(),
                        label,
                        recipientName,
                        phone,
                        country,
                        province,
                        district,
                        ward,
                        detail,
                        checkDefaultAddress.isChecked(),
                        (success, saveMessage) -> {
                            if (!success) {
                                Toast.makeText(this, saveMessage == null ? "Không thể lưu địa chỉ." : saveMessage, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Toast.makeText(this, "Đã lưu địa chỉ.", Toast.LENGTH_SHORT).show();
                            renderAddresses();
                            dialog.dismiss();
                            setResult(RESULT_OK);
                        }
                );
            }));

            dialog.show();
        });
    }

    private void bindDropdown(AutoCompleteTextView view, List<String> options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);
        view.setAdapter(adapter);
        view.setOnClickListener(v -> view.showDropDown());
    }
}
