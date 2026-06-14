package com.librarysystem.dao;

import com.librarysystem.model.Review;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReviewDao {
    private static final String COLUMNS = """
            rv.review_id, rv.record_id, rv.user_id, u.student_no, u.name AS user_name,
            rv.book_id, b.title AS book_title, rv.rating, rv.comment, rv.created_at
            """;

    private static final String FROM = """
            FROM reviews rv
            JOIN users u ON u.user_id = rv.user_id
            JOIN books b ON b.book_id = rv.book_id
            """;

    public List<Review> listAll() throws SQLException {
        String sql = """
                SELECT %s
                %s
                ORDER BY rv.created_at DESC, rv.review_id DESC
                """.formatted(COLUMNS, FROM);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapReviews(resultSet);
        }
    }

    public List<Review> listByBook(int bookId) throws SQLException {
        String sql = """
                SELECT %s
                %s
                WHERE rv.book_id = ?
                ORDER BY rv.created_at DESC, rv.review_id DESC
                """.formatted(COLUMNS, FROM);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapReviews(resultSet);
            }
        }
    }

    public void create(int recordId, int userId, int bookId, int rating, String comment) throws SQLException {
        String sql = """
                INSERT INTO reviews (record_id, user_id, book_id, rating, comment, created_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recordId);
            statement.setInt(2, userId);
            statement.setInt(3, bookId);
            statement.setInt(4, rating);
            statement.setString(5, comment);
            statement.executeUpdate();
        }
    }

    private List<Review> mapReviews(ResultSet resultSet) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        while (resultSet.next()) {
            reviews.add(new Review(
                    resultSet.getInt("review_id"),
                    resultSet.getInt("record_id"),
                    resultSet.getInt("user_id"),
                    resultSet.getString("student_no"),
                    resultSet.getString("user_name"),
                    resultSet.getInt("book_id"),
                    resultSet.getString("book_title"),
                    resultSet.getInt("rating"),
                    resultSet.getString("comment"),
                    toLocalDateTime(resultSet.getTimestamp("created_at"))
            ));
        }
        return reviews;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
