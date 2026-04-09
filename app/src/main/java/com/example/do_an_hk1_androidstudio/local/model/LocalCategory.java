package com.example.do_an_hk1_androidstudio.local.model;

public class LocalCategory {
    private final String categoryId;
    private final String name;
    private final String imageUrl;
    private final boolean active;

    public LocalCategory(String categoryId, String name, String imageUrl, boolean active) {
        this.categoryId = categoryId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }
}
