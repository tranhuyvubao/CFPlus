package com.example.do_an_hk1_androidstudio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.example.do_an_hk1_androidstudio.cloud.StoreCloudRepository;
import com.example.do_an_hk1_androidstudio.config.CafeStoreConfig;
import com.example.do_an_hk1_androidstudio.local.model.StoreBranch;
import com.example.do_an_hk1_androidstudio.local.model.StoreProfile;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ThongTinCuaHangActivity extends AppCompatActivity {

    private StoreCloudRepository storeRepository;
    private ListenerRegistration profileListener;
    private ListenerRegistration branchesListener;
    private TextView tvStoreInfoName;
    private TextView tvMapStatus;
    private TextView tvStoreAddress;
    private TextView tvStoreHours;
    private TextView tvStoreContact;
    private LinearLayout layoutPublicBranches;
    private final List<StoreBranch> branches = new ArrayList<>();
    private StoreProfile storeProfile;
    private StoreBranch selectedBranch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.thongtin_cuahang);

        NestedScrollView scrollView = findViewById(R.id.scrollView);
        TextView btnTroVe = findViewById(R.id.btnTroVe);
        TextView btnOpenMap = findViewById(R.id.btnOpenMap);
        TextView btnDirections = findViewById(R.id.btnDirections);
        TextView btnCallStore = findViewById(R.id.btnCallStore);
        tvStoreInfoName = findViewById(R.id.tvStoreInfoName);
        tvMapStatus = findViewById(R.id.tvMapStatus);
        tvStoreAddress = findViewById(R.id.tvStoreAddress);
        tvStoreHours = findViewById(R.id.tvStoreHours);
        tvStoreContact = findViewById(R.id.tvStoreContact);
        layoutPublicBranches = findViewById(R.id.layoutPublicBranches);

        storeRepository = new StoreCloudRepository(this);
        storeProfile = storeRepository.defaultProfile();
        selectedBranch = storeRepository.defaultBranches().get(0);
        bindStoreInfo();
        listenStoreInfo();

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnTroVe.setOnClickListener(v -> finish());
        btnOpenMap.setOnClickListener(v -> openStoreMap(false));
        btnDirections.setOnClickListener(v -> openStoreMap(true));
        btnCallStore.setOnClickListener(v -> callStore());
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

    private void listenStoreInfo() {
        profileListener = storeRepository.listenStoreProfile(profile -> {
            storeProfile = profile;
            selectedBranch = findDefaultBranch(profile.getDefaultBranchId());
            bindStoreInfo();
        });
        branchesListener = storeRepository.listenBranches(nextBranches -> {
            branches.clear();
            for (StoreBranch branch : nextBranches) {
                if (branch.isActive()) {
                    branches.add(branch);
                }
            }
            selectedBranch = findDefaultBranch(storeProfile == null ? "main" : storeProfile.getDefaultBranchId());
            bindStoreInfo();
        });
    }

    private void bindStoreInfo() {
        StoreProfile profile = storeProfile == null ? storeRepository.defaultProfile() : storeProfile;
        StoreBranch branch = selectedBranch == null ? storeRepository.defaultBranches().get(0) : selectedBranch;
        tvStoreInfoName.setText(profile.getStoreName());
        tvMapStatus.setText(valueOrDefault(profile.getTagline(), "Chọn chi nhánh gần bạn để xem địa chỉ, giờ mở cửa và chỉ đường."));
        tvStoreAddress.setText(branch.getAddress());
        tvStoreHours.setText(branch.getHours());
        tvStoreContact.setText(branch.getPhone() + " • " + valueOrDefault(profile.getEmail(), CafeStoreConfig.STORE_EMAIL));
        renderBranches();
    }

    private void renderBranches() {
        layoutPublicBranches.removeAllViews();
        List<StoreBranch> visibleBranches = branches.isEmpty() ? storeRepository.defaultBranches() : branches;
        for (StoreBranch branch : visibleBranches) {
            TextView row = new TextView(this);
            boolean selected = selectedBranch != null && branch.getBranchId().equals(selectedBranch.getBranchId());
            row.setBackgroundResource(selected ? R.drawable.manager_accent_pill : R.drawable.manager_search_background);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setText(branch.getName() + "\n" + branch.getAddress());
            row.setTextColor(getColor(selected ? R.color.dashboard_accent : R.color.dashboard_text_primary));
            row.setTextSize(14);
            row.setOnClickListener(v -> {
                selectedBranch = branch;
                bindStoreInfo();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dp(10));
            row.setLayoutParams(params);
            layoutPublicBranches.addView(row);
        }
    }

    private void openStoreMap(boolean directionsMode) {
        StoreBranch branch = selectedBranch == null ? storeRepository.defaultBranches().get(0) : selectedBranch;
        String destination = branch.getLatitude() + "," + branch.getLongitude();
        Intent intent;
        if (directionsMode) {
            intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + destination + "&travelmode=driving"));
        } else {
            intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + destination + "(" + Uri.encode(branch.getName()) + ")"));
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Thiết bị chưa mở được ứng dụng bản đồ.", Toast.LENGTH_SHORT).show();
        }
    }

    private void callStore() {
        StoreBranch branch = selectedBranch == null ? storeRepository.defaultBranches().get(0) : selectedBranch;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + branch.getPhone())));
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở ứng dụng gọi điện.", Toast.LENGTH_SHORT).show();
        }
    }

    private StoreBranch findDefaultBranch(String defaultBranchId) {
        for (StoreBranch branch : branches) {
            if (branch.getBranchId().equals(defaultBranchId)) {
                return branch;
            }
        }
        if (!branches.isEmpty()) {
            return branches.get(0);
        }
        return storeRepository.defaultBranches().get(0);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
