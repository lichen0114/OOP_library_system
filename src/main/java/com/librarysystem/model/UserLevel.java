package com.librarysystem.model;

public class UserLevel {
    private final String code;
    private final String displayName;
    private final int maxBorrowDays;
    private final int maxActiveLoans;
    private final boolean canFavorite;
    private final boolean admin;
    private final boolean registrationAllowed;
    private final boolean customLevel;

    public UserLevel(String code, String displayName, int maxBorrowDays, int maxActiveLoans,
                     boolean canFavorite, boolean admin, boolean registrationAllowed, boolean customLevel) {
        this.code = code;
        this.displayName = displayName;
        this.maxBorrowDays = maxBorrowDays;
        this.maxActiveLoans = maxActiveLoans;
        this.canFavorite = canFavorite;
        this.admin = admin;
        this.registrationAllowed = registrationAllowed;
        this.customLevel = customLevel;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxBorrowDays() {
        return maxBorrowDays;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }

    public boolean canFavorite() {
        return canFavorite;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public boolean isCustomLevel() {
        return customLevel;
    }

    public String getSummary() {
        return displayName + " (" + code + ", " + maxBorrowDays + " days, "
                + maxActiveLoans + " loans" + (canFavorite ? ", favorites" : "") + ")";
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
