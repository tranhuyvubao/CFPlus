package com.example.do_an_hk1_androidstudio.local.model;

public class LocalIngredient {
    private final String ingredientId;
    private final String name;
    private final String unit;
    private final double currentQty;
    private final double minStock;
    private final boolean active;

    public LocalIngredient(String ingredientId,
                           String name,
                           String unit,
                           double currentQty,
                           double minStock,
                           boolean active) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.unit = unit;
        this.currentQty = currentQty;
        this.minStock = minStock;
        this.active = active;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public double getCurrentQty() {
        return currentQty;
    }

    public double getMinStock() {
        return minStock;
    }

    public boolean isActive() {
        return active;
    }
}
