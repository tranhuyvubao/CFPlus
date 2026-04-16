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

    private final SharedPreferences preferences;

    public VnpayPaymentSessionStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(String orderId, int amount, int discountAmount, @Nullable String promoCode) {
        preferences.edit()
                .putString(KEY_ORDER_ID, orderId)
                .putInt(KEY_AMOUNT, amount)
                .putInt(KEY_DISCOUNT, discountAmount)
                .putString(KEY_PROMO, promoCode)
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
                preferences.getString(KEY_PROMO, null)
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

        public PendingPayment(String orderId, int amount, int discountAmount, @Nullable String promoCode) {
            this.orderId = orderId;
            this.amount = amount;
            this.discountAmount = discountAmount;
            this.promoCode = promoCode;
        }
    }
}
