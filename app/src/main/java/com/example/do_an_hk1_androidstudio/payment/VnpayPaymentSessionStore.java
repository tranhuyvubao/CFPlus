package com.example.do_an_hk1_androidstudio.payment;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class VnpayPaymentSessionStore {

    private static final String PREF_NAME = "vnpay_payment_session";
    private static final String KEY_ORDER_ID = "order_id";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_DISCOUNT = "discount";
    private static final String KEY_PROMO = "promo";
    private static final String KEY_TMN_CODE = "tmn_code";
    private static final String KEY_HASH_SECRET = "hash_secret";
    private static final String KEY_RETURN_URL = "return_url";

    private final SharedPreferences preferences;

    public VnpayPaymentSessionStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(String orderId,
                     int amount,
                     int discountAmount,
                     @Nullable String promoCode,
                     @Nullable String tmnCode,
                     @Nullable String hashSecret,
                     @Nullable String returnUrl) {
        preferences.edit()
                .putString(KEY_ORDER_ID, orderId)
                .putInt(KEY_AMOUNT, amount)
                .putInt(KEY_DISCOUNT, discountAmount)
                .putString(KEY_PROMO, promoCode)
                .putString(KEY_TMN_CODE, tmnCode)
                .putString(KEY_HASH_SECRET, hashSecret)
                .putString(KEY_RETURN_URL, returnUrl)
                .apply();
    }

    @Nullable
    public PendingPayment read() {
        String orderId = preferences.getString(KEY_ORDER_ID, null);
        if (orderId == null || orderId.trim().isEmpty()) {
            return null;
        }
        return new PendingPayment(
                orderId,
                preferences.getInt(KEY_AMOUNT, 0),
                preferences.getInt(KEY_DISCOUNT, 0),
                preferences.getString(KEY_PROMO, null),
                preferences.getString(KEY_TMN_CODE, null),
                preferences.getString(KEY_HASH_SECRET, null),
                preferences.getString(KEY_RETURN_URL, null)
        );
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    public static class PendingPayment {
        public final String orderId;
        public final int amount;
        public final int discountAmount;
        @Nullable
        public final String promoCode;
        @Nullable
        public final String tmnCode;
        @Nullable
        public final String hashSecret;
        @Nullable
        public final String returnUrl;

        public PendingPayment(String orderId,
                              int amount,
                              int discountAmount,
                              @Nullable String promoCode,
                              @Nullable String tmnCode,
                              @Nullable String hashSecret,
                              @Nullable String returnUrl) {
            this.orderId = orderId;
            this.amount = amount;
            this.discountAmount = discountAmount;
            this.promoCode = promoCode;
            this.tmnCode = tmnCode;
            this.hashSecret = hashSecret;
            this.returnUrl = returnUrl;
        }

        public boolean hasMerchantConfig() {
            return tmnCode != null && !tmnCode.trim().isEmpty()
                    && hashSecret != null && !hashSecret.trim().isEmpty()
                    && returnUrl != null && !returnUrl.trim().isEmpty();
        }
    }
}
