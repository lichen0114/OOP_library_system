package com.librarysystem.dao;

import com.librarysystem.model.User;
import com.librarysystem.model.UserLevel;
import com.librarysystem.model.UserStatus;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private static final String USER_COLUMNS = """
            u.user_id, u.student_no, u.name, u.password_hash, u.role_level,
            u.created_at, u.status, l.display_name, l.max_borrow_days,
            l.max_active_loans, l.can_favorite, l.is_admin,
            l.registration_allowed, l.custom_level
            """;

    public Optional<User> findByStudentNo(String studentNo) throws SQLException {
        String sql = """
                SELECT %s
                FROM users u
                JOIN user_levels l ON l.level_code = u.role_level
                WHERE u.student_no = ?
                """.formatted(USER_COLUMNS);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(int userId) throws SQLException {
        String sql = """
                SELECT %s
                FROM users u
                JOIN user_levels l ON l.level_code = u.role_level
                WHERE u.user_id = ?
                """.formatted(USER_COLUMNS);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public List<User> listAll() throws SQLException {
        String sql = """
                SELECT %s
                FROM users u
                JOIN user_levels l ON l.level_code = u.role_level
                ORDER BY l.is_admin DESC, u.user_id
                """.formatted(USER_COLUMNS);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        }
    }

    public List<User> search(String query) throws SQLException {
        String sql = """
                SELECT %s
                FROM users u
                JOIN user_levels l ON l.level_code = u.role_level
                WHERE ? = ''
                   OR u.student_no LIKE ?
                   OR u.name LIKE ?
                ORDER BY u.user_id
                """.formatted(USER_COLUMNS);
        String normalized = query == null ? "" : query.trim();
        String like = "%" + normalized + "%";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, like);
            statement.setString(3, like);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
                return users;
            }
        }
    }

    public User createStudent(String studentNo, String name, String passwordHash, String levelCode) throws SQLException {
        String sql = """
                INSERT INTO users (student_no, name, password_hash, role_level, status, created_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', NOW())
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, studentNo);
            statement.setString(2, name);
            statement.setString(3, passwordHash);
            statement.setString(4, levelCode);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("User was inserted but no generated key was returned.");
    }

    public void updateStatus(int userId, UserStatus status) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE user_id = ? AND role_level <> 'ADMIN'";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public void assignLevel(int userId, String levelCode) throws SQLException {
        String sql = "UPDATE users SET role_level = ? WHERE user_id = ? AND role_level <> 'ADMIN'";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, levelCode);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return new User(
                resultSet.getInt("user_id"),
                resultSet.getString("student_no"),
                resultSet.getString("name"),
                resultSet.getString("password_hash"),
                mapLevel(resultSet),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                UserStatus.valueOf(resultSet.getString("status"))
        );
    }

    private UserLevel mapLevel(ResultSet resultSet) throws SQLException {
        return new UserLevel(
                resultSet.getString("role_level"),
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
