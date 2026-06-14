CREATE DATABASE IF NOT EXISTS library_system
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE library_system;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS borrow_records;
DROP TABLE IF EXISTS book_isbns;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  student_no VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  role_level ENUM('NORMAL', 'VIP', 'ADMIN') NOT NULL DEFAULT 'NORMAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  INDEX idx_users_name (name),
  INDEX idx_users_role_status (role_level, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE books (
  book_id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  authors TEXT,
  subjects TEXT,
  publisher VARCHAR(255),
  publish_year INT,
  edition VARCHAR(100),
  format_desc VARCHAR(255),
  source VARCHAR(100),
  note TEXT,
  status ENUM('AVAILABLE', 'BORROWED', 'REMOVED') NOT NULL DEFAULT 'AVAILABLE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_books_title (title),
  INDEX idx_books_publisher (publisher),
  INDEX idx_books_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE book_isbns (
  isbn_id INT AUTO_INCREMENT PRIMARY KEY,
  book_id INT NOT NULL,
  isbn VARCHAR(32) NOT NULL,
  UNIQUE KEY uk_book_isbn (book_id, isbn),
  INDEX idx_book_isbn (isbn),
  CONSTRAINT fk_book_isbns_book
    FOREIGN KEY (book_id) REFERENCES books(book_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE borrow_records (
  record_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  book_id INT NOT NULL,
  borrow_date DATETIME NOT NULL,
  due_date DATETIME NOT NULL,
  return_date DATETIME NULL,
  borrow_days INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_borrow_user (user_id),
  INDEX idx_borrow_book (book_id),
  INDEX idx_borrow_active (return_date),
  INDEX idx_borrow_due (due_date),
  CONSTRAINT fk_borrow_user
    FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_borrow_book
    FOREIGN KEY (book_id) REFERENCES books(book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
