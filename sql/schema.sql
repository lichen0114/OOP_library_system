CREATE DATABASE IF NOT EXISTS library_system
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE library_system;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS level_requests;
DROP TABLE IF EXISTS borrow_records;
DROP TABLE IF EXISTS book_isbns;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS user_levels;
DROP TABLE IF EXISTS app_settings;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE user_levels (
  level_code VARCHAR(30) PRIMARY KEY,
  display_name VARCHAR(100) NOT NULL,
  max_borrow_days INT NOT NULL,
  max_active_loans INT NOT NULL,
  can_favorite BOOLEAN NOT NULL DEFAULT FALSE,
  is_admin BOOLEAN NOT NULL DEFAULT FALSE,
  registration_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  custom_level BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_levels_admin (is_admin),
  INDEX idx_user_levels_registration (registration_allowed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  student_no VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  role_level VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  INDEX idx_users_name (name),
  INDEX idx_users_role_status (role_level, status),
  CONSTRAINT fk_users_level
    FOREIGN KEY (role_level) REFERENCES user_levels(level_code)
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

CREATE TABLE level_requests (
  request_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  requested_level_code VARCHAR(30) NOT NULL,
  reason TEXT,
  status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
  admin_note TEXT,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL,
  reviewed_by INT NULL,
  INDEX idx_level_requests_user (user_id),
  INDEX idx_level_requests_status (status, requested_at),
  CONSTRAINT fk_level_requests_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_level_requests_level
    FOREIGN KEY (requested_level_code) REFERENCES user_levels(level_code),
  CONSTRAINT fk_level_requests_reviewer
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favorites (
  user_id INT NOT NULL,
  book_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, book_id),
  INDEX idx_favorites_book (book_id),
  CONSTRAINT fk_favorites_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_favorites_book
    FOREIGN KEY (book_id) REFERENCES books(book_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservations (
  reservation_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  book_id INT NOT NULL,
  status ENUM('PENDING', 'NOTIFIED', 'CANCELLED', 'FULFILLED') NOT NULL DEFAULT 'PENDING',
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  notified_at DATETIME NULL,
  fulfilled_at DATETIME NULL,
  cancelled_at DATETIME NULL,
  INDEX idx_reservations_user (user_id, status),
  INDEX idx_reservations_book (book_id, status, requested_at),
  CONSTRAINT fk_reservations_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_reservations_book
    FOREIGN KEY (book_id) REFERENCES books(book_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reviews (
  review_id INT AUTO_INCREMENT PRIMARY KEY,
  record_id INT NOT NULL UNIQUE,
  user_id INT NOT NULL,
  book_id INT NOT NULL,
  rating INT NOT NULL,
  comment TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_reviews_book (book_id, created_at),
  INDEX idx_reviews_user (user_id),
  CONSTRAINT fk_reviews_record
    FOREIGN KEY (record_id) REFERENCES borrow_records(record_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_reviews_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_reviews_book
    FOREIGN KEY (book_id) REFERENCES books(book_id)
    ON DELETE CASCADE,
  CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_settings (
  setting_key VARCHAR(64) PRIMARY KEY,
  setting_value VARCHAR(255) NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
