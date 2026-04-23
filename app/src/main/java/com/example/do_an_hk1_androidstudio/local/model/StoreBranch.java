package com.example.do_an_hk1_androidstudio.local.model;

public class StoreBranch {
    private final String branchId;
    private final String name;
    private final String address;
    private final String phone;
    private final String hours;
    private final double latitude;
    private final double longitude;
    private final boolean active;

    public StoreBranch(String branchId,
                       String name,
                       String address,
                       String phone,
                       String hours,
                       double latitude,
                       double longitude,
                       boolean active) {
        this.branchId = branchId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.hours = hours;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = active;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getHours() {
        return hours;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isActive() {
        return active;
    }
}
