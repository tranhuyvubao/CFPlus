package com.example.do_an_hk1_androidstudio.local.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PromoBanner {
    private final String bannerId;
    private final String title;
    private final String subtitle;
    private final String imageUrl;
    private final String actionText;
    private final String productId;
    private final List<String> productIds;
    private final int sortOrder;
    private final boolean active;

    public PromoBanner(String bannerId,
                       String title,
                       String subtitle,
                       String imageUrl,
                       String actionText,
                       String productId,
                       int sortOrder,
                       boolean active) {
        this(bannerId, title, subtitle, imageUrl, actionText, productId, singletonProductId(productId), sortOrder, active);
    }

    public PromoBanner(String bannerId,
                       String title,
                       String subtitle,
                       String imageUrl,
                       String actionText,
                       String productId,
                       List<String> productIds,
                       int sortOrder,
                       boolean active) {
        this.bannerId = bannerId;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.actionText = actionText;
        this.productId = productId;
        this.productIds = new ArrayList<>(productIds == null ? Collections.emptyList() : productIds);
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public String getBannerId() {
        return bannerId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getActionText() {
        return actionText;
    }

    public String getProductId() {
        return productId;
    }

    public List<String> getProductIds() {
        return new ArrayList<>(productIds);
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    private static List<String> singletonProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        values.add(productId.trim());
        return values;
    }
}
