package com.librarysystem.service;

import com.librarysystem.dao.SettingsDao;
import com.librarysystem.model.LibrarySettings;

import java.sql.SQLException;

public class SettingsService {
    private final SettingsDao settingsDao = new SettingsDao();

    public LibrarySettings getSettings() throws SQLException {
        return settingsDao.getSettings();
    }

    public void updateSettings(int reminderDays, int finePerOverdueDay) throws SQLException {
        if (reminderDays < 0) {
            throw new IllegalArgumentException("Reminder days cannot be negative.");
        }
        if (finePerOverdueDay < 0) {
            throw new IllegalArgumentException("Fine per overdue day cannot be negative.");
        }
        settingsDao.updateSettings(reminderDays, finePerOverdueDay);
    }
}
