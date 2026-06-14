package com.librarysystem.dao;

import com.librarysystem.model.UserLevel;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LevelDao {
    public List<UserLevel> listAll() throws SQLException {
        String sql = """
                SELECT *
                FROM user_levels
                ORDER BY is_admin DESC, custom_level, level_code
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapLevels(resultSet);
        }
    }

    public List<UserLevel> listAssignable() throws SQLException {
        String sql = """
                SELECT *
                FROM user_levels
                WHERE is_admin = FALSE
                ORDER BY custom_level, level_code
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapLevels(resultSet);
        }
    }

    public List<UserLevel> listRegistrationAllowed() throws SQLException {
        String sql = """
                SELECT *
                FROM user_levels
                WHERE registration_allowed = TRUE AND is_admin = FALSE
                ORDER BY custom_level, level_code
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapLevels(resultSet);
        }
    }

    public Optional<UserLevel> findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM user_levels WHERE level_code = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapLevel(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public void createCustomLevel(UserLevel level) throws SQLException {
        String sql = """
                INSERT INTO user_levels
                    (level_code, display_name, max_borrow_days, max_active_loans,
                     can_favorite, is_admin, registration_allowed, custom_level)
                VALUES (?, ?, ?, ?, ?, FALSE, ?, TRUE)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEditableLevel(statement, level);
            statement.setBoolean(6, level.isRegistrationAllowed());
            statement.executeUpdate();
        }
    }

    public void updateCustomLevel(UserLevel level) throws SQLException {
        String sql = """
                UPDATE user_levels
                SET display_name = ?,
                    max_borrow_days = ?,
                    max_active_loans = ?,
                    can_favorite = ?,
                    registration_allowed = ?
                WHERE level_code = ? AND custom_level = TRUE AND is_admin = FALSE
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, level.getDisplayName());
            statement.setInt(2, level.getMaxBorrowDays());
            statement.setInt(3, level.getMaxActiveLoans());
            statement.setBoolean(4, level.canFavorite());
            statement.setBoolean(5, level.isRegistrationAllowed());
            statement.setString(6, level.getCode());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Only custom non-admin levels can be edited.");
            }
        }
    }

    private void bindEditableLevel(PreparedStatement statement, UserLevel level) throws SQLException {
        statement.setString(1, level.getCode());
        statement.setString(2, level.getDisplayName());
        statement.setInt(3, level.getMaxBorrowDays());
        statement.setInt(4, level.getMaxActiveLoans());
        statement.setBoolean(5, level.canFavorite());
    }

    private List<UserLevel> mapLevels(ResultSet resultSet) throws SQLException {
        List<UserLevel> levels = new ArrayList<>();
        while (resultSet.next()) {
            levels.add(mapLevel(resultSet));
        }
        return levels;
    }

    private UserLevel mapLevel(ResultSet resultSet) throws SQLException {
        return new UserLevel(
                resultSet.getString("level_code"),
                resultSet.getString("display_name"),
                resultSet.getInt("max_borrow_days"),
                resultSet.getInt("max_active_loans"),
                resultSet.getBoolean("can_favorite"),
                resultSet.getBoolean("is_admin"),
                resultSet.getBoolean("registration_allowed"),
                resultSet.getBoolean("custom_level")
        );
    }
}
