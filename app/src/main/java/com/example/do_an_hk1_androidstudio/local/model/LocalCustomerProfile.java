package com.example.do_an_hk1_androidstudio.local.model;

public class LocalCustomerProfile {
    private final String customerId;
    private final int loyaltyPoint;
    private final Long birthdayMillis;
    private final String gender;
    private final long createdAtMillis;
    private final Long updatedAtMillis;

    public LocalCustomerProfile(String customerId,
                                int loyaltyPoint,
                                Long birthdayMillis,
                                String gender,
                                long createdAtMillis,
                                Long updatedAtMillis) {
        this.customerId = customerId;
        this.loyaltyPoint = loyaltyPoint;
        this.birthdayMillis = birthdayMillis;
        this.gender = gender;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getLoyaltyPoint() {
        return loyaltyPoint;
    }

    public Long getBirthdayMillis() {
        return birthdayMillis;
    }

    public String getGender() {
        return gender;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public Long getUpdatedAtMillis() {
        return updatedAtMillis;
    }
}
