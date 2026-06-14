package com.librarysystem.model;

import java.time.LocalDateTime;

public class User {
    private final int userId;
    private final String studentNo;
    private final String name;
    private final String passwordHash;
    private final RoleLevel roleLevel;
    private final LocalDateTime createdAt;
    private final UserStatus status;

    public User(int userId, String studentNo, String name, String passwordHash,
                RoleLevel roleLevel, LocalDateTime createdAt, UserStatus status) {
        this.userId = userId;
        this.studentNo = studentNo;
        this.name = name;
        this.passwordHash = passwordHash;
        this.roleLevel = roleLevel;
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

    public RoleLevel getRoleLevel() {
        return roleLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isAdmin() {
        return roleLevel == RoleLevel.ADMIN;
    }

    public boolean isSuspended() {
        return status == UserStatus.SUSPENDED;
    }
}
