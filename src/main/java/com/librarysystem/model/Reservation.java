package com.librarysystem.model;

import java.time.LocalDateTime;

public class Reservation {
    private final int reservationId;
    private final int userId;
    private final String studentNo;
    private final String userName;
    private final int bookId;
    private final String bookTitle;
    private final String status;
    private final LocalDateTime requestedAt;
    private final LocalDateTime notifiedAt;

    public Reservation(int reservationId, int userId, String studentNo, String userName,
                       int bookId, String bookTitle, String status,
                       LocalDateTime requestedAt, LocalDateTime notifiedAt) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.studentNo = studentNo;
        this.userName = userName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.status = status;
        this.requestedAt = requestedAt;
        this.notifiedAt = notifiedAt;
    }

    public int getReservationId() {
        return reservationId;
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

    public String getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }
}
