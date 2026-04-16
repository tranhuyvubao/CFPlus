package com.example.do_an_hk1_androidstudio.payment;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

public class VnpaySettingsStore {

    private static final String PREF_NAME = "vnpay_settings";
    private static final String KEY_TMN_CODE = "tmn_code";
    private static final String KEY_HASH_SECRET = "hash_secret";
    private static final String KEY_RETURN_URL = "return_url";

    private final SharedPreferences preferences;

    public VnpaySettingsStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(String tmnCode, String hashSecret, @Nullable String returnUrl) {
        preferences.edit()
                .putString(KEY_TMN_CODE, tmnCode == null ? "" : tmnCode.trim())
                .putString(KEY_HASH_SECRET, hashSecret == null ? "" : hashSecret.trim())
                .putString(KEY_RETURN_URL, returnUrl == null ? VnpayConfig.DEFAULT_RETURN_URL : returnUrl.trim())
                .apply();
    }

    public MerchantConfig read() {
        return new MerchantConfig(
                preferences.getString(KEY_TMN_CODE, ""),
                preferences.getString(KEY_HASH_SECRET, ""),
                preferences.getString(KEY_RETURN_URL, VnpayConfig.DEFAULT_RETURN_URL)
        );
    }

    public boolean isConfigured() {
        MerchantConfig config = read();
        return !TextUtils.isEmpty(config.tmnCode) && !TextUtils.isEmpty(config.hashSecret);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    public static class MerchantConfig {
        public final String tmnCode;
        public final String hashSecret;
        public final String returnUrl;

        public MerchantConfig(@Nullable String tmnCode, @Nullable String hashSecret, @Nullable String returnUrl) {
            this.tmnCode = tmnCode == null ? "" : tmnCode;
            this.hashSecret = hashSecret == null ? "" : hashSecret;
            this.returnUrl = returnUrl == null ? VnpayConfig.DEFAULT_RETURN_URL : returnUrl;
        }
    }
}
