package com.example.do_an_hk1_androidstudio.ui;

import androidx.annotation.NonNull;

import com.example.do_an_hk1_androidstudio.local.model.LocalOrder;

import java.util.List;

public final class MembershipTierHelper {

    private MembershipTierHelper() {
    }

    public enum Tier {
        DONG("Đồng", "Khởi đầu cho hành trình thưởng thức", 0xFFEED9C5L, 0xFFB88553L),
        BAC("Bạc", "Ưu đãi nhiều hơn cho khách quen", 0xFFE6EDF7L, 0xFF8BA1B8L),
        VANG("Vàng", "Đặc quyền tốt hơn mỗi lần ghé quán", 0xFFFBE8A5L, 0xFFD8A933L),
        KIM_CUONG("Kim cương", "Trải nghiệm premium dành cho khách VIP", 0xFFD7E8FFL, 0xFF6F8DD9L);

        public final String label;
        public final String subtitle;
        public final long startColor;
        public final long endColor;

        Tier(String label, String subtitle, long startColor, long endColor) {
            this.label = label;
            this.subtitle = subtitle;
            this.startColor = startColor;
            this.endColor = endColor;
        }
    }

    public static class Summary {
        public final Tier tier;
        public final int paidOrderCount;
        public final int totalSpent;
        public final int loyaltyPoint;

        public Summary(Tier tier, int paidOrderCount, int totalSpent, int loyaltyPoint) {
            this.tier = tier;
            this.paidOrderCount = paidOrderCount;
            this.totalSpent = totalSpent;
            this.loyaltyPoint = loyaltyPoint;
        }
    }

    @NonNull
    public static Summary buildSummary(@NonNull List<LocalOrder> orders, int loyaltyPoint) {
        int paidOrders = 0;
        int totalSpent = 0;
        for (LocalOrder order : orders) {
            if ("paid".equals(order.getStatus())) {
                paidOrders++;
                totalSpent += order.getTotal();
            }
        }
        return new Summary(resolveTier(paidOrders, totalSpent, loyaltyPoint), paidOrders, totalSpent, loyaltyPoint);
    }

    @NonNull
    public static Tier resolveTier(int paidOrderCount, int totalSpent, int loyaltyPoint) {
        if (paidOrderCount >= 20 || totalSpent >= 2_500_000 || loyaltyPoint >= 900) {
            return Tier.KIM_CUONG;
        }
        if (paidOrderCount >= 12 || totalSpent >= 1_200_000 || loyaltyPoint >= 450) {
            return Tier.VANG;
        }
        if (paidOrderCount >= 5 || totalSpent >= 500_000 || loyaltyPoint >= 150) {
            return Tier.BAC;
        }
        return Tier.DONG;
    }
}
