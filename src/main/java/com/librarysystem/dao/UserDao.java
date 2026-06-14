package com.librarysystem.dao;

import com.librarysystem.model.RoleLevel;
import com.librarysystem.model.User;
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
    public Optional<User> findByStudentNo(String studentNo) throws SQLException {
        String sql = "SELECT * FROM users WHERE student_no = ?";
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
        String sql = "SELECT * FROM users WHERE user_id = ?";
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
        String sql = "SELECT * FROM users ORDER BY role_level = 'ADMIN' DESC, user_id";
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
                SELECT *
                FROM users
                WHERE ? = ''
                   OR student_no LIKE ?
                   OR name LIKE ?
                ORDER BY user_id
                """;
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

    public User createStudent(String studentNo, String name, String passwordHash, RoleLevel roleLevel) throws SQLException {
        String sql = """
                INSERT INTO users (student_no, name, password_hash, role_level, status, created_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', NOW())
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, studentNo);
            statement.setString(2, name);
            statement.setString(3, passwordHash);
            statement.setString(4, roleLevel.name());
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

    private User mapUser(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return new User(
                resultSet.getInt("user_id"),
                resultSet.getString("student_no"),
                resultSet.getString("name"),
                resultSet.getString("password_hash"),
                RoleLevel.valueOf(resultSet.getString("role_level")),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                UserStatus.valueOf(resultSet.getString("status"))
        );
    }
}
