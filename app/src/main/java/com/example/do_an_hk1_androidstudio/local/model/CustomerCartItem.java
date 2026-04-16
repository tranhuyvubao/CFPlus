package com.example.do_an_hk1_androidstudio.local.model;

import androidx.annotation.Nullable;

public class CustomerCartItem {
    private final String cartItemId;
    private final String productId;
    private final String productName;
    private final int unitPrice;
    private final int quantity;
    @Nullable
    private final String size;
    @Nullable
    private final String iceLevel;
    @Nullable
    private final String note;
    @Nullable
    private final String imageUrl;

    public CustomerCartItem(String cartItemId,
                            String productId,
                            String productName,
                            int unitPrice,
                            int quantity,
                            @Nullable String size,
                            @Nullable String iceLevel,
                            @Nullable String note,
                            @Nullable String imageUrl) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.size = size;
        this.iceLevel = iceLevel;
        this.note = note;
        this.imageUrl = imageUrl;
    }

    public String getCartItemId() {
        return cartItemId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    @Nullable
    public String getSize() {
        return size;
    }

    @Nullable
    public String getIceLevel() {
        return iceLevel;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public int getLineTotal() {
        return unitPrice * quantity;
    }

    public String buildVariantLabel() {
        StringBuilder builder = new StringBuilder();
        if (size != null && !size.trim().isEmpty()) {
            builder.append("Size ").append(size.trim().toUpperCase());
        }
        if (iceLevel != null && !iceLevel.trim().isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append(iceLevel.trim());
        }
        return builder.toString();
    }
}
