package com.example.do_an_hk1_androidstudio.payment;

public final class VnpayConfig {

    public static final String SANDBOX_PAYMENT_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VERSION = "2.1.0";
    public static final String COMMAND = "pay";
    public static final String CURR_CODE = "VND";
    public static final String LOCALE = "vn";
    public static final String ORDER_TYPE = "other";
    public static final String DEFAULT_RETURN_URL = "https://cafeplus-1fd32.web.app/vnpay-return.html";

    public static final String DEFAULT_TMN_CODE = "";
    public static final String DEFAULT_HASH_SECRET = "";

    private VnpayConfig() {
    }

}
