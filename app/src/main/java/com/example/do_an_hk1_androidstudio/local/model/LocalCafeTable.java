package com.example.do_an_hk1_androidstudio.local.model;

public class LocalCafeTable {
    private final String tableId;
    private final String name;
    private final String code;
    private final String area;
    private final String status;
    private final boolean active;

    public LocalCafeTable(String tableId, String name, String code, String area, String status, boolean active) {
        this.tableId = tableId;
        this.name = name;
        this.code = code;
        this.area = area;
        this.status = status;
        this.active = active;
    }

    public String getTableId() {
        return tableId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getArea() {
        return area;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }
}
