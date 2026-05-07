package com.example.do_an_hk1_androidstudio.local.model;

import com.example.do_an_hk1_androidstudio.local.DataHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalOrder {
    private final String orderId;
    private final String orderCode;
    private final String orderType;
    private final String orderChannel;
    private final String tableId;
    private final String tableName;
    private final String customerId;
    private final String staffId;
    private final String status;
    private final int subtotal;
    private final int discountAmount;
    private final int total;
    private final String deliveryAddressText;
    private final long createdAtMillis;
    private final boolean needsStaffAttention;
    private final int lastCustomerItemAddedQty;
    private final long lastCustomerItemAddedAtMillis;
    private final List<LocalOrderItem> lastCustomerAddedItems;
    private final String supportRequestNote;
    private final List<LocalOrderItem> items;

    public LocalOrder(String orderId,
                      String orderCode,
                      String orderType,
                      String orderChannel,
                      String tableId,
                      String tableName,
                      String customerId,
                      String staffId,
                      String status,
                      int subtotal,
                      int discountAmount,
                      int total,
                      String deliveryAddressText,
                      long createdAtMillis,
                      List<LocalOrderItem> items) {
        this(orderId,
                orderCode,
                orderType,
                orderChannel,
                tableId,
                tableName,
                customerId,
                staffId,
                status,
                subtotal,
                discountAmount,
                total,
                deliveryAddressText,
                createdAtMillis,
                false,
                0,
                0L,
                null,
                null,
                items);
    }

    public LocalOrder(String orderId,
                      String orderCode,
                      String orderType,
                      String orderChannel,
                      String tableId,
                      String tableName,
                      String customerId,
                      String staffId,
                      String status,
                      int subtotal,
                      int discountAmount,
                      int total,
                      String deliveryAddressText,
                      long createdAtMillis,
                      boolean needsStaffAttention,
                      int lastCustomerItemAddedQty,
                      long lastCustomerItemAddedAtMillis,
                      List<LocalOrderItem> lastCustomerAddedItems,
                      String supportRequestNote,
                      List<LocalOrderItem> items) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.orderType = orderType;
        this.orderChannel = orderChannel;
        this.tableId = tableId;
        this.tableName = tableName;
        this.customerId = customerId;
        this.staffId = staffId;
        this.status = status;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.total = total;
        this.deliveryAddressText = deliveryAddressText;
        this.createdAtMillis = createdAtMillis;
        this.needsStaffAttention = needsStaffAttention;
        this.lastCustomerItemAddedQty = lastCustomerItemAddedQty;
        this.lastCustomerItemAddedAtMillis = lastCustomerItemAddedAtMillis;
        this.lastCustomerAddedItems = lastCustomerAddedItems == null ? new ArrayList<>() : new ArrayList<>(lastCustomerAddedItems);
        this.supportRequestNote = supportRequestNote;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getDisplayOrderCode() {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            return DataHelper.newOrderCode(createdAtMillis);
        }
        if (orderCode.startsWith("web_order_")
                || orderCode.startsWith("cloud_order_")
                || orderCode.startsWith("order_")) {
            return DataHelper.newOrderCode(createdAtMillis);
        }
        return orderCode;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getOrderChannel() {
        return orderChannel;
    }

    public String getTableId() {
        return tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStaffId() {
        return staffId;
    }

    public String getStatus() {
        return status;
    }

    public int getSubtotal() {
        return subtotal;
    }

    public int getDiscountAmount() {
        return discountAmount;
    }

    public int getTotal() {
        return total;
    }

    public String getDeliveryAddressText() {
        return deliveryAddressText;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean needsStaffAttention() {
        return needsStaffAttention;
    }

    public int getLastCustomerItemAddedQty() {
        return lastCustomerItemAddedQty;
    }

    public long getLastCustomerItemAddedAtMillis() {
        return lastCustomerItemAddedAtMillis;
    }

    public List<LocalOrderItem> getLastCustomerAddedItems() {
        return Collections.unmodifiableList(lastCustomerAddedItems);
    }

    public String getSupportRequestNote() {
        return supportRequestNote;
    }

    public List<LocalOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
