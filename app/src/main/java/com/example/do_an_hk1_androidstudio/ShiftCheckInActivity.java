package com.example.do_an_hk1_androidstudio;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.do_an_hk1_androidstudio.cloud.ShiftAttendanceRepository;
import com.example.do_an_hk1_androidstudio.cloud.StoreCloudRepository;
import com.example.do_an_hk1_androidstudio.config.CafeStoreConfig;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.StoreBranch;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShiftCheckInActivity extends AppCompatActivity {

    private static final String PREF_NAME = "shift_attendance";
    private static final String KEY_CHECKED_IN = "checked_in";
    private static final String KEY_LAST_ACTION = "last_action";

    private TextView tvShiftStatus;
    private TextView tvShiftHint;
    private ShiftAttendanceRepository repository;
    private StoreCloudRepository storeRepository;
    private LocalSessionManager sessionManager;
    private ListenerRegistration profileListener;
    private ListenerRegistration branchesListener;
    private final List<StoreBranch> activeBranches = new ArrayList<>();
    private StoreBranch targetBranch;
    private String defaultBranchId = "main";
    private ActivityResultLauncher<String> permissionLauncher;
    private String pendingAction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shift_check_in);
        InsetsHelper.applyStatusBarPadding(findViewById(R.id.headerShiftAttendance));
        InsetsHelper.applyNavigationBarPadding(findViewById(R.id.rootShiftAttendance));

        repository = new ShiftAttendanceRepository(this);
        storeRepository = new StoreCloudRepository(this);
        sessionManager = new LocalSessionManager(this);
        targetBranch = storeRepository.defaultBranches().get(0);
        tvShiftStatus = findViewById(R.id.tvShiftStatus);
        tvShiftHint = findViewById(R.id.tvShiftHint);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted && pendingAction != null) {
                        handleShiftAction(pendingAction);
                    } else {
                        Toast.makeText(this, "Cần quyền vị trí để chấm công.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.tvShiftBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCheckIn).setOnClickListener(v -> requestOrRun("check_in"));
        findViewById(R.id.btnCheckOut).setOnClickListener(v -> requestOrRun("check_out"));
        listenStoreBranches();
        renderState();
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

    private void listenStoreBranches() {
        profileListener = storeRepository.listenStoreProfile(profile -> {
            defaultBranchId = profile.getDefaultBranchId();
            updateTargetBranch();
        });
        branchesListener = storeRepository.listenBranches(branches -> {
            activeBranches.clear();
            for (StoreBranch branch : branches) {
                if (branch.isActive()) {
                    activeBranches.add(branch);
                }
            }
            updateTargetBranch();
        });
    }

    private void updateTargetBranch() {
        for (StoreBranch branch : activeBranches) {
            if (branch.getBranchId().equals(defaultBranchId)) {
                targetBranch = branch;
                return;
            }
        }
        if (!activeBranches.isEmpty()) {
            targetBranch = activeBranches.get(0);
        }
    }

    private void requestOrRun(String action) {
        pendingAction = action;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            handleShiftAction(action);
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void handleShiftAction(String action) {
        Location location = getBestLastKnownLocation();
        if (location == null) {
            Toast.makeText(this, "Chưa lấy được vị trí hiện tại. Hãy bật GPS rồi thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        StoreBranch branch = targetBranch == null ? storeRepository.defaultBranches().get(0) : targetBranch;
        float[] result = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), branch.getLatitude(), branch.getLongitude(), result);
        float distance = result[0];
        if (distance > CafeStoreConfig.ATTENDANCE_RADIUS_METERS) {
            tvShiftHint.setText("Bạn đang cách " + branch.getName() + " khoảng " + Math.round(distance) + "m, cần trong bán kính 50m.");
            Toast.makeText(this, "Bạn đang ở ngoài bán kính cho phép.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getCurrentUserId();
        String userName = sessionManager.getCurrentUserFullName();
        if (userId == null) {
            userId = "unknown_user";
        }
        if (userName == null || userName.trim().isEmpty()) {
            userName = sessionManager.getCurrentUserEmail();
        }
        if (userName == null || userName.trim().isEmpty()) {
            userName = "Nhân viên quán";
        }

        repository.logShiftAction(userId, userName, action, location.getLatitude(), location.getLongitude(), distance);
        saveState("check_in".equals(action), action);
        tvShiftHint.setText(("check_in".equals(action) ? "Check-in" : "Check-out")
                + " thành công tại "
                + branch.getName()
                + " lúc "
                + new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(new Date()));
        renderState();
    }

    @Nullable
    private Location getBestLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }
        Location best = null;
        for (String provider : locationManager.getProviders(true)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null && (best == null || location.getAccuracy() < best.getAccuracy())) {
                best = location;
            }
        }
        return best;
    }

    private void renderState() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean checkedIn = preferences.getBoolean(KEY_CHECKED_IN, false);
        String lastAction = preferences.getString(KEY_LAST_ACTION, "Chưa có thao tác");
        tvShiftStatus.setText(checkedIn ? "Đang trong ca" : "Chưa vào ca");
        if (tvShiftHint.getText() == null || tvShiftHint.getText().toString().trim().isEmpty()) {
            tvShiftHint.setText(lastAction);
        }
    }

    private void saveState(boolean checkedIn, String action) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CHECKED_IN, checkedIn)
                .putString(KEY_LAST_ACTION, ("check_in".equals(action) ? "Đã check-in" : "Đã check-out"))
                .apply();
    }
}
