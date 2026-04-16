package com.example.do_an_hk1_androidstudio.payment;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class VnpayUtils {

    private static final TimeZone VN_TIME_ZONE = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    private VnpayUtils() {
    }

    public static String buildPaymentUrl(String txnRef,
                                         int amount,
                                         @Nullable String orderInfo,
                                         String tmnCode,
                                         String hashSecret,
                                         String returnUrl) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", VnpayConfig.VERSION);
        params.put("vnp_Command", VnpayConfig.COMMAND);
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(Math.max(0, amount) * 100L));
        params.put("vnp_CurrCode", VnpayConfig.CURR_CODE);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo == null || orderInfo.trim().isEmpty() ? "Thanh toán CFPLUS" : orderInfo.trim());
        params.put("vnp_OrderType", VnpayConfig.ORDER_TYPE);
        params.put("vnp_Locale", VnpayConfig.LOCALE);
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_CreateDate", formatDate(new Date()));
        params.put("vnp_ExpireDate", formatExpireDate());
        params.put("vnp_IpAddr", "127.0.0.1");

        String signData = buildSignedData(params);
        String secureHash = hmacSha512(hashSecret, signData);
        String queryToSend = buildEncodedQuery(params);
        return VnpayConfig.SANDBOX_PAYMENT_URL + "?" + queryToSend + "&vnp_SecureHash=" + secureHash;
    }

    public static boolean isValidReturnSignature(Uri data, String hashSecret) {
        String receivedHash = data.getQueryParameter("vnp_SecureHash");
        if (receivedHash == null || receivedHash.trim().isEmpty()) {
            return false;
        }

        Map<String, String> params = new LinkedHashMap<>();
        for (String key : data.getQueryParameterNames()) {
            if (!key.startsWith("vnp_")) {
                continue;
            }
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            params.put(key, safe(data.getQueryParameter(key)));
        }
        String expected = hmacSha512(hashSecret, buildSignedData(params));
        return expected.equalsIgnoreCase(receivedHash);
    }

    public static String readQuery(Uri data, String key) {
        return safe(data.getQueryParameter(key));
    }

    private static String buildSignedData(Map<String, String> params) {
        return buildQuery(params, false);
    }

    private static String buildEncodedQuery(Map<String, String> params) {
        return buildQuery(params, true);
    }

    private static String buildQuery(Map<String, String> params, boolean encodeFieldName) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            String value = safe(params.get(key));
            if (value.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(encodeFieldName ? urlEncode(key) : key);
            builder.append('=');
            builder.append(urlEncode(value));
        }
        return builder.toString();
    }

    private static String hmacSha512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                hash.append(String.format(Locale.US, "%02x", item));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPAY", e);
        }
    }

    private static String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        formatter.setTimeZone(VN_TIME_ZONE);
        return formatter.format(date);
    }

    private static String formatExpireDate() {
        java.util.Calendar calendar = java.util.Calendar.getInstance(VN_TIME_ZONE, Locale.US);
        calendar.add(java.util.Calendar.MINUTE, 15);
        return formatDate(calendar.getTime());
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }
}
