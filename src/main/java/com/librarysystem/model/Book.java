package com.librarysystem.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Book {
    private final int bookId;
    private final String title;
    private final String authors;
    private final String subjects;
    private final String publisher;
    private final Integer publishYear;
    private final String edition;
    private final String formatDesc;
    private final String source;
    private final String note;
    private final BookStatus status;
    private final List<String> isbns;

    public Book(int bookId, String title, String authors, String subjects, String publisher,
                Integer publishYear, String edition, String formatDesc, String source,
                String note, BookStatus status, List<String> isbns) {
        this.bookId = bookId;
        this.title = title;
        this.authors = authors;
        this.subjects = subjects;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.edition = edition;
        this.formatDesc = formatDesc;
        this.source = source;
        this.note = note;
        this.status = status;
        this.isbns = new ArrayList<>(isbns == null ? List.of() : isbns);
    }

    public int getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public String getSubjects() {
        return subjects;
    }

    public String getPublisher() {
        return publisher;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public String getEdition() {
        return edition;
    }

    public String getFormatDesc() {
        return formatDesc;
    }

    public String getSource() {
        return source;
    }

    public String getNote() {
        return note;
    }

    public BookStatus getStatus() {
        return status;
    }

    public List<String> getIsbns() {
        return Collections.unmodifiableList(isbns);
    }

    public String getIsbnText() {
        return String.join(", ", isbns);
    }
}
