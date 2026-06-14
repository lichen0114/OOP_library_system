package com.librarysystem.service;

import com.librarysystem.dao.BookDao;
import com.librarysystem.dao.FavoriteDao;
import com.librarysystem.model.Book;
import com.librarysystem.model.BookStatus;
import com.librarysystem.model.User;
import com.librarysystem.model.UserStatus;

import java.sql.SQLException;
import java.util.List;

public class FavoriteService {
    private final BookDao bookDao = new BookDao();
    private final FavoriteDao favoriteDao = new FavoriteDao();

    public List<Book> listFavorites(User user) throws SQLException {
        requireFavoriteAccess(user);
        return bookDao.listFavorites(user.getUserId());
    }

    public void addFavorite(User user, int bookId) throws SQLException {
        requireFavoriteAccess(user);
        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        if (book.getStatus() == BookStatus.REMOVED) {
            throw new IllegalArgumentException("Removed books cannot be favorited.");
        }
        if (favoriteDao.exists(user.getUserId(), bookId)) {
            throw new IllegalArgumentException("This book is already in favorites.");
        }
        favoriteDao.add(user.getUserId(), bookId);
    }

    public void removeFavorite(User user, int bookId) throws SQLException {
        requireFavoriteAccess(user);
        favoriteDao.remove(user.getUserId(), bookId);
    }

    private void requireFavoriteAccess(User user) {
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Admin accounts cannot use student favorites.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("This account is suspended and cannot use favorites.");
        }
        if (!user.getUserLevel().canFavorite()) {
            throw new IllegalArgumentException("Favorites are available only to VIP or custom levels with favorites enabled.");
        }
    }
}
