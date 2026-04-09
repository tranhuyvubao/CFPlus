package com.example.do_an_hk1_androidstudio.local.model;

public class LocalOrderItem {
    private final String itemId;
    private final String orderId;
    private final String productId;
    private final String productName;
    private final String variantName;
    private final int qty;
    private final int unitPrice;
    private final String note;
    private final int lineTotal;
    private final String imageUrl;

    public LocalOrderItem(String itemId,
                          String orderId,
                          String productId,
                          String productName,
                          String variantName,
                          int qty,
                          int unitPrice,
                          String note,
                          int lineTotal,
                          String imageUrl) {
        this.itemId = itemId;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.variantName = variantName;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.note = note;
        this.lineTotal = lineTotal;
        this.imageUrl = imageUrl;
    }

    public String getItemId() {
        return itemId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getVariantName() {
        return variantName;
    }

    public int getQty() {
        return qty;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public String getNote() {
        return note;
    }

    public int getLineTotal() {
        return lineTotal;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
