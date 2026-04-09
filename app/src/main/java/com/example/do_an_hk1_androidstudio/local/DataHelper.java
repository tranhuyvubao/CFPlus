package com.example.do_an_hk1_androidstudio.local;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

public final class DataHelper {

    private DataHelper() {
    }

    public static String sha256(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash password", e);
        }
    }

    public static String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String newOrderCode() {
        return newOrderCode(System.currentTimeMillis());
    }

    public static String newOrderCode(long timestampMillis) {
        return new SimpleDateFormat("HHmmssddMMyyyy", Locale.getDefault()).format(timestampMillis);
    }
}
