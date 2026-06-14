package com.librarysystem.service;

import com.librarysystem.dao.BookDao;
import com.librarysystem.model.Book;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class BookService {
    private final BookDao bookDao = new BookDao();

    public List<Book> search(String query) throws SQLException {
        return bookDao.search(query);
    }

    public Optional<Book> findById(int bookId) throws SQLException {
        return bookDao.findById(bookId);
    }
}
