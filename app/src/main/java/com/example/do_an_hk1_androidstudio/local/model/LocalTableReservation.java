package com.example.do_an_hk1_androidstudio.local.model;

public class LocalTableReservation {
    private final String reservationId;
    private final String tableId;
    private final String tableName;
    private final String customerId;
    private final String customerName;
    private final String customerPhone;
    private final long reservationTimeMillis;
    private final int guestCount;
    private final String note;
    private final String status;

    public LocalTableReservation(String reservationId,
                                 String tableId,
                                 String tableName,
                                 String customerId,
                                 String customerName,
                                 String customerPhone,
                                 long reservationTimeMillis,
                                 int guestCount,
                                 String note,
                                 String status) {
        this.reservationId = reservationId;
        this.tableId = tableId;
        this.tableName = tableName;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.reservationTimeMillis = reservationTimeMillis;
        this.guestCount = guestCount;
        this.note = note;
        this.status = status;
    }

    public String getReservationId() {
        return reservationId;
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

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public long getReservationTimeMillis() {
        return reservationTimeMillis;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public String getNote() {
        return note;
    }

    public String getStatus() {
        return status;
    }
}
