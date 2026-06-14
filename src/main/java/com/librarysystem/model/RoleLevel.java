package com.librarysystem.model;

public enum RoleLevel {
    NORMAL(7, 3),
    VIP(14, 5),
    ADMIN(0, 0);

    private final int maxBorrowDays;
    private final int maxActiveLoans;

    RoleLevel(int maxBorrowDays, int maxActiveLoans) {
        this.maxBorrowDays = maxBorrowDays;
        this.maxActiveLoans = maxActiveLoans;
    }

    public int getMaxBorrowDays() {
        return maxBorrowDays;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }
}
