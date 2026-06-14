package com.librarysystem.dao;

import com.librarysystem.model.Reservation;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationDao {
    private static final String COLUMNS = """
            r.reservation_id, r.user_id, u.student_no, u.name AS user_name,
            r.book_id, b.title AS book_title, r.status, r.requested_at, r.notified_at
            """;

    private static final String FROM = """
            FROM reservations r
            JOIN users u ON u.user_id = r.user_id
            JOIN books b ON b.book_id = r.book_id
            """;

    public List<Reservation> listAll() throws SQLException {
        String sql = """
                SELECT %s
                %s
                ORDER BY r.status IN ('PENDING', 'NOTIFIED') DESC, r.requested_at DESC
                """.formatted(COLUMNS, FROM);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapReservations(resultSet);
        }
    }

    public List<Reservation> listByUser(int userId) throws SQLException {
        String sql = """
                SELECT %s
                %s
                WHERE r.user_id = ?
                ORDER BY r.status IN ('PENDING', 'NOTIFIED') DESC, r.requested_at DESC
                """.formatted(COLUMNS, FROM);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapReservations(resultSet);
            }
        }
    }

    public boolean hasActiveReservation(Connection connection, int userId, int bookId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM reservations
                WHERE user_id = ? AND book_id = ? AND status IN ('PENDING', 'NOTIFIED')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public void create(Connection connection, int userId, int bookId) throws SQLException {
        String sql = """
                INSERT INTO reservations (user_id, book_id, status, requested_at)
                VALUES (?, ?, 'PENDING', NOW())
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, bookId);
            statement.executeUpdate();
        }
    }

    public Optional<Reservation> findEarliestActiveForBook(Connection connection, int bookId) throws SQLException {
        String sql = """
                SELECT %s
                %s
                WHERE r.book_id = ? AND r.status IN ('PENDING', 'NOTIFIED')
                ORDER BY CASE WHEN r.status = 'NOTIFIED' THEN 0 ELSE 1 END, r.requested_at, r.reservation_id
                LIMIT 1
                FOR UPDATE
                """.formatted(COLUMNS, FROM);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapReservation(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Reservation> findEarliestPendingForBook(Connection connection, int bookId) throws SQLException {
        String sql = """
                SELECT %s
                %s
                WHERE r.book_id = ? AND r.status = 'PENDING'
                ORDER BY r.requested_at, r.reservation_id
                LIMIT 1
                FOR UPDATE
                """.formatted(COLUMNS, FROM);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapReservation(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public void markNotified(Connection connection, int reservationId) throws SQLException {
        String sql = "UPDATE reservations SET status = 'NOTIFIED', notified_at = NOW() WHERE reservation_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservationId);
            statement.executeUpdate();
        }
    }

    public void markFulfilled(Connection connection, int reservationId) throws SQLException {
        String sql = "UPDATE reservations SET status = 'FULFILLED', fulfilled_at = NOW() WHERE reservation_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservationId);
            statement.executeUpdate();
        }
    }

    public void cancelForUser(int reservationId, int userId) throws SQLException {
        String sql = """
                UPDATE reservations
                SET status = 'CANCELLED', cancelled_at = NOW()
                WHERE reservation_id = ? AND user_id = ? AND status IN ('PENDING', 'NOTIFIED')
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservationId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private List<Reservation> mapReservations(ResultSet resultSet) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        while (resultSet.next()) {
            reservations.add(mapReservation(resultSet));
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet resultSet) throws SQLException {
        return new Reservation(
                resultSet.getInt("reservation_id"),
                resultSet.getInt("user_id"),
                resultSet.getString("student_no"),
                resultSet.getString("user_name"),
                resultSet.getInt("book_id"),
                resultSet.getString("book_title"),
                resultSet.getString("status"),
                toLocalDateTime(resultSet.getTimestamp("requested_at")),
                toLocalDateTime(resultSet.getTimestamp("notified_at"))
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
