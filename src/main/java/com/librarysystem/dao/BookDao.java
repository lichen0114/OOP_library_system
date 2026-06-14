package com.librarysystem.dao;

import com.librarysystem.model.Book;
import com.librarysystem.model.BookStatus;
import com.librarysystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BookDao {
    private static final String BOOK_COLUMNS = """
            b.book_id, b.title, b.authors, b.subjects, b.publisher, b.publish_year,
            b.edition, b.format_desc, b.source, b.note, b.status
            """;

    public List<Book> search(String query) throws SQLException {
        String sql = """
                SELECT %s, COALESCE(GROUP_CONCAT(DISTINCT i.isbn ORDER BY i.isbn SEPARATOR ', '), '') AS isbns
                FROM books b
                LEFT JOIN book_isbns i ON i.book_id = b.book_id
                WHERE ? = ''
                   OR b.title LIKE ?
                   OR b.authors LIKE ?
                   OR b.subjects LIKE ?
                   OR b.publisher LIKE ?
                   OR i.isbn LIKE ?
                GROUP BY %s
                ORDER BY b.book_id
                LIMIT 300
                """.formatted(BOOK_COLUMNS, BOOK_COLUMNS);
        String normalized = query == null ? "" : query.trim();
        String like = "%" + normalized + "%";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setString(5, like);
            statement.setString(6, like);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Book> books = new ArrayList<>();
                while (resultSet.next()) {
                    books.add(mapBook(resultSet));
                }
                return books;
            }
        }
    }

    public Optional<Book> findById(int bookId) throws SQLException {
        String sql = """
                SELECT %s, COALESCE(GROUP_CONCAT(DISTINCT i.isbn ORDER BY i.isbn SEPARATOR ', '), '') AS isbns
                FROM books b
                LEFT JOIN book_isbns i ON i.book_id = b.book_id
                WHERE b.book_id = ?
                GROUP BY %s
                """.formatted(BOOK_COLUMNS, BOOK_COLUMNS);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapBook(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public int addBook(Book book) throws SQLException {
        String bookSql = """
                INSERT INTO books
                    (title, authors, subjects, publisher, publish_year, edition, format_desc, source, note, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'AVAILABLE')
                """;
        String isbnSql = "INSERT INTO book_isbns (book_id, isbn) VALUES (?, ?)";
        Connection connection = Database.getConnection();
        try {
            connection.setAutoCommit(false);
            int bookId;
            try (PreparedStatement bookStatement = connection.prepareStatement(bookSql, Statement.RETURN_GENERATED_KEYS)) {
                bookStatement.setString(1, book.getTitle());
                bookStatement.setString(2, book.getAuthors());
                bookStatement.setString(3, book.getSubjects());
                bookStatement.setString(4, book.getPublisher());
                if (book.getPublishYear() == null) {
                    bookStatement.setNull(5, java.sql.Types.INTEGER);
                } else {
                    bookStatement.setInt(5, book.getPublishYear());
                }
                bookStatement.setString(6, book.getEdition());
                bookStatement.setString(7, book.getFormatDesc());
                bookStatement.setString(8, book.getSource());
                bookStatement.setString(9, book.getNote());
                bookStatement.executeUpdate();
                try (ResultSet keys = bookStatement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Book was inserted but no generated key was returned.");
                    }
                    bookId = keys.getInt(1);
                }
            }
            try (PreparedStatement isbnStatement = connection.prepareStatement(isbnSql)) {
                for (String isbn : book.getIsbns()) {
                    if (!isbn.isBlank()) {
                        isbnStatement.setInt(1, bookId);
                        isbnStatement.setString(2, isbn.trim());
                        isbnStatement.addBatch();
                    }
                }
                isbnStatement.executeBatch();
            }
            connection.commit();
            return bookId;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw e;
        } finally {
            closeQuietly(connection);
        }
    }

    public void updateStatus(int bookId, BookStatus status) throws SQLException {
        String sql = "UPDATE books SET status = ? WHERE book_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, bookId);
            statement.executeUpdate();
        }
    }

    public boolean hasActiveLoan(int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE book_id = ? AND return_date IS NULL";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private Book mapBook(ResultSet resultSet) throws SQLException {
        int publishYear = resultSet.getInt("publish_year");
        Integer nullableYear = resultSet.wasNull() ? null : publishYear;
        String isbnText = resultSet.getString("isbns");
        List<String> isbns = isbnText == null || isbnText.isBlank()
                ? List.of()
                : Arrays.stream(isbnText.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return new Book(
                resultSet.getInt("book_id"),
                resultSet.getString("title"),
                resultSet.getString("authors"),
                resultSet.getString("subjects"),
                resultSet.getString("publisher"),
                nullableYear,
                resultSet.getString("edition"),
                resultSet.getString("format_desc"),
                resultSet.getString("source"),
                resultSet.getString("note"),
                BookStatus.valueOf(resultSet.getString("status")),
                isbns
        );
    }

    private void rollbackQuietly(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException ignored) {
            // Preserve the original SQL failure.
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
