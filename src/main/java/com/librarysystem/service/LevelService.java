package com.librarysystem.service;

import com.librarysystem.dao.LevelDao;
import com.librarysystem.dao.LevelRequestDao;
import com.librarysystem.dao.UserDao;
import com.librarysystem.model.LevelRequest;
import com.librarysystem.model.User;
import com.librarysystem.model.UserLevel;
import com.librarysystem.util.Database;
import com.librarysystem.util.ValidationUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class LevelService {
    private final LevelDao levelDao = new LevelDao();
    private final LevelRequestDao levelRequestDao = new LevelRequestDao();
    private final UserDao userDao = new UserDao();

    public List<UserLevel> listAllLevels() throws SQLException {
        return levelDao.listAll();
    }

    public List<UserLevel> listAssignableLevels() throws SQLException {
        return levelDao.listAssignable();
    }

    public List<UserLevel> listRegistrationLevels() throws SQLException {
        return levelDao.listRegistrationAllowed();
    }

    public List<LevelRequest> listAllRequests() throws SQLException {
        return levelRequestDao.listAll();
    }

    public List<LevelRequest> listRequests(User user) throws SQLException {
        return levelRequestDao.listByUser(user.getUserId());
    }

    public void requestLevel(User user, String levelCode, String reason) throws SQLException {
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Admin accounts do not request student levels.");
        }
        String normalizedCode = normalizeCode(levelCode);
        UserLevel target = levelDao.findByCode(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Requested level does not exist."));
        if (target.isAdmin()) {
            throw new IllegalArgumentException("Admin level cannot be requested.");
        }
        if (target.getCode().equals(user.getLevelCode())) {
            throw new IllegalArgumentException("You already have that level.");
        }
        if (levelRequestDao.hasPending(user.getUserId())) {
            throw new IllegalArgumentException("You already have a pending level request.");
        }
        levelRequestDao.create(user.getUserId(), normalizedCode, emptyToNull(reason));
    }

    public void createCustomLevel(String code, String displayName, int maxBorrowDays,
                                  int maxActiveLoans, boolean canFavorite,
                                  boolean registrationAllowed) throws SQLException {
        UserLevel level = buildEditableLevel(code, displayName, maxBorrowDays, maxActiveLoans, canFavorite, registrationAllowed);
        if (levelDao.findByCode(level.getCode()).isPresent()) {
            throw new IllegalArgumentException("A level with that code already exists.");
        }
        levelDao.createCustomLevel(level);
    }

    public void updateCustomLevel(String code, String displayName, int maxBorrowDays,
                                  int maxActiveLoans, boolean canFavorite,
                                  boolean registrationAllowed) throws SQLException {
        UserLevel level = buildEditableLevel(code, displayName, maxBorrowDays, maxActiveLoans, canFavorite, registrationAllowed);
        levelDao.updateCustomLevel(level);
    }

    public void assignUserLevel(int userId, String levelCode) throws SQLException {
        String normalizedCode = normalizeCode(levelCode);
        UserLevel level = levelDao.findByCode(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Selected level does not exist."));
        if (level.isAdmin()) {
            throw new IllegalArgumentException("Admin level cannot be assigned from user management.");
        }
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Administrator accounts cannot be reassigned here.");
        }
        userDao.assignLevel(userId, normalizedCode);
    }

    public void reviewRequest(User admin, int requestId, boolean approve, String note) throws SQLException {
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("Only admins can review level requests.");
        }
        Connection connection = Database.getConnection();
        try {
            connection.setAutoCommit(false);
            PendingRequest request = lockPendingRequest(connection, requestId);
            if (approve) {
                updateUserLevel(connection, request.userId(), request.requestedLevelCode());
            }
            updateRequest(connection, requestId, approve ? "APPROVED" : "REJECTED", admin.getUserId(), emptyToNull(note));
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly(connection);
            throw e;
        } finally {
            closeQuietly(connection);
        }
    }

    private UserLevel buildEditableLevel(String code, String displayName, int maxBorrowDays,
                                         int maxActiveLoans, boolean canFavorite,
                                         boolean registrationAllowed) {
        String normalizedCode = normalizeCode(code);
        String normalizedName = ValidationUtil.requireText(displayName, "Display name");
        if (!normalizedCode.matches("[A-Z0-9_]{2,30}")) {
            throw new IllegalArgumentException("Level code must be 2-30 characters: A-Z, 0-9, or underscore.");
        }
        if (maxBorrowDays < 1 || maxBorrowDays > 14) {
            throw new IllegalArgumentException("Max borrow days must be between 1 and 14.");
        }
        if (maxActiveLoans < 1 || maxActiveLoans > 50) {
            throw new IllegalArgumentException("Max active loans must be between 1 and 50.");
        }
        return new UserLevel(normalizedCode, normalizedName, maxBorrowDays, maxActiveLoans,
                canFavorite, false, registrationAllowed, true);
    }

    private PendingRequest lockPendingRequest(Connection connection, int requestId) throws SQLException {
        String sql = """
                SELECT lr.user_id, lr.requested_level_code, l.is_admin
                FROM level_requests lr
                JOIN user_levels l ON l.level_code = lr.requested_level_code
                WHERE lr.request_id = ? AND lr.status = 'PENDING'
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Pending request not found.");
                }
                if (resultSet.getBoolean("is_admin")) {
                    throw new IllegalArgumentException("Admin level requests cannot be approved.");
                }
                return new PendingRequest(
                        resultSet.getInt("user_id"),
                        resultSet.getString("requested_level_code")
                );
            }
        }
    }

    private void updateUserLevel(Connection connection, int userId, String levelCode) throws SQLException {
        String sql = "UPDATE users SET role_level = ? WHERE user_id = ? AND role_level <> 'ADMIN'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, levelCode);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private void updateRequest(Connection connection, int requestId, String status, int reviewedBy, String note) throws SQLException {
        String sql = """
                UPDATE level_requests
                SET status = ?, admin_note = ?, reviewed_by = ?, reviewed_at = NOW()
                WHERE request_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, note);
            statement.setInt(3, reviewedBy);
            statement.setInt(4, requestId);
            statement.executeUpdate();
        }
    }

    private String normalizeCode(String code) {
        return ValidationUtil.requireText(code, "Level code").toUpperCase(Locale.ROOT);
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
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

    private record PendingRequest(int userId, String requestedLevelCode) {
    }
}
