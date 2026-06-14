# Library Borrowing and Returning System

JavaFX desktop application for library borrowing and returning, built with Maven, MySQL, and JDBC.

## Demo Accounts

| Role | Account | Password |
| --- | --- | --- |
| Admin | `admin` | `admin123` |
| Normal student | `S001` | `password123` |
| VIP student | `S002` | `password123` |

## Requirements

- Java 17 or newer
- Maven 3.9+
- MySQL 5.7+ or 8.x

The app reads database settings from `src/main/resources/db.properties`.

```properties
db.url=jdbc:mysql://localhost:3306/library_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei&characterEncoding=utf8
db.user=root
db.password=
```

Update `db.user` and `db.password` if your local MySQL account is different.

## Database Setup

Start MySQL, then import the schema and seed data:

```bash
mysql -uroot < sql/schema.sql
mysql -uroot < sql/seed.sql
```

If your root account uses a password:

```bash
mysql -uroot -p < sql/schema.sql
mysql -uroot -p < sql/seed.sql
```

The generated seed imports:

- 20 users from `專題預設數據 (1)/使用者資料/Users.json`
- 200 books from `專題預設數據 (1)/書籍資料/Books.json`
- ISBNs into `book_isbns`
- 30 borrow records from `專題預設數據 (1)/借還紀錄資料/Borrow_records.json`
- 3 extra demo borrow records for `S001` and `S002`
- Demo accounts `S001`, `S002`, and `admin`

Relative borrow dates such as `-45 days` and `4 days` are converted to MySQL expressions relative to import time.

To regenerate `sql/seed.sql` from the JSON files:

```bash
python3 scripts/generate_seed.py
```

## Build And Run

Compile:

```bash
mvn clean compile
```

Run the JavaFX app:

```bash
mvn javafx:run
```

## Features

Students can register, log in, search by title/author/subject/publisher/ISBN, borrow available books, return active loans, inspect personal history, inspect book history, and view due-date reminders.

Admins can view all borrow records, search records by student, add books with ISBNs, mark books removed, suspend/reactivate users, and view system statistics.

Rules enforced by the service layer:

- `NORMAL`: 7-day loan period, max 3 active loans
- `VIP`: 14-day loan period, max 5 active loans
- Suspended users can log in but cannot borrow
- Borrowed and removed books cannot be borrowed
- Borrow and return operations run in transactions
