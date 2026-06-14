package com.librarysystem.service;

import com.librarysystem.dao.BookDao;
import com.librarysystem.dao.BorrowRecordDao;
import com.librarysystem.dao.UserDao;
import com.librarysystem.model.Book;
import com.librarysystem.model.BookStatus;
import com.librarysystem.model.BorrowRecord;
import com.librarysystem.model.LevelRequest;
import com.librarysystem.model.LibrarySettings;
import com.librarysystem.model.Reservation;
import com.librarysystem.model.Review;
import com.librarysystem.model.SubjectPopularity;
import com.librarysystem.model.User;
import com.librarysystem.model.UserLevel;
import com.librarysystem.model.UserStatus;
import com.librarysystem.util.Database;
import com.librarysystem.util.ValidationUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    private final BookDao bookDao = new BookDao();
    private final UserDao userDao = new UserDao();
    private final BorrowRecordDao borrowRecordDao = new BorrowRecordDao();
    private final LevelService levelService = new LevelService();
    private final ReservationService reservationService = new ReservationService();
    private final ReviewService reviewService = new ReviewService();
    private final SettingsService settingsService = new SettingsService();

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

    public List<UserLevel> listLevels() throws SQLException {
        return levelService.listAllLevels();
    }

    public List<UserLevel> listAssignableLevels() throws SQLException {
        return levelService.listAssignableLevels();
    }

    public void createCustomLevel(String code, String displayName, int maxBorrowDays,
                                  int maxActiveLoans, boolean canFavorite,
                                  boolean registrationAllowed) throws SQLException {
        levelService.createCustomLevel(code, displayName, maxBorrowDays, maxActiveLoans, canFavorite, registrationAllowed);
    }

    public void updateCustomLevel(String code, String displayName, int maxBorrowDays,
                                  int maxActiveLoans, boolean canFavorite,
                                  boolean registrationAllowed) throws SQLException {
        levelService.updateCustomLevel(code, displayName, maxBorrowDays, maxActiveLoans, canFavorite, registrationAllowed);
    }

    public void assignUserLevel(int userId, String levelCode) throws SQLException {
        levelService.assignUserLevel(userId, levelCode);
    }

    public List<LevelRequest> listLevelRequests() throws SQLException {
        return levelService.listAllRequests();
    }

    public void reviewLevelRequest(User admin, int requestId, boolean approve, String note) throws SQLException {
        levelService.reviewRequest(admin, requestId, approve, note);
    }

    public List<Reservation> listReservations() throws SQLException {
        return reservationService.listAllReservations();
    }

    public List<Review> listReviews() throws SQLException {
        return reviewService.listAllReviews();
    }

    public LibrarySettings getSettings() throws SQLException {
        return settingsService.getSettings();
    }

    public void updateSettings(int reminderDays, int finePerOverdueDay) throws SQLException {
        settingsService.updateSettings(reminderDays, finePerOverdueDay);
    }

    public Map<String, Integer> statistics() throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection connection = Database.getConnection()) {
            stats.put("Books", scalar(connection, "SELECT COUNT(*) FROM books"));
            stats.put("Available books", scalar(connection, "SELECT COUNT(*) FROM books WHERE status = 'AVAILABLE'"));
            stats.put("Borrowed books", scalar(connection, "SELECT COUNT(*) FROM books WHERE status = 'BORROWED'"));
            stats.put("Removed books", scalar(connection, "SELECT COUNT(*) FROM books WHERE status = 'REMOVED'"));
            stats.put("Users", scalar(connection, "SELECT COUNT(*) FROM users u JOIN user_levels l ON l.level_code = u.role_level WHERE l.is_admin = FALSE"));
            stats.put("Normal users", scalar(connection, "SELECT COUNT(*) FROM users WHERE role_level = 'NORMAL'"));
            stats.put("VIP users", scalar(connection, "SELECT COUNT(*) FROM users WHERE role_level = 'VIP'"));
            stats.put("Favorite-enabled users", scalar(connection, "SELECT COUNT(*) FROM users u JOIN user_levels l ON l.level_code = u.role_level WHERE l.can_favorite = TRUE"));
            stats.put("Suspended users", scalar(connection, "SELECT COUNT(*) FROM users WHERE status = 'SUSPENDED'"));
            stats.put("Borrow records", scalar(connection, "SELECT COUNT(*) FROM borrow_records"));
            stats.put("Active loans", scalar(connection, "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL"));
            stats.put("Overdue active loans", scalar(connection, "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL AND due_date < NOW()"));
            stats.put("Pending reservations", scalar(connection, "SELECT COUNT(*) FROM reservations WHERE status = 'PENDING'"));
            stats.put("Notified reservations", scalar(connection, "SELECT COUNT(*) FROM reservations WHERE status = 'NOTIFIED'"));
            stats.put("Reviews", scalar(connection, "SELECT COUNT(*) FROM reviews"));
            stats.put("Pending level requests", scalar(connection, "SELECT COUNT(*) FROM level_requests WHERE status = 'PENDING'"));
        }
        return stats;
    }

    public List<SubjectPopularity> subjectPopularity() throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        String sql = """
                SELECT b.subjects
                FROM borrow_records r
                JOIN books b ON b.book_id = r.book_id
                WHERE b.subjects IS NOT NULL AND b.subjects <> ''
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String subjects = resultSet.getString("subjects");
                for (String subject : subjects.split(",")) {
                    String normalized = subject.trim();
                    if (!normalized.isEmpty()) {
                        counts.merge(normalized, 1, Integer::sum);
                    }
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(20)
                .map(entry -> new SubjectPopularity(entry.getKey(), entry.getValue()))
                .toList();
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
