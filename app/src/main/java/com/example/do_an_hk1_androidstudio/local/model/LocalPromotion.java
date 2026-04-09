package com.example.do_an_hk1_androidstudio.local.model;

public class LocalPromotion {
    private final String promotionId;
    private final String code;
    private final String type;
    private final double value;
    private final int minOrder;
    private final Integer maxDiscount;
    private final Long startDateMillis;
    private final Long endDateMillis;
    private final boolean active;

    public LocalPromotion(String promotionId,
                          String code,
                          String type,
                          double value,
                          int minOrder,
                          Integer maxDiscount,
                          Long startDateMillis,
                          Long endDateMillis,
                          boolean active) {
        this.promotionId = promotionId;
        this.code = code;
        this.type = type;
        this.value = value;
        this.minOrder = minOrder;
        this.maxDiscount = maxDiscount;
        this.startDateMillis = startDateMillis;
        this.endDateMillis = endDateMillis;
        this.active = active;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public int getMinOrder() {
        return minOrder;
    }

    public Integer getMaxDiscount() {
        return maxDiscount;
    }

    public Long getStartDateMillis() {
        return startDateMillis;
    }

    public Long getEndDateMillis() {
        return endDateMillis;
    }

    public boolean isActive() {
        return active;
    }
}
