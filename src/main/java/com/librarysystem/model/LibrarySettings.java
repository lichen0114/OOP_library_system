package com.librarysystem.model;

public class LibrarySettings {
    private final int reminderDays;
    private final int finePerOverdueDay;

    public LibrarySettings(int reminderDays, int finePerOverdueDay) {
        this.reminderDays = reminderDays;
        this.finePerOverdueDay = finePerOverdueDay;
    }

    public int getReminderDays() {
        return reminderDays;
    }

    public int getFinePerOverdueDay() {
        return finePerOverdueDay;
    }
}
