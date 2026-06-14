package com.librarysystem.model;

public class SubjectPopularity {
    private final String subject;
    private final int borrowCount;

    public SubjectPopularity(String subject, int borrowCount) {
        this.subject = subject;
        this.borrowCount = borrowCount;
    }

    public String getSubject() {
        return subject;
    }

    public int getBorrowCount() {
        return borrowCount;
    }
}
