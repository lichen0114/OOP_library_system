package com.librarysystem.service;

import com.librarysystem.dao.BorrowRecordDao;
import com.librarysystem.dao.ReviewDao;
import com.librarysystem.model.BorrowRecord;
import com.librarysystem.model.Review;
import com.librarysystem.model.User;

import java.sql.SQLException;
import java.util.List;

public class ReviewService {
    private final BorrowRecordDao borrowRecordDao = new BorrowRecordDao();
    private final ReviewDao reviewDao = new ReviewDao();

    public List<Review> listBookReviews(int bookId) throws SQLException {
        return reviewDao.listByBook(bookId);
    }

    public List<Review> listAllReviews() throws SQLException {
        return reviewDao.listAll();
    }

    public void createReview(User user, int recordId, int rating, String comment) throws SQLException {
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Admin accounts cannot create book reviews.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        BorrowRecord record = borrowRecordDao.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow record not found."));
        if (record.getUserId() != user.getUserId()) {
            throw new IllegalArgumentException("You can review only your own borrowing records.");
        }
        if (record.getReturnDate() == null) {
            throw new IllegalArgumentException("Only returned books can be reviewed.");
        }
        if (record.getReviewId() != null) {
            throw new IllegalArgumentException("This borrowing record already has a review.");
        }
        reviewDao.create(recordId, user.getUserId(), record.getBookId(), rating, emptyToNull(comment));
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
