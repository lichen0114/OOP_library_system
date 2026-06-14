package com.librarysystem.ui;

import com.librarysystem.model.Book;
import com.librarysystem.model.BorrowRecord;
import com.librarysystem.model.User;
import com.librarysystem.service.AdminService;
import com.librarysystem.service.AuthService;
import com.librarysystem.service.BookService;
import com.librarysystem.service.BorrowService;
import com.librarysystem.util.DateUtil;
import com.librarysystem.util.ValidationUtil;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LibraryApp extends Application {
    private final AuthService authService = new AuthService();
    private final BookService bookService = new BookService();
    private final BorrowService borrowService = new BorrowService();
    private final AdminService adminService = new AdminService();

    private Stage primaryStage;
    private User currentUser;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("Library Borrowing and Returning System");
        showAuthScreen();
        stage.show();
    }

    private void showAuthScreen() {
        Label title = new Label("Library Borrowing and Returning System");
        title.getStyleClass().add("app-title");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(loginTab(), registerTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox root = new VBox(18, title, tabPane);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(32));
        root.getStyleClass().add("auth-root");
        showScene(root, 900, 620);
    }

    private Tab loginTab() {
        TextField studentNoField = new TextField();
        studentNoField.setPromptText("admin, S001, S002, or student number");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Label message = new Label();
        message.getStyleClass().add("muted-label");
        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-button");

        loginButton.setOnAction(event -> {
            try {
                currentUser = authService.login(studentNoField.getText(), passwordField.getText());
                if (currentUser.isSuspended()) {
                    showInfo("Account suspended", "Login is allowed, but borrowing is disabled for this account.");
                }
                if (currentUser.isAdmin()) {
                    showAdminDashboard();
                } else {
                    showStudentDashboard();
                }
            } catch (Exception e) {
                message.setText(readableError(e));
            }
        });
        passwordField.setOnAction(event -> loginButton.fire());

        VBox box = formBox(
                new Label("Sign in"),
                labeled("Account", studentNoField),
                labeled("Password", passwordField),
                loginButton,
                message,
                new Label("Demo accounts: admin / admin123, S001 / password123, S002 / password123")
        );
        return new Tab("Login", box);
    }

    private Tab registerTab() {
        TextField studentNoField = new TextField();
        studentNoField.setPromptText("Student number");
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Label message = new Label();
        message.getStyleClass().add("muted-label");
        Button registerButton = new Button("Register normal student");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setOnAction(event -> {
            try {
                currentUser = authService.register(studentNoField.getText(), nameField.getText(), passwordField.getText());
                showStudentDashboard();
            } catch (Exception e) {
                message.setText(readableError(e));
            }
        });
        passwordField.setOnAction(event -> registerButton.fire());

        VBox box = formBox(
                new Label("Create student account"),
                labeled("Student number", studentNoField),
                labeled("Name", nameField),
                labeled("Password", passwordField),
                registerButton,
                message
        );
        return new Tab("Register", box);
    }

    private void showStudentDashboard() {
        BorderPane root = dashboardRoot("Student Dashboard");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                studentSearchTab(),
                studentReturnTab(),
                studentHistoryTab(),
                studentBookHistoryTab(),
                studentReminderTab()
        );
        root.setCenter(tabs);
        showScene(root, 1180, 760);
    }

    private Tab studentSearchTab() {
        TextField queryField = new TextField();
        queryField.setPromptText("Search title, author, subject, publisher, or ISBN");
        Button searchButton = new Button("Search");
        Button borrowButton = new Button("Borrow selected");
        TableView<Book> table = createBookTable();
        Runnable refresh = () -> loadBooks(table, queryField.getText());
        searchButton.setOnAction(event -> refresh.run());
        queryField.setOnAction(event -> refresh.run());
        borrowButton.setOnAction(event -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("No book selected", "Select a book first.");
                return;
            }
            try {
                borrowService.borrowBook(currentUser, selected.getBookId());
                showInfo("Borrowed", "The selected book was borrowed.");
                showStudentDashboard();
            } catch (Exception e) {
                showError("Could not borrow book", e);
            }
        });
        refresh.run();

        VBox box = new VBox(12, toolbar(queryField, searchButton, borrowButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Search & Borrow", box);
    }

    private Tab studentReturnTab() {
        TableView<BorrowRecord> table = createRecordTable(false);
        Button returnButton = new Button("Return selected");
        Runnable refresh = () -> loadRecords(table, () -> borrowService.listActiveLoans(currentUser));
        returnButton.setOnAction(event -> {
            BorrowRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("No loan selected", "Select an active loan first.");
                return;
            }
            try {
                borrowService.returnBook(currentUser, selected.getBookId());
                showInfo("Returned", "The selected book was returned.");
                showStudentDashboard();
            } catch (Exception e) {
                showError("Could not return book", e);
            }
        });
        refresh.run();

        VBox box = new VBox(12, toolbar(returnButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Return", box);
    }

    private Tab studentHistoryTab() {
        TableView<BorrowRecord> table = createRecordTable(false);
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> loadRecords(table, () -> borrowService.listHistory(currentUser)));
        refreshButton.fire();

        VBox box = new VBox(12, toolbar(refreshButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("My History", box);
    }

    private Tab studentBookHistoryTab() {
        TextField bookIdField = new TextField();
        bookIdField.setPromptText("Book ID");
        Button searchButton = new Button("Show history");
        TableView<BorrowRecord> table = createRecordTable(true);
        searchButton.setOnAction(event -> {
            try {
                int bookId = Integer.parseInt(bookIdField.getText().trim());
                loadRecords(table, () -> borrowService.listBookHistory(bookId));
            } catch (NumberFormatException e) {
                showError("Invalid book ID", e);
            }
        });
        bookIdField.setOnAction(event -> searchButton.fire());

        VBox box = new VBox(12, toolbar(bookIdField, searchButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Book History", box);
    }

    private Tab studentReminderTab() {
        TableView<BorrowRecord> table = createRecordTable(false);
        Button refreshButton = new Button("Refresh reminders");
        refreshButton.setOnAction(event -> loadRecords(table, () -> borrowService.listActiveLoans(currentUser)));
        refreshButton.fire();

        Label hint = new Label("Active loans are sorted by due date. Rows with Status = Overdue need immediate return.");
        hint.getStyleClass().add("muted-label");
        VBox box = new VBox(12, toolbar(refreshButton), hint, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Reminders", box);
    }

    private void showAdminDashboard() {
        BorderPane root = dashboardRoot("Admin Dashboard");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                adminRecordsTab(),
                adminStudentSearchTab(),
                adminBookTab(),
                adminUsersTab(),
                adminStatisticsTab()
        );
        root.setCenter(tabs);
        showScene(root, 1260, 820);
    }

    private Tab adminRecordsTab() {
        TableView<BorrowRecord> table = createRecordTable(true);
        Button refreshButton = new Button("Refresh all records");
        refreshButton.setOnAction(event -> loadRecords(table, adminService::listAllRecords));
        refreshButton.fire();

        VBox box = new VBox(12, toolbar(refreshButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Records", box);
    }

    private Tab adminStudentSearchTab() {
        TextField queryField = new TextField();
        queryField.setPromptText("Student number or name");
        Button searchButton = new Button("Search records");
        TableView<BorrowRecord> table = createRecordTable(true);
        searchButton.setOnAction(event -> loadRecords(table, () -> adminService.searchStudentRecords(queryField.getText())));
        queryField.setOnAction(event -> searchButton.fire());
        searchButton.fire();

        VBox box = new VBox(12, toolbar(queryField, searchButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Student Search", box);
    }

    private Tab adminBookTab() {
        TextField queryField = new TextField();
        queryField.setPromptText("Search title, author, subject, publisher, or ISBN");
        Button searchButton = new Button("Search");
        Button removeButton = new Button("Remove selected");
        TableView<Book> table = createBookTable();
        Runnable refresh = () -> loadBooks(table, queryField.getText());
        searchButton.setOnAction(event -> refresh.run());
        queryField.setOnAction(event -> refresh.run());
        removeButton.setOnAction(event -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("No book selected", "Select a book first.");
                return;
            }
            try {
                adminService.removeBook(selected.getBookId());
                showInfo("Book removed", "The selected book is marked REMOVED.");
                refresh.run();
            } catch (Exception e) {
                showError("Could not remove book", e);
            }
        });

        GridPane addForm = new GridPane();
        addForm.getStyleClass().add("form-grid");
        addForm.setHgap(10);
        addForm.setVgap(10);
        TextField title = new TextField();
        TextField authors = new TextField();
        TextField subjects = new TextField();
        TextField publisher = new TextField();
        TextField year = new TextField();
        TextField edition = new TextField();
        TextField format = new TextField();
        TextField source = new TextField();
        TextArea note = new TextArea();
        note.setPrefRowCount(2);
        TextField isbns = new TextField();
        isbns.setPromptText("Comma-separated ISBNs");
        addRow(addForm, 0, "Title", title);
        addRow(addForm, 1, "Authors", authors);
        addRow(addForm, 2, "Subjects", subjects);
        addRow(addForm, 3, "Publisher", publisher);
        addRow(addForm, 4, "Publish year", year);
        addRow(addForm, 5, "Edition", edition);
        addRow(addForm, 6, "Format", format);
        addRow(addForm, 7, "Source", source);
        addRow(addForm, 8, "Note", note);
        addRow(addForm, 9, "ISBNs", isbns);

        Button addButton = new Button("Add book");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> {
            try {
                Integer parsedYear = ValidationUtil.parseOptionalYear(year.getText());
                List<String> isbnList = Arrays.stream(isbns.getText().split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList();
                int newBookId = adminService.addBook(
                        title.getText(), authors.getText(), subjects.getText(), publisher.getText(),
                        parsedYear, edition.getText(), format.getText(), source.getText(), note.getText(), isbnList
                );
                showInfo("Book added", "New book ID: " + newBookId);
                title.clear();
                authors.clear();
                subjects.clear();
                publisher.clear();
                year.clear();
                edition.clear();
                format.clear();
                source.clear();
                note.clear();
                isbns.clear();
                refresh.run();
            } catch (Exception e) {
                showError("Could not add book", e);
            }
        });

        VBox left = new VBox(12, toolbar(queryField, searchButton, removeButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox right = new VBox(12, new Label("Add Book"), addForm, addButton);
        right.getStyleClass().add("side-panel");
        HBox content = new HBox(16, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        refresh.run();

        VBox box = new VBox(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Books", box);
    }

    private Tab adminUsersTab() {
        TextField queryField = new TextField();
        queryField.setPromptText("Student number or name");
        Button searchButton = new Button("Search");
        Button suspendButton = new Button("Suspend");
        Button reactivateButton = new Button("Reactivate");
        TableView<User> table = createUserTable();
        Runnable refresh = () -> loadUsers(table, queryField.getText());
        searchButton.setOnAction(event -> refresh.run());
        queryField.setOnAction(event -> refresh.run());
        suspendButton.setOnAction(event -> updateSelectedUserStatus(table, true, refresh));
        reactivateButton.setOnAction(event -> updateSelectedUserStatus(table, false, refresh));
        refresh.run();

        VBox box = new VBox(12, toolbar(queryField, searchButton, suspendButton, reactivateButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Users", box);
    }

    private Tab adminStatisticsTab() {
        VBox statsBox = new VBox(8);
        statsBox.getStyleClass().add("stats-box");
        Button refreshButton = new Button("Refresh statistics");
        refreshButton.setOnAction(event -> {
            try {
                statsBox.getChildren().clear();
                for (Map.Entry<String, Integer> entry : adminService.statistics().entrySet()) {
                    Label label = new Label(entry.getKey() + ": " + entry.getValue());
                    label.getStyleClass().add("stat-line");
                    statsBox.getChildren().add(label);
                }
            } catch (Exception e) {
                showError("Could not load statistics", e);
            }
        });
        refreshButton.fire();

        VBox box = new VBox(16, toolbar(refreshButton), statsBox);
        box.setPadding(new Insets(16));
        return new Tab("Statistics", box);
    }

    private BorderPane dashboardRoot(String screenTitle) {
        Label title = new Label(screenTitle);
        title.getStyleClass().add("screen-title");
        Label userLabel = new Label(currentUser.getName() + " (" + currentUser.getStudentNo() + ", "
                + currentUser.getRoleLevel() + ", " + currentUser.getStatus() + ")");
        userLabel.getStyleClass().add(currentUser.isSuspended() ? "warning-label" : "muted-label");
        Button logout = new Button("Logout");
        logout.setOnAction(event -> {
            currentUser = null;
            showAuthScreen();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(14, title, spacer, userLabel, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(14, 18, 14, 18));
        top.getStyleClass().add("top-bar");
        BorderPane root = new BorderPane();
        root.setTop(top);
        return root;
    }

    private TableView<Book> createBookTable() {
        TableView<Book> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("ID", book -> String.valueOf(book.getBookId()), 60),
                column("Title", Book::getTitle, 260),
                column("Authors", Book::getAuthors, 180),
                column("Subjects", Book::getSubjects, 180),
                column("Publisher", Book::getPublisher, 160),
                column("Year", book -> book.getPublishYear() == null ? "" : String.valueOf(book.getPublishYear()), 80),
                column("Status", book -> book.getStatus().name(), 100),
                column("ISBNs", Book::getIsbnText, 180)
        );
        return table;
    }

    private TableView<BorrowRecord> createRecordTable(boolean includeStudent) {
        TableView<BorrowRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(column("Record", record -> String.valueOf(record.getRecordId()), 70));
        if (includeStudent) {
            table.getColumns().addAll(
                    column("Student No", BorrowRecord::getStudentNo, 110),
                    column("Name", BorrowRecord::getUserName, 110)
            );
        }
        table.getColumns().addAll(
                column("Book ID", record -> String.valueOf(record.getBookId()), 70),
                column("Book", BorrowRecord::getBookTitle, 240),
                column("Borrowed", record -> DateUtil.format(record.getBorrowDate()), 130),
                column("Due", record -> DateUtil.format(record.getDueDate()), 130),
                column("Returned", record -> DateUtil.format(record.getReturnDate()), 130),
                column("Days", record -> String.valueOf(record.getBorrowDays()), 60),
                column("Status", BorrowRecord::getReturnStatus, 90)
        );
        return table;
    }

    private TableView<User> createUserTable() {
        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("ID", user -> String.valueOf(user.getUserId()), 70),
                column("Student No", User::getStudentNo, 130),
                column("Name", User::getName, 130),
                column("Role", user -> user.getRoleLevel().name(), 100),
                column("Status", user -> user.getStatus().name(), 110),
                column("Created", user -> DateUtil.format(user.getCreatedAt()), 140)
        );
        return table;
    }

    private <T> TableColumn<T, String> column(String title, Function<T, String> mapper, double prefWidth) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(nullToBlank(mapper.apply(data.getValue()))));
        column.setPrefWidth(prefWidth);
        return column;
    }

    private void loadBooks(TableView<Book> table, String query) {
        try {
            table.setItems(FXCollections.observableArrayList(bookService.search(query)));
        } catch (Exception e) {
            showError("Could not load books", e);
        }
    }

    private void loadUsers(TableView<User> table, String query) {
        try {
            table.setItems(FXCollections.observableArrayList(adminService.searchUsers(query)));
        } catch (Exception e) {
            showError("Could not load users", e);
        }
    }

    private void loadRecords(TableView<BorrowRecord> table, RecordLoader loader) {
        try {
            table.setItems(FXCollections.observableArrayList(loader.load()));
        } catch (Exception e) {
            showError("Could not load records", e);
        }
    }

    private void updateSelectedUserStatus(TableView<User> table, boolean suspend, Runnable refresh) {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("No user selected", "Select a user first.");
            return;
        }
        try {
            if (suspend) {
                adminService.suspendUser(selected.getUserId());
            } else {
                adminService.reactivateUser(selected.getUserId());
            }
            refresh.run();
        } catch (Exception e) {
            showError("Could not update user", e);
        }
    }

    private VBox formBox(javafx.scene.Node... nodes) {
        VBox box = new VBox(14, nodes);
        box.setPadding(new Insets(28));
        box.setMaxWidth(520);
        return box;
    }

    private VBox labeled(String label, javafx.scene.Node control) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        VBox box = new VBox(6, labelNode, control);
        return box;
    }

    private HBox toolbar(javafx.scene.Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        for (javafx.scene.Node node : nodes) {
            if (node instanceof TextField textField) {
                HBox.setHgrow(textField, Priority.ALWAYS);
            }
        }
        return box;
    }

    private void addRow(GridPane gridPane, int row, String label, javafx.scene.Node node) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        gridPane.add(labelNode, 0, row);
        gridPane.add(node, 1, row);
        GridPane.setHgrow(node, Priority.ALWAYS);
    }

    private void showScene(javafx.scene.Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(readableError(throwable));
        alert.showAndWait();
    }

    private String readableError(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current instanceof SQLException) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface RecordLoader {
        List<BorrowRecord> load() throws SQLException;
    }
}
