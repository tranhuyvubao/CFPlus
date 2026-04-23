package com.example.do_an_hk1_androidstudio.cloud;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.do_an_hk1_androidstudio.local.DataHelper;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ShiftAttendanceRepository {

    private final Context appContext;
    private final FirebaseFirestore firestore;

    public ShiftAttendanceRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseProvider.getFirestore(appContext);
    }

    public void logShiftAction(@NonNull String userId,
                               @NonNull String userName,
                               @NonNull String action,
                               double latitude,
                               double longitude,
                               float distanceMeters) {
        FirebaseProvider.ensureAuthenticated(appContext, (success, message) -> {
            if (!success) {
                return;
            }
            String id = DataHelper.newId("shift");
            Map<String, Object> values = new HashMap<>();
            values.put("shift_log_id", id);
            values.put("user_id", userId);
            values.put("user_name", userName);
            values.put("action", action);
            values.put("latitude", latitude);
            values.put("longitude", longitude);
            values.put("distance_meters", distanceMeters);
            values.put("created_at", System.currentTimeMillis());
            firestore.collection("shift_attendance_logs").document(id).set(values);
        });
    }
}
