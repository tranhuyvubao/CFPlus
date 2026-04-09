package com.example.do_an_hk1_androidstudio.local.model;

public class LocalCustomerAddress {
    private final String addressId;
    private final String customerId;
    private final String label;
    private final String recipientName;
    private final String phone;
    private final String country;
    private final String province;
    private final String district;
    private final String ward;
    private final String detailAddress;
    private final boolean isDefault;
    private final long createdAtMillis;
    private final Long updatedAtMillis;

    public LocalCustomerAddress(String addressId,
                                String customerId,
                                String label,
                                String recipientName,
                                String phone,
                                String country,
                                String province,
                                String district,
                                String ward,
                                String detailAddress,
                                boolean isDefault,
                                long createdAtMillis,
                                Long updatedAtMillis) {
        this.addressId = addressId;
        this.customerId = customerId;
        this.label = label;
        this.recipientName = recipientName;
        this.phone = phone;
        this.country = country;
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.detailAddress = detailAddress;
        this.isDefault = isDefault;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public String getAddressId() {
        return addressId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getLabel() {
        return label;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getCountry() {
        return country;
    }

    public String getProvince() {
        return province;
    }

    public String getDistrict() {
        return district;
    }

    public String getWard() {
        return ward;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public Long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public String buildDisplayAddress() {
        StringBuilder builder = new StringBuilder();
        append(builder, detailAddress);
        append(builder, ward);
        append(builder, district);
        append(builder, province);
        append(builder, country);
        return builder.toString();
    }

    private void append(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }
}
