package com.librarysystem.model;

import java.time.LocalDateTime;

public class Review {
    private final int reviewId;
    private final int recordId;
    private final int userId;
    private final String studentNo;
    private final String userName;
    private final int bookId;
    private final String bookTitle;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;

    public Review(int reviewId, int recordId, int userId, String studentNo, String userName,
                  int bookId, String bookTitle, int rating, String comment, LocalDateTime createdAt) {
        this.reviewId = reviewId;
        this.recordId = recordId;
        this.userId = userId;
        this.studentNo = studentNo;
        this.userName = userName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public int getReviewId() {
        return reviewId;
    }

    public int getRecordId() {
        return recordId;
    }

    public int getUserId() {
        return userId;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public String getUserName() {
        return userName;
    }

    public int getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getSummary() {
        String text = rating + "/5";
        if (comment != null && !comment.isBlank()) {
            text += " - " + comment;
        }
        return text;
    }
}
