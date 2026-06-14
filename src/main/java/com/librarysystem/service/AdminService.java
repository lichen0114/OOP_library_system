package com.librarysystem.service;

import com.librarysystem.dao.BookDao;
import com.librarysystem.dao.BorrowRecordDao;
import com.librarysystem.dao.UserDao;
import com.librarysystem.model.Book;
import com.librarysystem.model.BookStatus;
import com.librarysystem.model.BorrowRecord;
import com.librarysystem.model.RoleLevel;
import com.librarysystem.model.User;
import com.librarysystem.model.UserStatus;
import com.librarysystem.util.Database;
import com.librarysystem.util.ValidationUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    private final BookDao bookDao = new BookDao();
    private final UserDao userDao = new UserDao();
    private final BorrowRecordDao borrowRecordDao = new BorrowRecordDao();

    public List<BorrowRecord> listAllRecords() throws SQLException {
        return borrowRecordDao.listAll();
    }

    public List<BorrowRecord> searchStudentRecords(String query) throws SQLException {
        return borrowRecordDao.searchByStudent(query);
    }

    public List<User> listUsers() throws SQLException {
        return userDao.listAll();
    }

    public List<User> searchUsers(String query) throws SQLException {
        return userDao.search(query);
    }

    public int addBook(String title, String authors, String subjects, String publisher,
                       Integer publishYear, String edition, String formatDesc,
                       String source, String note, List<String> isbns) throws SQLException {
        Book book = new Book(
                0,
                ValidationUtil.requireText(title, "Title"),
                emptyToNull(authors),
                emptyToNull(subjects),
                emptyToNull(publisher),
                publishYear,
                emptyToNull(edition),
                emptyToNull(formatDesc),
                emptyToNull(source),
                emptyToNull(note),
                BookStatus.AVAILABLE,
                isbns
        );
        return bookDao.addBook(book);
    }

    public void removeBook(int bookId) throws SQLException {
        if (bookDao.hasActiveLoan(bookId)) {
            throw new IllegalArgumentException("Borrowed books cannot be removed until returned.");
        }
        bookDao.updateStatus(bookId, BookStatus.REMOVED);
    }

    public void suspendUser(int userId) throws SQLException {
        userDao.updateStatus(userId, UserStatus.SUSPENDED);
    }

    public void reactivateUser(int userId) throws SQLException {
        userDao.updateStatus(userId, UserStatus.ACTIVE);
    }

    public Map<String, Integer> statistics() throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection connection = Database.getConnection()) {
            stats.put("Books", scalar(connection, "SELECT COUNT(*) FROM books"));
            stats.put("Available books", scalar(connection, "SELECT COUNT(*) FROM books WHERE status = 'AVAILABLE'"));
            stats.put("Borrowed books", scalar(connection, "SELECT COUNT(*) FROM books WHERE status = 'BORROWED'"));
            stats.put("Removed books", scalar(connection, "SELECT COUNT(*) FROM books WHERE status = 'REMOVED'"));
            stats.put("Users", scalar(connection, "SELECT COUNT(*) FROM users WHERE role_level <> 'ADMIN'"));
            stats.put("Normal users", scalar(connection, "SELECT COUNT(*) FROM users WHERE role_level = 'NORMAL'"));
            stats.put("VIP users", scalar(connection, "SELECT COUNT(*) FROM users WHERE role_level = 'VIP'"));
            stats.put("Suspended users", scalar(connection, "SELECT COUNT(*) FROM users WHERE status = 'SUSPENDED'"));
            stats.put("Borrow records", scalar(connection, "SELECT COUNT(*) FROM borrow_records"));
            stats.put("Active loans", scalar(connection, "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL"));
            stats.put("Overdue active loans", scalar(connection, "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL AND due_date < NOW()"));
        }
        return stats;
    }

    private int scalar(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
