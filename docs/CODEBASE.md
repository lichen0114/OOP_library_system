# Codebase Guide

This document describes the current JavaFX/MySQL library system implementation. It complements `README.md` by focusing on architecture, data flow, workflows, and business rules that matter when maintaining or extending the code.

## Project Overview

The app is a Java 17 desktop application built with Maven. The UI is JavaFX, persistence is MySQL through MySQL Connector/J, and all database access is plain JDBC with prepared statements.

Runtime entry point:

- `src/main/java/com/librarysystem/ui/LibraryApp.java`

Build and runtime configuration:

- `pom.xml` targets Java 17 and configures `javafx-maven-plugin` with `com.librarysystem.ui.LibraryApp`.
- `src/main/resources/db.properties` provides `db.url`, `db.user`, and `db.password`.
- `src/main/resources/app.css` styles the JavaFX screens.

## Module Map

- `src/main/java/com/librarysystem/model`: immutable read models and enums used across UI, service, and DAO layers.
- `src/main/java/com/librarysystem/dao`: JDBC queries, inserts, updates, joins, and `ResultSet` mapping.
- `src/main/java/com/librarysystem/service`: business rules, validation orchestration, permissions, and transaction boundaries.
- `src/main/java/com/librarysystem/ui`: JavaFX screen construction, event handlers, tables, forms, and alerts.
- `src/main/java/com/librarysystem/util`: database connection loading, validation helpers, password hashing, and date formatting.
- `src/main/resources`: runtime classpath resources.
- `sql/schema.sql`: recreates the MySQL schema.
- `sql/seed.sql`: generated demo data and operational defaults.
- `scripts/generate_seed.py`: regenerates `sql/seed.sql` from the JSON files under `專題預設數據 (1)`.

`target/` is generated Maven output and should not be edited.

## Architecture Flow

The application is intentionally layered:

1. `LibraryApp` builds JavaFX tabs and handles button events synchronously on the JavaFX application thread.
2. UI event handlers call service methods such as `BorrowService.borrowBook`, `ReservationService.reserveBook`, or `AdminService.addBook`.
3. Services enforce business rules, check account level/status, and open transactions when multiple SQL writes must succeed together.
4. DAOs own SQL text, prepared-statement binding, joins, and mapping into model objects.
5. `Database.getConnection()` loads `db.properties` from the classpath and creates a new `DriverManager` connection for each DAO/service operation.

There is no ORM, dependency injection container, background worker queue, or HTTP API. Calls are direct Java method calls inside the desktop process.

## Public Models And Statuses

User permissions come from the `user_levels` table and the `UserLevel` model loaded by `LevelDao`. The older `RoleLevel` enum still exists, but live permissions are database-backed.

Seeded levels:

| Code | Display | Borrow days | Active loans | Favorites | Admin | Registration |
| --- | --- | ---: | ---: | --- | --- | --- |
| `NORMAL` | Normal Student | 7 | 3 | No | No | Yes |
| `VIP` | VIP Student | 14 | 5 | Yes | No | Yes |
| `ADMIN` | System Administrator | 0 | 0 | No | Yes | No |
| `RESEARCH` | Research Student | 14 | 8 | Yes | No | No |

Database-backed statuses:

- Books: `AVAILABLE`, `BORROWED`, `REMOVED`.
- Users: `ACTIVE`, `SUSPENDED`.
- Reservations: `PENDING`, `NOTIFIED`, `CANCELLED`, `FULFILLED`.
- Level requests: `PENDING`, `APPROVED`, `REJECTED`.

Important derived fields:

- `Book.isbns` is aggregated from `book_isbns` with `GROUP_CONCAT`.
- `BorrowRecord.overdueDays` is computed in `BorrowRecordDao` using `TIMESTAMPDIFF` against `return_date` or `NOW()`.
- `BorrowRecord.fineAmount` is computed from overdue days and the current `fine_per_overdue_day` setting.
- Reminder rows are active loans with `due_date <= NOW() + reminder_days`.
- Review data is left-joined into borrow-record read models.
- Subject popularity is derived in `AdminService` by splitting comma-separated `books.subjects` values from borrow records.

## Student Workflows

The unauthenticated screen has three tabs: `Student Login`, `Admin Login`, and `Register`.

Student login:

- `AuthService.loginStudent` verifies the student number and password.
- Admin accounts are rejected from the student tab.
- Suspended users can log in; the UI warns that borrowing, reservations, and favorites are disabled.

Registration:

- The registration tab loads levels where `registration_allowed = TRUE` and `is_admin = FALSE`.
- `AuthService.register` rejects duplicate student numbers, unknown levels, admin levels, and non-registration levels.
- New accounts are created as `ACTIVE`.

Student dashboard tabs:

- `Search & Borrow`: searches title, author, subject, publisher, or ISBN; shows details and reviews; can borrow, reserve, or add favorites.
- `Return`: lists active loans and returns the selected book.
- `My History & Reviews`: lists the student's borrow history and creates a review for a returned record.
- `Book History`: accepts a book ID and shows all borrow records for that book.
- `Reservations`: lists the student's reservations and can cancel active reservations.
- `Favorites`: enabled only for levels with `can_favorite`; lists favorites, borrows selected favorites, or removes them.
- `Reminders & Fines`: lists active loans due within the configured reminder window and shows the current fine rate.
- `Level Request`: lists the student's level requests and submits a request for another non-admin level.

## Admin Workflows

Admin login:

- `AuthService.loginAdmin` verifies credentials and requires the loaded user level to have `is_admin = TRUE`.
- Non-admin accounts are rejected from the admin tab.

Admin dashboard tabs:

- `Records`: lists all borrow records, including derived overdue/fine/review columns.
- `Student Search`: searches borrow records by student number or name.
- `Books`: searches books, adds new books with optional ISBNs, and marks selected books as `REMOVED`.
- `Users`: searches users, suspends/reactivates non-admin users, and assigns non-admin levels.
- `Level Requests`: approves or rejects pending level requests with an optional admin note.
- `Reservations`: lists all reservations.
- `Reviews`: lists all reviews.
- `Levels & Settings`: views levels, creates/updates custom non-admin levels, and edits reminder/fine settings.
- `Subject Popularity`: shows the top derived subject counts in a table and bar chart.
- `Statistics`: shows aggregate counts for books, users, records, reservations, reviews, and pending level requests.

## Data Model

The schema currently contains ten tables:

| Table | Purpose |
| --- | --- |
| `user_levels` | Permission source for borrow limits, favorites, admin flag, registration eligibility, and custom levels. |
| `users` | Student/admin accounts with password hash, level, creation time, and active/suspended status. |
| `books` | Book metadata and status. Removed books are soft-deleted with status `REMOVED`. |
| `book_isbns` | One-to-many ISBN child table for books. |
| `borrow_records` | Borrow transactions with borrow date, due date, return date, and selected borrow period. |
| `level_requests` | Student requests to change level, with review status, admin note, reviewer, and timestamps. |
| `favorites` | User/book join table for favorite-enabled student levels. |
| `reservations` | Holds, notification state, fulfillment, cancellation, and timestamps. |
| `reviews` | One review per borrow record, with a 1-5 rating and optional comment. |
| `app_settings` | Key/value operational settings such as reminder window and fine rate. |

Relationships:

- `users.role_level -> user_levels.level_code`.
- `book_isbns.book_id -> books.book_id` with cascade delete.
- `borrow_records.user_id -> users.user_id`; `borrow_records.book_id -> books.book_id`.
- `level_requests.user_id -> users.user_id`; `requested_level_code -> user_levels.level_code`; `reviewed_by -> users.user_id`.
- `favorites.user_id -> users.user_id`; `favorites.book_id -> books.book_id`; both cascade delete.
- `reservations.user_id -> users.user_id`; `reservations.book_id -> books.book_id`; both cascade delete.
- `reviews.record_id -> borrow_records.record_id` and is unique; reviews also reference the reviewing user and book.

## Business Rules

Borrowing:

- Admin accounts cannot borrow.
- Suspended accounts cannot borrow.
- Borrow days must be one of `1`, `3`, `7`, or `14`.
- The selected period must not exceed the user's level `max_borrow_days`.
- The user's active loan count must be below `max_active_loans`.
- Only `AVAILABLE` books can be borrowed.
- Borrowing inserts a `borrow_records` row and updates the book status to `BORROWED` in one transaction.

Returning:

- Return locks the user's latest active record for the book.
- Return sets `return_date = NOW()`.
- If there is a pending reservation for the book, the earliest pending reservation is marked `NOTIFIED`.
- The book status is set to `AVAILABLE` even when a reservation is notified.

Reservations:

- Admin and suspended accounts cannot reserve.
- Only currently `BORROWED` books can be reserved; available books are meant to be borrowed directly.
- A user cannot reserve a book they already have on active loan.
- A user cannot have more than one active reservation (`PENDING` or `NOTIFIED`) for the same book.
- On borrow, if the book has an active reservation, only the earliest `NOTIFIED` reservation's owner may borrow it; that reservation is then marked `FULFILLED`.
- There is no expiration timer for `NOTIFIED` reservations.
- Cancelling a `NOTIFIED` reservation does not automatically notify the next pending reservation.

Favorites:

- Admin accounts cannot use favorites.
- Suspended accounts cannot use favorites.
- Only levels with `can_favorite = TRUE` can list, add, or remove favorites.
- Removed books cannot be favorited.
- Duplicate favorites are rejected.

Reviews:

- Admin accounts cannot create student reviews.
- Ratings must be from 1 to 5.
- A student can review only their own borrow records.
- The borrow record must be returned before review.
- Each borrow record can have at most one review.

Levels:

- Level requests are student-only and cannot target admin levels.
- A student cannot request their current level.
- A student can have only one pending level request at a time.
- Admin approval updates the user's level and marks the request `APPROVED` in one transaction.
- Admin rejection marks the request `REJECTED`.
- User-management assignment cannot assign admin level and cannot reassign administrator accounts.
- Custom level codes must be 2-30 characters using `A-Z`, `0-9`, or `_`.
- Custom level `max_borrow_days` must be 1-14; `max_active_loans` must be 1-50.
- Only custom non-admin levels can be edited through the app.

Settings and fines:

- `reminder_days` and `fine_per_overdue_day` cannot be negative.
- If a setting is missing or non-numeric, `SettingsDao` falls back to `reminder_days = 3` and `fine_per_overdue_day = 5`.
- Fines are not stored on borrow records; changing `fine_per_overdue_day` changes displayed fines for historical rows.

Soft deletion and status:

- Books are removed by changing status to `REMOVED`.
- A borrowed book cannot be removed until returned.
- `BookDao.search` does not hide removed books; the UI shows the status column.
- Suspended users can still log in and view data. Borrowing, reservations, and favorites are explicitly blocked; reviews and level requests are not blocked by suspension checks in the service layer.

Password handling:

- New registrations store SHA-256 hashes.
- `PasswordUtil.verify` accepts either a SHA-256 match or exact raw stored value match, which keeps imported demo/user data usable.

## Seed And Configuration Notes

Database setup is split into schema and generated seed data:

```bash
mysql -uroot < sql/schema.sql
mysql -uroot < sql/seed.sql
```

Seed defaults:

- Built-in levels: `NORMAL`, `VIP`, `ADMIN`, `RESEARCH`.
- App settings: `reminder_days = 3`, `fine_per_overdue_day = 5`.
- Demo accounts: `S001 / password123`, `S002 / password123`, `admin / admin123`.
- JSON-backed data: 20 imported users, 200 books, ISBNs, and 30 borrow records.
- Extra demo data: borrow history for demo users, reviews, favorites, reservations, pending level requests, and two removed books for admin testing.

`scripts/generate_seed.py` converts relative date strings such as `-45 days` into `DATE_ADD(NOW(), INTERVAL ... DAY)`, so seeded due dates and overdue states are relative to the time `sql/seed.sql` is imported.

When dataset files change, regenerate the seed:

```bash
python3 scripts/generate_seed.py
```

## Development Notes

Common commands:

```bash
mvn clean compile
mvn javafx:run
python3 scripts/generate_seed.py
```

Manual database verification examples:

```bash
mysql -uroot -e "USE library_system; SELECT COUNT(*) FROM books;"
mysql -uroot -e "USE library_system; SELECT status, COUNT(*) FROM books GROUP BY status;"
```

There is no automated test framework configured. Before submitting code changes, compile with Maven and manually verify affected flows in the JavaFX app. For database-related changes, reload both SQL files and check expected table counts and statuses.

Implementation conventions:

- Keep SQL in DAOs or SQL files.
- Use prepared statements for runtime database queries.
- Keep multi-step transactions in services.
- Avoid adding business rules directly to JavaFX event handlers.
- Do not edit generated build output under `target/`.
- Treat `sql/seed.sql` as generated; edit `scripts/generate_seed.py` or the JSON dataset when changing seed generation.
