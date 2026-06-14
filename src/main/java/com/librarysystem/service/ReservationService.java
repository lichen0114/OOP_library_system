package com.librarysystem.service;

import com.librarysystem.dao.BorrowRecordDao;
import com.librarysystem.dao.ReservationDao;
import com.librarysystem.model.BookStatus;
import com.librarysystem.model.Reservation;
import com.librarysystem.model.User;
import com.librarysystem.model.UserStatus;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ReservationService {
    private final ReservationDao reservationDao = new ReservationDao();
    private final BorrowRecordDao borrowRecordDao = new BorrowRecordDao();

    public List<Reservation> listReservations(User user) throws SQLException {
        return reservationDao.listByUser(user.getUserId());
    }

    public List<Reservation> listAllReservations() throws SQLException {
        return reservationDao.listAll();
    }

    public void reserveBook(User user, int bookId) throws SQLException {
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Admin accounts cannot reserve books.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("This account is suspended and cannot reserve books.");
        }

        Connection connection = Database.getConnection();
        try {
            connection.setAutoCommit(false);
            BookStatus status = lockBookAndReadStatus(connection, bookId);
            if (status != BookStatus.BORROWED) {
                throw new IllegalArgumentException("Only currently borrowed books can be reserved. Available books can be borrowed directly.");
            }
            if (borrowRecordDao.hasActiveLoanForBook(connection, user.getUserId(), bookId)) {
                throw new IllegalArgumentException("You already have this book borrowed.");
            }
            if (reservationDao.hasActiveReservation(connection, user.getUserId(), bookId)) {
                throw new IllegalArgumentException("You already have an active reservation for this book.");
            }
            reservationDao.create(connection, user.getUserId(), bookId);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly(connection);
            throw e;
        } finally {
            closeQuietly(connection);
        }
    }

    public void cancelReservation(User user, int reservationId) throws SQLException {
        reservationDao.cancelForUser(reservationId, user.getUserId());
    }

    private BookStatus lockBookAndReadStatus(Connection connection, int bookId) throws SQLException {
        String sql = "SELECT status FROM books WHERE book_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Book not found.");
                }
                return BookStatus.valueOf(resultSet.getString("status"));
            }
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException ignored) {
            // Preserve the original failure.
        }
    }

    private void closeQuietly(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // Nothing useful to add for UI callers.
        }
    }
}
