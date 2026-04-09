package com.example.do_an_hk1_androidstudio.local.model;

public class LocalProduct {
    private final String productId;
    private final String categoryId;
    private final String name;
    private final int basePrice;
    private final String imageUrl;
    private final boolean active;

    public LocalProduct(String productId,
                        String categoryId,
                        String name,
                        int basePrice,
                        String imageUrl,
                        boolean active) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.name = name;
        this.basePrice = basePrice;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    public String getProductId() {
        return productId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }
}
