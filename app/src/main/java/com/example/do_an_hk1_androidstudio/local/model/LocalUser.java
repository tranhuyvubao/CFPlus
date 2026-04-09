package com.example.do_an_hk1_androidstudio.local.model;

public class LocalUser {
    private final String userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final String phone;
    private final String role;
    private final String gender;
    private final boolean active;
    private final Long birthdayMillis;
    private final long createdAtMillis;
    private final Long updatedAtMillis;

    public LocalUser(String userId,
                     String username,
                     String email,
                     String fullName,
                     String phone,
                     String role,
                     String gender,
                     boolean active,
                     Long birthdayMillis,
                     long createdAtMillis,
                     Long updatedAtMillis) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.gender = gender;
        this.active = active;
        this.birthdayMillis = birthdayMillis;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getGender() {
        return gender;
    }

    public boolean isActive() {
        return active;
    }

    public Long getBirthdayMillis() {
        return birthdayMillis;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public Long getUpdatedAtMillis() {
        return updatedAtMillis;
    }
}
