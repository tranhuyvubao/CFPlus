package com.example.do_an_hk1_androidstudio.ui;

import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormatter {

    private static final NumberFormat COMMA_FORMATTER = NumberFormat.getIntegerInstance(Locale.US);

    private MoneyFormatter() {
    }

    public static String format(int amount) {
        return COMMA_FORMATTER.format(amount) + "đ";
    }

    public static String format(long amount) {
        return COMMA_FORMATTER.format(amount) + "đ";
    }
}
