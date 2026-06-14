# Repository Guidelines

## Project Structure & Module Organization

This is a Maven JavaFX desktop app backed by MySQL/JDBC.

- `src/main/java/com/librarysystem/model`: entities and enums.
- `src/main/java/com/librarysystem/dao`: JDBC data access using prepared statements.
- `src/main/java/com/librarysystem/service`: business rules, transactions, auth, borrow/return, and admin operations.
- `src/main/java/com/librarysystem/ui`: JavaFX application and screen construction.
- `src/main/java/com/librarysystem/util`: database, validation, password, and date helpers.
- `src/main/resources`: runtime resources such as `db.properties` and `app.css`.
- `sql/schema.sql` and `sql/seed.sql`: MySQL schema and seed data.
- `scripts/generate_seed.py`: regenerates `sql/seed.sql` from the JSON dataset in `專題預設數據 (1)`.

Do not edit generated build output under `target/`.

## Build, Test, and Development Commands

- `mvn clean compile`: cleans and compiles the Java sources.
- `mvn javafx:run`: launches the JavaFX app.
- `mysql -uroot < sql/schema.sql`: creates/recreates the `library_system` database schema.
- `mysql -uroot < sql/seed.sql`: imports the book, user, ISBN, and borrow-record seed data.
- `python3 scripts/generate_seed.py`: regenerates seed SQL after dataset changes.

Update `src/main/resources/db.properties` if your MySQL user, password, host, or port differs.

## Coding Style & Naming Conventions

Target Java 17. Use 4-space indentation, descriptive names, and package-aligned responsibilities. Class names use `PascalCase`; methods, fields, and local variables use `camelCase`; enum constants use `UPPER_SNAKE_CASE`.

Keep SQL in DAOs or SQL files, use prepared statements for runtime queries, and keep transaction boundaries in services. Avoid putting business rules directly in JavaFX UI code.

## Testing Guidelines

No automated test framework is currently configured. Before submitting changes, run `mvn clean compile` and manually verify affected flows with `mvn javafx:run`.

For database-related changes, reload both SQL files and verify expected counts, for example:

```bash
mysql -uroot -e "USE library_system; SELECT COUNT(*) FROM books;"
```

## Commit & Pull Request Guidelines

The repository history is minimal, so use clear imperative commit messages such as `Add admin user management` or `Fix borrow transaction rollback`.

Pull requests should include a short summary, manual verification steps, database migration/seed notes if relevant, and screenshots for UI changes. Mention any changes to demo credentials or `db.properties` expectations.

## Security & Configuration Tips

Do not commit real database passwords. Keep demo credentials limited to local development. Preserve password hashing for new accounts and demo users.
