package com.librarysystem.model;

import java.time.LocalDateTime;

public class User {
    private final int userId;
    private final String studentNo;
    private final String name;
    private final String passwordHash;
    private final UserLevel userLevel;
    private final LocalDateTime createdAt;
    private final UserStatus status;

    public User(int userId, String studentNo, String name, String passwordHash,
                UserLevel userLevel, LocalDateTime createdAt, UserStatus status) {
        this.userId = userId;
        this.studentNo = studentNo;
        this.name = name;
        this.passwordHash = passwordHash;
        this.userLevel = userLevel;
        this.createdAt = createdAt;
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserLevel getUserLevel() {
        return userLevel;
    }

    public String getLevelCode() {
        return userLevel.getCode();
    }

    public String getLevelDisplayName() {
        return userLevel.getDisplayName();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isAdmin() {
        return userLevel.isAdmin();
    }

    public boolean isSuspended() {
        return status == UserStatus.SUSPENDED;
    }
}
