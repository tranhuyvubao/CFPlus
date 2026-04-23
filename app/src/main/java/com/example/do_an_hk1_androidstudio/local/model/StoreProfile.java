package com.example.do_an_hk1_androidstudio.local.model;

public class StoreProfile {
    private final String storeName;
    private final String tagline;
    private final String logoUrl;
    private final String email;
    private final String defaultBranchId;

    public StoreProfile(String storeName,
                        String tagline,
                        String logoUrl,
                        String email,
                        String defaultBranchId) {
        this.storeName = storeName;
        this.tagline = tagline;
        this.logoUrl = logoUrl;
        this.email = email;
        this.defaultBranchId = defaultBranchId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getTagline() {
        return tagline;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getDefaultBranchId() {
        return defaultBranchId;
    }
}
