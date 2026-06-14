package com.librarysystem.dao;

import com.librarysystem.model.BorrowRecord;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BorrowRecordDao {
    private static final String JOINED_COLUMNS = """
            r.record_id, r.user_id, u.student_no, u.name AS user_name,
            r.book_id, b.title AS book_title, r.borrow_date, r.due_date,
            r.return_date, r.borrow_days, r.created_at
            """;

    public List<BorrowRecord> listAll() throws SQLException {
        String sql = """
                SELECT %s
                FROM borrow_records r
                JOIN users u ON u.user_id = r.user_id
                JOIN books b ON b.book_id = r.book_id
                ORDER BY r.borrow_date DESC, r.record_id DESC
                """.formatted(JOINED_COLUMNS);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapRecords(resultSet);
        }
    }

    public List<BorrowRecord> listByUser(int userId) throws SQLException {
        String sql = """
                SELECT %s
                FROM borrow_records r
                JOIN users u ON u.user_id = r.user_id
                JOIN books b ON b.book_id = r.book_id
                WHERE r.user_id = ?
                ORDER BY r.borrow_date DESC, r.record_id DESC
                """.formatted(JOINED_COLUMNS);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapRecords(resultSet);
            }
        }
    }

    public List<BorrowRecord> listActiveByUser(int userId) throws SQLException {
        String sql = """
                SELECT %s
                FROM borrow_records r
                JOIN users u ON u.user_id = r.user_id
                JOIN books b ON b.book_id = r.book_id
                WHERE r.user_id = ? AND r.return_date IS NULL
                ORDER BY r.due_date, r.record_id
                """.formatted(JOINED_COLUMNS);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapRecords(resultSet);
            }
        }
    }

    public List<BorrowRecord> listByBook(int bookId) throws SQLException {
        String sql = """
                SELECT %s
                FROM borrow_records r
                JOIN users u ON u.user_id = r.user_id
                JOIN books b ON b.book_id = r.book_id
                WHERE r.book_id = ?
                ORDER BY r.borrow_date DESC, r.record_id DESC
                """.formatted(JOINED_COLUMNS);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapRecords(resultSet);
            }
        }
    }

    public List<BorrowRecord> searchByStudent(String query) throws SQLException {
        String sql = """
                SELECT %s
                FROM borrow_records r
                JOIN users u ON u.user_id = r.user_id
                JOIN books b ON b.book_id = r.book_id
                WHERE ? = ''
                   OR u.student_no LIKE ?
                   OR u.name LIKE ?
                ORDER BY r.borrow_date DESC, r.record_id DESC
                """.formatted(JOINED_COLUMNS);
        String normalized = query == null ? "" : query.trim();
        String like = "%" + normalized + "%";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, like);
            statement.setString(3, like);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapRecords(resultSet);
            }
        }
    }

    public int countActiveByUser(Connection connection, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private List<BorrowRecord> mapRecords(ResultSet resultSet) throws SQLException {
        List<BorrowRecord> records = new ArrayList<>();
        while (resultSet.next()) {
            records.add(mapRecord(resultSet));
        }
        return records;
    }

    private BorrowRecord mapRecord(ResultSet resultSet) throws SQLException {
        return new BorrowRecord(
                resultSet.getInt("record_id"),
                resultSet.getInt("user_id"),
                resultSet.getString("student_no"),
                resultSet.getString("user_name"),
                resultSet.getInt("book_id"),
                resultSet.getString("book_title"),
                toLocalDateTime(resultSet.getTimestamp("borrow_date")),
                toLocalDateTime(resultSet.getTimestamp("due_date")),
                toLocalDateTime(resultSet.getTimestamp("return_date")),
                resultSet.getInt("borrow_days"),
                toLocalDateTime(resultSet.getTimestamp("created_at"))
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
