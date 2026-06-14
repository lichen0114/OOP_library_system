package com.librarysystem.dao;

import com.librarysystem.model.LevelRequest;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LevelRequestDao {
    private static final String COLUMNS = """
            lr.request_id, lr.user_id, u.student_no, u.name AS user_name,
            u.role_level AS current_level_code, lr.requested_level_code,
            requested.display_name AS requested_level_name,
            lr.reason, lr.status, lr.admin_note, lr.requested_at, lr.reviewed_at,
            reviewer.name AS reviewer_name
            """;

    private static final String FROM = """
            FROM level_requests lr
            JOIN users u ON u.user_id = lr.user_id
            JOIN user_levels requested ON requested.level_code = lr.requested_level_code
            LEFT JOIN users reviewer ON reviewer.user_id = lr.reviewed_by
            """;

    public List<LevelRequest> listAll() throws SQLException {
        String sql = """
                SELECT %s
                %s
                ORDER BY lr.status = 'PENDING' DESC, lr.requested_at DESC
                """.formatted(COLUMNS, FROM);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapRequests(resultSet);
        }
    }

    public List<LevelRequest> listByUser(int userId) throws SQLException {
        String sql = """
                SELECT %s
                %s
                WHERE lr.user_id = ?
                ORDER BY lr.requested_at DESC
                """.formatted(COLUMNS, FROM);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapRequests(resultSet);
            }
        }
    }

    public boolean hasPending(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM level_requests WHERE user_id = ? AND status = 'PENDING'";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public void create(int userId, String requestedLevelCode, String reason) throws SQLException {
        String sql = """
                INSERT INTO level_requests (user_id, requested_level_code, reason, status, requested_at)
                VALUES (?, ?, ?, 'PENDING', NOW())
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, requestedLevelCode);
            statement.setString(3, reason);
            statement.executeUpdate();
        }
    }

    private List<LevelRequest> mapRequests(ResultSet resultSet) throws SQLException {
        List<LevelRequest> requests = new ArrayList<>();
        while (resultSet.next()) {
            requests.add(new LevelRequest(
                    resultSet.getInt("request_id"),
                    resultSet.getInt("user_id"),
                    resultSet.getString("student_no"),
                    resultSet.getString("user_name"),
                    resultSet.getString("current_level_code"),
                    resultSet.getString("requested_level_code"),
                    resultSet.getString("requested_level_name"),
                    resultSet.getString("reason"),
                    resultSet.getString("status"),
                    resultSet.getString("admin_note"),
                    toLocalDateTime(resultSet.getTimestamp("requested_at")),
                    toLocalDateTime(resultSet.getTimestamp("reviewed_at")),
                    resultSet.getString("reviewer_name")
            ));
        }
        return requests;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
