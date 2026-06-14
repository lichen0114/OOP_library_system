package com.librarysystem.dao;

import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FavoriteDao {
    public boolean exists(int userId, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND book_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public void add(int userId, int bookId) throws SQLException {
        String sql = "INSERT INTO favorites (user_id, book_id, created_at) VALUES (?, ?, NOW())";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, bookId);
            statement.executeUpdate();
        }
    }

    public void remove(int userId, int bookId) throws SQLException {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND book_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, bookId);
            statement.executeUpdate();
        }
    }
}
