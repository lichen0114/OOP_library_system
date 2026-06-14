package com.librarysystem.service;

import com.librarysystem.dao.BorrowRecordDao;
import com.librarysystem.model.BookStatus;
import com.librarysystem.model.BorrowRecord;
import com.librarysystem.model.RoleLevel;
import com.librarysystem.model.User;
import com.librarysystem.model.UserStatus;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class BorrowService {
    private final BorrowRecordDao borrowRecordDao = new BorrowRecordDao();

    public void borrowBook(User user, int bookId) throws SQLException {
        if (user.getRoleLevel() == RoleLevel.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot borrow books.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("This account is suspended and cannot borrow books.");
        }

        Connection connection = Database.getConnection();
        try {
            connection.setAutoCommit(false);

            BookStatus status = lockBookAndReadStatus(connection, bookId);
            if (status != BookStatus.AVAILABLE) {
                throw new IllegalArgumentException("Only available books can be borrowed.");
            }

            int activeLoans = borrowRecordDao.countActiveByUser(connection, user.getUserId());
            if (activeLoans >= user.getRoleLevel().getMaxActiveLoans()) {
                throw new IllegalArgumentException("Borrow limit reached for " + user.getRoleLevel() + " users.");
            }

            String insertRecord = """
                    INSERT INTO borrow_records
                        (user_id, book_id, borrow_date, due_date, return_date, borrow_days, created_at)
                    VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), NULL, ?, NOW())
                    """;
            try (PreparedStatement statement = connection.prepareStatement(insertRecord)) {
                statement.setInt(1, user.getUserId());
                statement.setInt(2, bookId);
                statement.setInt(3, user.getRoleLevel().getMaxBorrowDays());
                statement.setInt(4, user.getRoleLevel().getMaxBorrowDays());
                statement.executeUpdate();
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

            updateBookStatus(connection, bookId, BookStatus.AVAILABLE);
            connection.commit();
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

    public List<BorrowRecord> listBookHistory(int bookId) throws SQLException {
        return borrowRecordDao.listByBook(bookId);
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
}
