package com.example.do_an_hk1_androidstudio;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.do_an_hk1_androidstudio.cloud.CatalogCloudRepository;
import com.example.do_an_hk1_androidstudio.cloud.StoreCloudRepository;
import com.example.do_an_hk1_androidstudio.config.CafeStoreConfig;
import com.example.do_an_hk1_androidstudio.local.model.StoreBranch;
import com.example.do_an_hk1_androidstudio.local.model.StoreProfile;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ManagerStoreSettingsActivity extends AppCompatActivity {

    private StoreCloudRepository storeRepository;
    private CatalogCloudRepository catalogRepository;
    private ListenerRegistration profileListener;
    private ListenerRegistration branchesListener;

    private ImageView imgStoreLogo;
    private EditText edtStoreName;
    private EditText edtStoreTagline;
    private EditText edtStoreEmail;
    private TextView tvDefaultBranch;
    private TextView tvBranchSummary;
    private LinearLayout layoutBranches;

    private String currentLogoUrl = "";
    private String currentDefaultBranchId = "main";
    private final List<StoreBranch> branches = new ArrayList<>();

    private final ActivityResultLauncher<String> pickLogoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadLogo(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager_store_settings);
        InsetsHelper.applyActivityRootPadding(this);

        storeRepository = new StoreCloudRepository(this);
        catalogRepository = new CatalogCloudRepository(this);

        imgStoreLogo = findViewById(R.id.imgStoreLogo);
        edtStoreName = findViewById(R.id.edtStoreName);
        edtStoreTagline = findViewById(R.id.edtStoreTagline);
        edtStoreEmail = findViewById(R.id.edtStoreEmail);
        tvDefaultBranch = findViewById(R.id.tvDefaultBranch);
        tvBranchSummary = findViewById(R.id.tvBranchSummary);
        layoutBranches = findViewById(R.id.layoutBranches);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChooseLogo).setOnClickListener(v -> pickLogoLauncher.launch("image/*"));
        findViewById(R.id.btnSaveStoreProfile).setOnClickListener(v -> saveStoreProfile());
        findViewById(R.id.btnAddBranch).setOnClickListener(v -> showBranchDialog(null));

        listenStoreData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileListener != null) {
            profileListener.remove();
        }
        if (branchesListener != null) {
            branchesListener.remove();
        }
    }

    private void listenStoreData() {
        profileListener = storeRepository.listenStoreProfile(profile -> {
            currentLogoUrl = valueOrEmpty(profile.getLogoUrl());
            currentDefaultBranchId = TextUtils.isEmpty(profile.getDefaultBranchId()) ? "main" : profile.getDefaultBranchId();
            edtStoreName.setText(profile.getStoreName());
            edtStoreTagline.setText(profile.getTagline());
            edtStoreEmail.setText(profile.getEmail());
            bindLogo();
            bindDefaultBranchText();
            renderBranches();
        });

        branchesListener = storeRepository.listenBranches(nextBranches -> {
            branches.clear();
            branches.addAll(nextBranches);
            bindDefaultBranchText();
            renderBranches();
        });
    }

    private void saveStoreProfile() {
        String storeName = edtStoreName.getText().toString().trim();
        if (TextUtils.isEmpty(storeName)) {
            edtStoreName.setError("Nhập tên thương hiệu");
            return;
        }
        StoreProfile profile = new StoreProfile(
                storeName,
                edtStoreTagline.getText().toString().trim(),
                currentLogoUrl,
                edtStoreEmail.getText().toString().trim(),
                currentDefaultBranchId
        );
        storeRepository.saveStoreProfile(profile, (success, message) -> {
            Toast.makeText(this,
                    success ? "Đã lưu thông tin cửa hàng" : valueOrDefault(message, "Không lưu được thông tin"),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadLogo(Uri uri) {
        Toast.makeText(this, "Đang xử lý logo...", Toast.LENGTH_SHORT).show();
        catalogRepository.uploadCatalogImage(uri, "store", (success, imageUrl, message) -> {
            if (!success || TextUtils.isEmpty(imageUrl)) {
                Toast.makeText(this, valueOrDefault(message, "Không đọc được logo"), Toast.LENGTH_SHORT).show();
                return;
            }
            currentLogoUrl = imageUrl;
            bindLogo();
            Toast.makeText(this, "Đã chọn logo. Bấm lưu để áp dụng.", Toast.LENGTH_SHORT).show();
        });
    }

    private void bindLogo() {
        Glide.with(this)
                .load(TextUtils.isEmpty(currentLogoUrl) ? R.drawable.cfplus4 : currentLogoUrl)
                .placeholder(R.drawable.cfplus4)
                .error(R.drawable.cfplus4)
                .into(imgStoreLogo);
    }

    private void bindDefaultBranchText() {
        StoreBranch defaultBranch = findBranch(currentDefaultBranchId);
        String name = defaultBranch == null ? currentDefaultBranchId : defaultBranch.getName();
        tvDefaultBranch.setText("Chi nhánh mặc định: " + valueOrDefault(name, "main"));
    }

    private void renderBranches() {
        layoutBranches.removeAllViews();
        tvBranchSummary.setText(branches.size() + " chi nhánh");
        LayoutInflater inflater = LayoutInflater.from(this);
        for (StoreBranch branch : branches) {
            View itemView = inflater.inflate(R.layout.item_store_branch, layoutBranches, false);
            TextView tvName = itemView.findViewById(R.id.tvBranchName);
            TextView tvAddress = itemView.findViewById(R.id.tvBranchAddress);
            TextView tvMeta = itemView.findViewById(R.id.tvBranchMeta);
            TextView btnDefault = itemView.findViewById(R.id.btnDefaultBranch);
            TextView btnEdit = itemView.findViewById(R.id.btnEditBranch);
            TextView btnDelete = itemView.findViewById(R.id.btnDeleteBranch);

            boolean isDefault = branch.getBranchId().equals(currentDefaultBranchId);
            tvName.setText(branch.getName() + (isDefault ? " • mặc định" : ""));
            tvAddress.setText(branch.getAddress());
            tvMeta.setText(branch.getHours() + " • " + branch.getPhone() + (branch.isActive() ? "" : " • tạm ẩn"));
            btnDefault.setVisibility(isDefault ? View.GONE : View.VISIBLE);
            btnDefault.setOnClickListener(v -> {
                currentDefaultBranchId = branch.getBranchId();
                saveStoreProfile();
            });
            btnEdit.setOnClickListener(v -> showBranchDialog(branch));
            btnDelete.setOnClickListener(v -> confirmDeleteBranch(branch));

            layoutBranches.addView(itemView);
        }
    }

    private void showBranchDialog(@Nullable StoreBranch branch) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_store_branch, null, false);
        TextView title = dialogView.findViewById(R.id.tvBranchDialogTitle);
        EditText edtName = dialogView.findViewById(R.id.edtBranchName);
        EditText edtAddress = dialogView.findViewById(R.id.edtBranchAddress);
        EditText edtPhone = dialogView.findViewById(R.id.edtBranchPhone);
        EditText edtHours = dialogView.findViewById(R.id.edtBranchHours);
        EditText edtLat = dialogView.findViewById(R.id.edtBranchLat);
        EditText edtLng = dialogView.findViewById(R.id.edtBranchLng);
        Switch switchActive = dialogView.findViewById(R.id.switchBranchActive);

        title.setText(branch == null ? "Thêm chi nhánh" : "Sửa chi nhánh");
        edtName.setText(branch == null ? "" : branch.getName());
        edtAddress.setText(branch == null ? "" : branch.getAddress());
        edtPhone.setText(branch == null ? "" : branch.getPhone());
        edtHours.setText(branch == null ? CafeStoreConfig.STORE_HOURS : branch.getHours());
        edtLat.setText(String.valueOf(branch == null ? CafeStoreConfig.STORE_LAT : branch.getLatitude()));
        edtLng.setText(String.valueOf(branch == null ? CafeStoreConfig.STORE_LNG : branch.getLongitude()));
        switchActive.setChecked(branch == null || branch.isActive());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String hours = edtHours.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                edtName.setError("Nhập tên chi nhánh");
                return;
            }
            if (TextUtils.isEmpty(address)) {
                edtAddress.setError("Nhập địa chỉ");
                return;
            }
            storeRepository.saveBranch(
                    branch == null ? null : branch.getBranchId(),
                    name,
                    address,
                    phone,
                    hours,
                    parseDouble(edtLat.getText().toString(), CafeStoreConfig.STORE_LAT),
                    parseDouble(edtLng.getText().toString(), CafeStoreConfig.STORE_LNG),
                    switchActive.isChecked(),
                    (success, message) -> {
                        Toast.makeText(this,
                                success ? "Đã lưu chi nhánh" : valueOrDefault(message, "Không lưu được chi nhánh"),
                                Toast.LENGTH_SHORT).show();
                        if (success) {
                            dialog.dismiss();
                        }
                    }
            );
        }));
        dialog.show();
    }

    private void confirmDeleteBranch(StoreBranch branch) {
        if (branch.getBranchId().equals(currentDefaultBranchId)) {
            Toast.makeText(this, "Không thể xóa chi nhánh mặc định", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xóa chi nhánh?")
                .setMessage("Chi nhánh \"" + branch.getName() + "\" sẽ bị xóa khỏi danh sách.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> storeRepository.deleteBranch(branch.getBranchId(), (success, message) -> {
                    Toast.makeText(this,
                            success ? "Đã xóa chi nhánh" : valueOrDefault(message, "Không xóa được chi nhánh"),
                            Toast.LENGTH_SHORT).show();
                }))
                .show();
    }

    @Nullable
    private StoreBranch findBranch(String branchId) {
        for (StoreBranch branch : branches) {
            if (branch.getBranchId().equals(branchId)) {
                return branch;
            }
        }
        return null;
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
