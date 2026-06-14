package com.librarysystem.service;

import com.librarysystem.dao.BorrowRecordDao;
import com.librarysystem.dao.ReservationDao;
import com.librarysystem.model.BookStatus;
import com.librarysystem.model.BorrowRecord;
import com.librarysystem.model.Reservation;
import com.librarysystem.model.User;
import com.librarysystem.model.UserStatus;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowService {
    private static final List<Integer> ALLOWED_BORROW_DAYS = List.of(1, 3, 7, 14);

    private final BorrowRecordDao borrowRecordDao = new BorrowRecordDao();
    private final ReservationDao reservationDao = new ReservationDao();
    private final SettingsService settingsService = new SettingsService();

    public void borrowBook(User user, int bookId) throws SQLException {
        List<Integer> options = allowedBorrowDays(user);
        if (options.isEmpty()) {
            throw new IllegalArgumentException("This account has no allowed borrow period.");
        }
        borrowBook(user, bookId, options.get(options.size() - 1));
    }

    public void borrowBook(User user, int bookId, int borrowDays) throws SQLException {
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Admin accounts cannot borrow books.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("This account is suspended and cannot borrow books.");
        }
        if (!ALLOWED_BORROW_DAYS.contains(borrowDays)) {
            throw new IllegalArgumentException("Borrow days must be 1, 3, 7, or 14.");
        }
        if (borrowDays > user.getUserLevel().getMaxBorrowDays()) {
            throw new IllegalArgumentException("Borrow days exceed this user's level limit.");
        }

        Connection connection = Database.getConnection();
        try {
            connection.setAutoCommit(false);

            BookStatus status = lockBookAndReadStatus(connection, bookId);
            if (status != BookStatus.AVAILABLE) {
                throw new IllegalArgumentException("Only available books can be borrowed.");
            }

            Optional<Reservation> activeReservation = reservationDao.findEarliestActiveForBook(connection, bookId);
            Integer fulfilledReservationId = null;
            if (activeReservation.isPresent()) {
                Reservation reservation = activeReservation.get();
                if (!"NOTIFIED".equals(reservation.getStatus()) || reservation.getUserId() != user.getUserId()) {
                    throw new IllegalArgumentException("This book is reserved for another user.");
                }
                fulfilledReservationId = reservation.getReservationId();
            }

            int activeLoans = borrowRecordDao.countActiveByUser(connection, user.getUserId());
            if (activeLoans >= user.getUserLevel().getMaxActiveLoans()) {
                throw new IllegalArgumentException("Borrow limit reached for " + user.getLevelDisplayName() + " users.");
            }

            String insertRecord = """
                    INSERT INTO borrow_records
                        (user_id, book_id, borrow_date, due_date, return_date, borrow_days, created_at)
                    VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), NULL, ?, NOW())
                    """;
            try (PreparedStatement statement = connection.prepareStatement(insertRecord)) {
                statement.setInt(1, user.getUserId());
                statement.setInt(2, bookId);
                statement.setInt(3, borrowDays);
                statement.setInt(4, borrowDays);
                statement.executeUpdate();
            }

            if (fulfilledReservationId != null) {
                reservationDao.markFulfilled(connection, fulfilledReservationId);
            }
            updateBookStatus(connection, bookId, BookStatus.BORROWED);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly(connection);
            throw e;
        } finally {
            closeQuietly(connection);
        }
    }

    public void returnBook(User user, int bookId) throws SQLException {
        Connection connection = Database.getConnection();
        try {
            connection.setAutoCommit(false);

            int recordId;
            String lockRecord = """
                    SELECT record_id
                    FROM borrow_records
                    WHERE user_id = ? AND book_id = ? AND return_date IS NULL
                    ORDER BY borrow_date DESC
                    LIMIT 1
                    FOR UPDATE
                    """;
            try (PreparedStatement statement = connection.prepareStatement(lockRecord)) {
                statement.setInt(1, user.getUserId());
                statement.setInt(2, bookId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalArgumentException("There is no active borrowing record for this book.");
                    }
                    recordId = resultSet.getInt("record_id");
                }
            }

            String updateRecord = "UPDATE borrow_records SET return_date = NOW() WHERE record_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(updateRecord)) {
                statement.setInt(1, recordId);
                statement.executeUpdate();
            }

            reservationDao.findEarliestPendingForBook(connection, bookId)
                    .ifPresent(reservation -> {
                        try {
                            reservationDao.markNotified(connection, reservation.getReservationId());
                        } catch (SQLException e) {
                            throw new ReservationUpdateException(e);
                        }
                    });
            updateBookStatus(connection, bookId, BookStatus.AVAILABLE);
            connection.commit();
        } catch (ReservationUpdateException e) {
            rollbackQuietly(connection);
            throw e.getCause();
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly(connection);
            throw e;
        } finally {
            closeQuietly(connection);
        }
    }

    public List<BorrowRecord> listHistory(User user) throws SQLException {
        return borrowRecordDao.listByUser(user.getUserId());
    }

    public List<BorrowRecord> listActiveLoans(User user) throws SQLException {
        return borrowRecordDao.listActiveByUser(user.getUserId());
    }

    public List<BorrowRecord> listReminders(User user) throws SQLException {
        return borrowRecordDao.listRemindersByUser(user.getUserId(), settingsService.getSettings().getReminderDays());
    }

    public List<BorrowRecord> listBookHistory(int bookId) throws SQLException {
        return borrowRecordDao.listByBook(bookId);
    }

    public List<Integer> allowedBorrowDays(User user) {
        List<Integer> days = new ArrayList<>();
        if (user.isAdmin()) {
            return days;
        }
        for (int option : ALLOWED_BORROW_DAYS) {
            if (option <= user.getUserLevel().getMaxBorrowDays()) {
                days.add(option);
            }
        }
        return days;
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

    private void updateBookStatus(Connection connection, int bookId, BookStatus status) throws SQLException {
        String sql = "UPDATE books SET status = ? WHERE book_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, bookId);
            statement.executeUpdate();
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

    private static class ReservationUpdateException extends RuntimeException {
        ReservationUpdateException(SQLException cause) {
            super(cause);
        }

        @Override
        public synchronized SQLException getCause() {
            return (SQLException) super.getCause();
        }
    }
}
