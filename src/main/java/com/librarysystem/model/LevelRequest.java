package com.librarysystem.model;

import java.time.LocalDateTime;

public class LevelRequest {
    private final int requestId;
    private final int userId;
    private final String studentNo;
    private final String userName;
    private final String currentLevelCode;
    private final String requestedLevelCode;
    private final String requestedLevelName;
    private final String reason;
    private final String status;
    private final String adminNote;
    private final LocalDateTime requestedAt;
    private final LocalDateTime reviewedAt;
    private final String reviewerName;

    public LevelRequest(int requestId, int userId, String studentNo, String userName,
                        String currentLevelCode, String requestedLevelCode, String requestedLevelName,
                        String reason, String status, String adminNote, LocalDateTime requestedAt,
                        LocalDateTime reviewedAt, String reviewerName) {
        this.requestId = requestId;
        this.userId = userId;
        this.studentNo = studentNo;
        this.userName = userName;
        this.currentLevelCode = currentLevelCode;
        this.requestedLevelCode = requestedLevelCode;
        this.requestedLevelName = requestedLevelName;
        this.reason = reason;
        this.status = status;
        this.adminNote = adminNote;
        this.requestedAt = requestedAt;
        this.reviewedAt = reviewedAt;
        this.reviewerName = reviewerName;
    }

    public int getRequestId() {
        return requestId;
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

    public String getCurrentLevelCode() {
        return currentLevelCode;
    }

    public String getRequestedLevelCode() {
        return requestedLevelCode;
    }

    public String getRequestedLevelName() {
        return requestedLevelName;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewerName() {
        return reviewerName;
    }
}
