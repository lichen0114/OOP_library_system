package com.librarysystem.model;

import java.time.LocalDateTime;

public class BorrowRecord {
    private final int recordId;
    private final int userId;
    private final String studentNo;
    private final String userName;
    private final int bookId;
    private final String bookTitle;
    private final LocalDateTime borrowDate;
    private final LocalDateTime dueDate;
    private final LocalDateTime returnDate;
    private final int borrowDays;
    private final LocalDateTime createdAt;
    private final int overdueDays;
    private final int fineAmount;
    private final Integer reviewId;
    private final Integer reviewRating;
    private final String reviewComment;

    public BorrowRecord(int recordId, int userId, String studentNo, String userName,
                        int bookId, String bookTitle, LocalDateTime borrowDate,
                        LocalDateTime dueDate, LocalDateTime returnDate,
                        int borrowDays, LocalDateTime createdAt,
                        int overdueDays, int fineAmount,
                        Integer reviewId, Integer reviewRating, String reviewComment) {
        this.recordId = recordId;
        this.userId = userId;
        this.studentNo = studentNo;
        this.userName = userName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.borrowDays = borrowDays;
        this.createdAt = createdAt;
        this.overdueDays = overdueDays;
        this.fineAmount = fineAmount;
        this.reviewId = reviewId;
        this.reviewRating = reviewRating;
        this.reviewComment = reviewComment;
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

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public int getBorrowDays() {
        return borrowDays;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getOverdueDays() {
        return overdueDays;
    }

    public int getFineAmount() {
        return fineAmount;
    }

    public Integer getReviewId() {
        return reviewId;
    }

    public Integer getReviewRating() {
        return reviewRating;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public String getReturnStatus() {
        if (returnDate != null) {
            return "Returned";
        }
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            return "Overdue";
        }
        return "Borrowed";
    }

    public String getReviewSummary() {
        if (reviewId == null || reviewRating == null) {
            return "";
        }
        String summary = reviewRating + "/5";
        if (reviewComment != null && !reviewComment.isBlank()) {
            summary += " - " + reviewComment;
        }
        return summary;
    }
}
