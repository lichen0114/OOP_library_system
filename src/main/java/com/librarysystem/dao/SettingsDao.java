package com.librarysystem.dao;

import com.librarysystem.model.LibrarySettings;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsDao {
    public LibrarySettings getSettings() throws SQLException {
        return new LibrarySettings(
                getInt("reminder_days", 3),
                getInt("fine_per_overdue_day", 5)
        );
    }

    public int getInt(String key, int defaultValue) throws SQLException {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    try {
                        return Integer.parseInt(resultSet.getString("setting_value"));
                    } catch (NumberFormatException ignored) {
                        return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }

    public void updateSettings(int reminderDays, int finePerOverdueDay) throws SQLException {
        upsert("reminder_days", String.valueOf(reminderDays));
        upsert("fine_per_overdue_day", String.valueOf(finePerOverdueDay));
    }

    private void upsert(String key, String value) throws SQLException {
        String sql = """
                INSERT INTO app_settings (setting_key, setting_value)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }
}
