package com.example.do_an_hk1_androidstudio.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.example.do_an_hk1_androidstudio.local.model.LocalUser;

public class LocalSessionManager {
    private static final String PREF_NAME = "local_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_FULL_NAME = "full_name";

    private final SharedPreferences sharedPreferences;

    public LocalSessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUser(LocalUser user) {
        sharedPreferences.edit()
                .putString(KEY_USER_ID, user.getUserId())
                .putString(KEY_EMAIL, user.getEmail())
                .putString(KEY_ROLE, user.getRole())
                .putString(KEY_FULL_NAME, user.getFullName())
                .apply();
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getCurrentUserId() != null;
    }

    @Nullable
    public String getCurrentUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    @Nullable
    public String getCurrentUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    @Nullable
    public String getCurrentUserRole() {
        return sharedPreferences.getString(KEY_ROLE, null);
    }

    @Nullable
    public String getCurrentUserFullName() {
        return sharedPreferences.getString(KEY_FULL_NAME, null);
    }
}
