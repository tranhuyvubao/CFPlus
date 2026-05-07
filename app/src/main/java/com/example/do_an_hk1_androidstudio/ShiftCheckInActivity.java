package com.example.do_an_hk1_androidstudio;

import android.Manifest;
import android.content.Context;
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
import com.example.do_an_hk1_androidstudio.cloud.StaffPushTokenRepository;
import com.example.do_an_hk1_androidstudio.cloud.StoreCloudRepository;
import com.example.do_an_hk1_androidstudio.config.CafeStoreConfig;
import com.example.do_an_hk1_androidstudio.local.LocalSessionManager;
import com.example.do_an_hk1_androidstudio.local.model.StoreBranch;
import com.example.do_an_hk1_androidstudio.ui.InsetsHelper;
import com.example.do_an_hk1_androidstudio.ui.ShiftAttendanceStateStore;
import com.example.do_an_hk1_androidstudio.ui.StaffNotificationSyncManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShiftCheckInActivity extends AppCompatActivity {

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

        long now = System.currentTimeMillis();
        long checkInAt = ShiftAttendanceStateStore.getLastCheckInAt(this);
        long shiftDurationMinutes = "check_out".equals(action) && checkInAt > 0
                ? Math.max(0, (now - checkInAt) / 60000L)
                : 0L;

        repository.logShiftAction(
                userId,
                userName,
                action,
                location.getLatitude(),
                location.getLongitude(),
                distance,
                now,
                shiftDurationMinutes
        );
        ShiftAttendanceStateStore.saveState(this, "check_in".equals(action), action, now);
        if ("check_in".equals(action)) {
            StaffNotificationSyncManager.getInstance(this).startIfNeeded();
            new StaffPushTokenRepository(this).syncForCurrentSession();
        } else {
            StaffNotificationSyncManager.getInstance(this).stop();
            new StaffPushTokenRepository(this).deactivateCurrentSessionToken();
        }
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
        boolean checkedIn = ShiftAttendanceStateStore.isCheckedIn(this);
        String lastAction = ShiftAttendanceStateStore.getLastAction(this);
        tvShiftStatus.setText(checkedIn ? "Đang trong ca" : "Chưa vào ca");
        if (tvShiftHint.getText() == null || tvShiftHint.getText().toString().trim().isEmpty()) {
            tvShiftHint.setText(lastAction);
        }
    }
}
