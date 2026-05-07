package com.example.do_an_hk1_androidstudio.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class ShiftAttendanceStateStore {
    private static final String PREF_NAME = "shift_attendance";
    private static final String KEY_CHECKED_IN = "checked_in";
    private static final String KEY_LAST_ACTION = "last_action";
    private static final String KEY_LAST_CHECK_IN_AT = "last_check_in_at";

    private ShiftAttendanceStateStore() {
    }

    public static boolean isCheckedIn(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_CHECKED_IN, false);
    }

    public static long getLastCheckInAt(@NonNull Context context) {
        return prefs(context).getLong(KEY_LAST_CHECK_IN_AT, 0L);
    }

    public static String getLastAction(@NonNull Context context) {
        return prefs(context).getString(KEY_LAST_ACTION, "Chưa có thao tác");
    }

    public static void saveState(@NonNull Context context, boolean checkedIn, @NonNull String action, long timestampMillis) {
        SharedPreferences.Editor editor = prefs(context).edit()
                .putBoolean(KEY_CHECKED_IN, checkedIn)
                .putString(KEY_LAST_ACTION, "check_in".equals(action) ? "Đã check-in" : "Đã check-out");
        if ("check_in".equals(action)) {
            editor.putLong(KEY_LAST_CHECK_IN_AT, timestampMillis);
        }
        editor.apply();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
