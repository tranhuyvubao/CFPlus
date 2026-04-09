package com.example.do_an_hk1_androidstudio.local.model;

public class LocalStockTransaction {
    private final String transactionId;
    private final String ingredientId;
    private final String type;
    private final double qty;
    private final String note;
    private final String staffId;
    private final long createdAtMillis;

    public LocalStockTransaction(String transactionId,
                                 String ingredientId,
                                 String type,
                                 double qty,
                                 String note,
                                 String staffId,
                                 long createdAtMillis) {
        this.transactionId = transactionId;
        this.ingredientId = ingredientId;
        this.type = type;
        this.qty = qty;
        this.note = note;
        this.staffId = staffId;
        this.createdAtMillis = createdAtMillis;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public String getType() {
        return type;
    }

    public double getQty() {
        return qty;
    }

    public String getNote() {
        return note;
    }

    public String getStaffId() {
        return staffId;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}
