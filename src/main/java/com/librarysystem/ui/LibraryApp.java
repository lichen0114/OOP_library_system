package com.librarysystem.ui;

import com.librarysystem.model.Book;
import com.librarysystem.model.BorrowRecord;
import com.librarysystem.model.LevelRequest;
import com.librarysystem.model.LibrarySettings;
import com.librarysystem.model.Reservation;
import com.librarysystem.model.Review;
import com.librarysystem.model.SubjectPopularity;
import com.librarysystem.model.User;
import com.librarysystem.model.UserLevel;
import com.librarysystem.service.AdminService;
import com.librarysystem.service.AuthService;
import com.librarysystem.service.BookService;
import com.librarysystem.service.BorrowService;
import com.librarysystem.service.FavoriteService;
import com.librarysystem.service.LevelService;
import com.librarysystem.service.ReservationService;
import com.librarysystem.service.ReviewService;
import com.librarysystem.service.SettingsService;
import com.librarysystem.util.DateUtil;
import com.librarysystem.util.ValidationUtil;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class LibraryApp extends Application {
    private final AuthService authService = new AuthService();
    private final BookService bookService = new BookService();
    private final BorrowService borrowService = new BorrowService();
    private final AdminService adminService = new AdminService();
    private final FavoriteService favoriteService = new FavoriteService();
    private final ReservationService reservationService = new ReservationService();
    private final ReviewService reviewService = new ReviewService();
    private final LevelService levelService = new LevelService();
    private final SettingsService settingsService = new SettingsService();

    private Stage primaryStage;
    private User currentUser;
    private StackPane contentPane;

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
        Label subtitle = new Label("Sign in to manage borrowing, reservations, and catalog operations.");
        subtitle.getStyleClass().add("auth-subtitle");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(studentLoginTab(), adminLoginTab(), registerTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("auth-tabs");

        VBox intro = new VBox(6, title, subtitle);
        intro.setAlignment(Pos.CENTER);

        VBox authCard = new VBox(tabPane);
        authCard.getStyleClass().add("auth-card");
        authCard.setMaxWidth(620);

        VBox root = new VBox(24, intro, authCard);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(54, 32, 32, 32));
        root.getStyleClass().add("auth-root");
        showScene(root, 920, 650);
    }

    private Tab studentLoginTab() {
        TextField studentNoField = new TextField();
        studentNoField.setPromptText("S001, S002, or student number");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Label message = new Label();
        message.getStyleClass().add("muted-label");
        Button loginButton = new Button("Student Login");
        loginButton.getStyleClass().add("primary-button");

        loginButton.setOnAction(event -> {
            try {
                currentUser = authService.loginStudent(studentNoField.getText(), passwordField.getText());
                if (currentUser.isSuspended()) {
                    showInfo("Account suspended", "Login is allowed, but borrowing, reservations, and favorites are disabled.");
                }
                showStudentDashboard();
            } catch (Exception e) {
                message.setText(readableError(e));
            }
        });
        passwordField.setOnAction(event -> loginButton.fire());

        VBox box = formBox(
                new Label("Student sign in"),
                labeled("Student number", studentNoField),
                labeled("Password", passwordField),
                loginButton,
                message,
                new Label("Demo students: S001 / password123, S002 / password123")
        );
        return new Tab("Student Login", box);
    }

    private Tab adminLoginTab() {
        TextField accountField = new TextField();
        accountField.setPromptText("admin");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Label message = new Label();
        message.getStyleClass().add("muted-label");
        Button loginButton = new Button("Admin Login");
        loginButton.getStyleClass().add("primary-button");

        loginButton.setOnAction(event -> {
            try {
                currentUser = authService.loginAdmin(accountField.getText(), passwordField.getText());
                showAdminDashboard();
            } catch (Exception e) {
                message.setText(readableError(e));
            }
        });
        passwordField.setOnAction(event -> loginButton.fire());

        VBox box = formBox(
                new Label("Administrator sign in"),
                labeled("Account", accountField),
                labeled("Password", passwordField),
                loginButton,
                message,
                new Label("Demo admin: admin / admin123")
        );
        return new Tab("Admin Login", box);
    }

    private Tab registerTab() {
        TextField studentNoField = new TextField();
        studentNoField.setPromptText("Student number");
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        ComboBox<UserLevel> levelBox = new ComboBox<>();
        Label message = new Label();
        message.getStyleClass().add("muted-label");
        loadRegistrationLevels(levelBox, message);

        Button registerButton = new Button("Register");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setOnAction(event -> {
            try {
                UserLevel selectedLevel = levelBox.getSelectionModel().getSelectedItem();
                if (selectedLevel == null) {
                    throw new IllegalArgumentException("Select a registration level.");
                }
                currentUser = authService.register(
                        studentNoField.getText(),
                        nameField.getText(),
                        passwordField.getText(),
                        selectedLevel.getCode()
                );
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
                labeled("Initial level", levelBox),
                registerButton,
                message
        );
        return new Tab("Register", box);
    }

    private void showStudentDashboard() {
        showDashboard(
                "Student Workspace",
                "Borrowing desk",
                List.of(
                        new NavItem("Home", "Current account status and borrowing snapshot.", this::studentHomePage),
                        new NavItem("Discover", "Search the catalog, borrow books, reserve unavailable titles, and inspect reviews.",
                                () -> studentSearchTab().getContent()),
                        new NavItem("My Loans", "Return active loans and review due-date reminders.",
                                this::studentLoansPage),
                        new NavItem("Reservations", "Track and cancel your reservation queue.",
                                () -> studentReservationTab().getContent()),
                        new NavItem("Favorites", "Manage saved titles and borrow from your favorites list.",
                                () -> studentFavoritesTab().getContent()),
                        new NavItem("Reviews", "Review completed loans and inspect a book's borrowing history.",
                                this::studentReviewsPage),
                        new NavItem("Level Request", "Request a borrowing-level change and track review status.",
                                () -> studentLevelRequestTab().getContent())
                ),
                1280,
                820
        );
    }

    private Node studentHomePage() {
        HBox metrics = metricRow(
                metricCard("Level", currentUser.getLevelDisplayName(), currentUser.getUserLevel().getSummary(), "info"),
                metricCard("Borrow days", borrowDaysSummary(), "Allowed by your current level.", "primary"),
                metricCard("Loan limit", String.valueOf(currentUser.getUserLevel().getMaxActiveLoans()), "Maximum active loans.", "success"),
                metricCard("Account", currentUser.getStatus().name(), "Current account standing.", currentUser.isSuspended() ? "warning" : "success")
        );

        TableView<BorrowRecord> activeLoans = createRecordTable(false);
        loadRecords(activeLoans, () -> borrowService.listActiveLoans(currentUser));

        TableView<Reservation> reservations = createReservationTable(false);
        loadReservations(reservations, () -> reservationService.listReservations(currentUser));

        VBox loansSection = section("Active Loans", activeLoans);
        VBox reservationsSection = section("Reservations", reservations);
        VBox.setVgrow(activeLoans, Priority.ALWAYS);
        VBox.setVgrow(reservations, Priority.ALWAYS);

        VBox page = routeContent(metrics, loansSection, reservationsSection);
        VBox.setVgrow(loansSection, Priority.ALWAYS);
        VBox.setVgrow(reservationsSection, Priority.ALWAYS);
        return page;
    }

    private Node studentLoansPage() {
        TableView<BorrowRecord> activeTable = createRecordTable(false);
        Button refreshButton = secondaryButton("Refresh active loans");
        Button returnButton = primaryButton("Return selected");
        Runnable refresh = () -> loadRecords(activeTable, () -> borrowService.listActiveLoans(currentUser));
        refreshButton.setOnAction(event -> refresh.run());
        returnButton.setOnAction(event -> {
            BorrowRecord selected = requireSelected(activeTable, "No loan selected", "Select an active loan first.");
            if (selected == null) {
                return;
            }
            try {
                borrowService.returnBook(currentUser, selected.getBookId());
                showInfo("Returned", "The selected book was returned.");
                refresh.run();
            } catch (Exception e) {
                showError("Could not return book", e);
            }
        });
        refresh.run();

        TableView<BorrowRecord> reminderTable = createRecordTable(false);
        Label hint = new Label();
        hint.getStyleClass().add("muted-label");
        Button reminderRefresh = secondaryButton("Refresh reminders");
        reminderRefresh.setOnAction(event -> {
            try {
                LibrarySettings settings = settingsService.getSettings();
                hint.setText("Showing active loans due within " + settings.getReminderDays()
                        + " day(s). Fine rate: " + settings.getFinePerOverdueDay() + " per overdue day.");
                reminderTable.setItems(FXCollections.observableArrayList(borrowService.listReminders(currentUser)));
            } catch (Exception e) {
                showError("Could not load reminders", e);
            }
        });
        reminderRefresh.fire();

        VBox activeSection = section("Active Loans", toolbar(refreshButton, returnButton), activeTable);
        VBox reminderSection = section("Reminders & Fines", toolbar(reminderRefresh), hint, reminderTable);
        VBox.setVgrow(activeTable, Priority.ALWAYS);
        VBox.setVgrow(reminderTable, Priority.ALWAYS);

        VBox page = routeContent(activeSection, reminderSection);
        VBox.setVgrow(activeSection, Priority.ALWAYS);
        VBox.setVgrow(reminderSection, Priority.ALWAYS);
        return page;
    }

    private Node studentReviewsPage() {
        TableView<BorrowRecord> historyTable = createRecordTable(false);
        ComboBox<Integer> ratingBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.getSelectionModel().select(Integer.valueOf(5));
        TextArea comment = new TextArea();
        comment.setPromptText("Review comment");
        comment.setPrefRowCount(2);
        Button refreshButton = secondaryButton("Refresh history");
        Button reviewButton = primaryButton("Review returned record");
        Runnable refresh = () -> loadRecords(historyTable, () -> borrowService.listHistory(currentUser));
        refreshButton.setOnAction(event -> refresh.run());
        reviewButton.setOnAction(event -> {
            BorrowRecord selected = requireSelected(historyTable, "No record selected", "Select a returned borrow record first.");
            if (selected == null) {
                return;
            }
            try {
                reviewService.createReview(currentUser, selected.getRecordId(), ratingBox.getValue(), comment.getText());
                showInfo("Review saved", "The review was saved.");
                comment.clear();
                refresh.run();
            } catch (Exception e) {
                showError("Could not save review", e);
            }
        });
        refresh.run();

        TextField bookIdField = new TextField();
        bookIdField.setPromptText("Book ID");
        Button searchButton = primaryButton("Show history");
        TableView<BorrowRecord> bookHistoryTable = createRecordTable(true);
        searchButton.setOnAction(event -> {
            try {
                int bookId = Integer.parseInt(bookIdField.getText().trim());
                loadRecords(bookHistoryTable, () -> borrowService.listBookHistory(bookId));
            } catch (NumberFormatException e) {
                showError("Invalid book ID", e);
            }
        });
        bookIdField.setOnAction(event -> searchButton.fire());

        VBox reviewControls = new VBox(8, toolbar(new Label("Rating"), ratingBox, reviewButton), comment);
        VBox historySection = section("Borrowing History & Reviews", toolbar(refreshButton), historyTable, reviewControls);
        VBox bookSection = section("Book Borrowing History", toolbar(bookIdField, searchButton), bookHistoryTable);
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        VBox.setVgrow(bookHistoryTable, Priority.ALWAYS);

        VBox page = routeContent(historySection, bookSection);
        VBox.setVgrow(historySection, Priority.ALWAYS);
        VBox.setVgrow(bookSection, Priority.ALWAYS);
        return page;
    }

    private Tab studentSearchTab() {
        TextField queryField = new TextField();
        queryField.setPromptText("Search title, author, subject, publisher, or ISBN");
        ComboBox<Integer> dayBox = borrowDaysBox();
        Button searchButton = new Button("Search");
        Button borrowButton = new Button("Borrow");
        Button reserveButton = new Button("Reserve");
        Button favoriteButton = new Button("Favorite");
        TableView<Book> table = createBookTable();
        TextArea details = readOnlyArea(6);
        TableView<Review> reviews = createReviewTable(false);

        Runnable refresh = () -> loadBooks(table, queryField.getText());
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldBook, newBook) -> {
            details.setText(formatBookDetails(newBook));
            if (newBook == null) {
                reviews.setItems(FXCollections.observableArrayList());
            } else {
                loadReviews(reviews, () -> reviewService.listBookReviews(newBook.getBookId()));
            }
        });
        searchButton.setOnAction(event -> refresh.run());
        queryField.setOnAction(event -> refresh.run());
        borrowButton.setOnAction(event -> {
            Book selected = requireSelected(table, "No book selected", "Select a book first.");
            if (selected == null) {
                return;
            }
            try {
                borrowService.borrowBook(currentUser, selected.getBookId(), dayBox.getValue());
                showInfo("Borrowed", "The selected book was borrowed.");
                showStudentDashboard();
            } catch (Exception e) {
                showError("Could not borrow book", e);
            }
        });
        reserveButton.setOnAction(event -> {
            Book selected = requireSelected(table, "No book selected", "Select a borrowed book first.");
            if (selected == null) {
                return;
            }
            try {
                reservationService.reserveBook(currentUser, selected.getBookId());
                showInfo("Reserved", "The selected book was reserved.");
                showStudentDashboard();
            } catch (Exception e) {
                showError("Could not reserve book", e);
            }
        });
        favoriteButton.setOnAction(event -> {
            Book selected = requireSelected(table, "No book selected", "Select a book first.");
            if (selected == null) {
                return;
            }
            try {
                favoriteService.addFavorite(currentUser, selected.getBookId());
                showInfo("Added to favorites", "The selected book was added to favorites.");
            } catch (Exception e) {
                showError("Could not add favorite", e);
            }
        });
        refresh.run();

        VBox lower = new VBox(8, new Label("Book Details"), details, new Label("Reviews"), reviews);
        VBox.setVgrow(reviews, Priority.ALWAYS);
        VBox box = new VBox(12, toolbar(queryField, searchButton, new Label("Days"), dayBox, borrowButton, reserveButton, favoriteButton), table, lower);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(lower, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Search & Borrow", box);
    }

    private Tab studentReturnTab() {
        TableView<BorrowRecord> table = createRecordTable(false);
        Button returnButton = new Button("Return selected");
        Runnable refresh = () -> loadRecords(table, () -> borrowService.listActiveLoans(currentUser));
        returnButton.setOnAction(event -> {
            BorrowRecord selected = requireSelected(table, "No loan selected", "Select an active loan first.");
            if (selected == null) {
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

    private Tab studentHistoryReviewTab() {
        TableView<BorrowRecord> table = createRecordTable(false);
        ComboBox<Integer> ratingBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.getSelectionModel().select(Integer.valueOf(5));
        TextArea comment = new TextArea();
        comment.setPromptText("Review comment");
        comment.setPrefRowCount(2);
        Button refreshButton = new Button("Refresh");
        Button reviewButton = new Button("Review returned record");
        Runnable refresh = () -> loadRecords(table, () -> borrowService.listHistory(currentUser));
        refreshButton.setOnAction(event -> refresh.run());
        reviewButton.setOnAction(event -> {
            BorrowRecord selected = requireSelected(table, "No record selected", "Select a returned borrow record first.");
            if (selected == null) {
                return;
            }
            try {
                reviewService.createReview(currentUser, selected.getRecordId(), ratingBox.getValue(), comment.getText());
                showInfo("Review saved", "The review was saved.");
                comment.clear();
                refresh.run();
            } catch (Exception e) {
                showError("Could not save review", e);
            }
        });
        refresh.run();

        VBox reviewBox = new VBox(8, toolbar(new Label("Rating"), ratingBox, reviewButton), comment);
        VBox box = new VBox(12, toolbar(refreshButton), table, reviewBox);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("My History & Reviews", box);
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

    private Tab studentReservationTab() {
        TableView<Reservation> table = createReservationTable(false);
        Button refreshButton = new Button("Refresh");
        Button cancelButton = new Button("Cancel selected");
        Runnable refresh = () -> loadReservations(table, () -> reservationService.listReservations(currentUser));
        refreshButton.setOnAction(event -> refresh.run());
        cancelButton.setOnAction(event -> {
            Reservation selected = requireSelected(table, "No reservation selected", "Select a reservation first.");
            if (selected == null) {
                return;
            }
            try {
                reservationService.cancelReservation(currentUser, selected.getReservationId());
                refresh.run();
            } catch (Exception e) {
                showError("Could not cancel reservation", e);
            }
        });
        refresh.run();

        VBox box = new VBox(12, toolbar(refreshButton, cancelButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Reservations", box);
    }

    private Tab studentFavoritesTab() {
        if (!currentUser.getUserLevel().canFavorite()) {
            Label label = new Label("Favorites are not enabled for " + currentUser.getLevelDisplayName() + ".");
            label.getStyleClass().add("muted-label");
            VBox box = new VBox(label);
            box.setPadding(new Insets(16));
            return new Tab("Favorites", box);
        }

        TableView<Book> table = createBookTable();
        ComboBox<Integer> dayBox = borrowDaysBox();
        Button refreshButton = new Button("Refresh");
        Button borrowButton = new Button("Borrow selected");
        Button removeButton = new Button("Remove selected");
        Runnable refresh = () -> loadBooks(table, () -> favoriteService.listFavorites(currentUser));
        refreshButton.setOnAction(event -> refresh.run());
        borrowButton.setOnAction(event -> {
            Book selected = requireSelected(table, "No favorite selected", "Select a favorite book first.");
            if (selected == null) {
                return;
            }
            try {
                borrowService.borrowBook(currentUser, selected.getBookId(), dayBox.getValue());
                showInfo("Borrowed", "The selected favorite was borrowed.");
                showStudentDashboard();
            } catch (Exception e) {
                showError("Could not borrow favorite", e);
            }
        });
        removeButton.setOnAction(event -> {
            Book selected = requireSelected(table, "No favorite selected", "Select a favorite book first.");
            if (selected == null) {
                return;
            }
            try {
                favoriteService.removeFavorite(currentUser, selected.getBookId());
                refresh.run();
            } catch (Exception e) {
                showError("Could not remove favorite", e);
            }
        });
        refresh.run();

        VBox box = new VBox(12, toolbar(refreshButton, new Label("Days"), dayBox, borrowButton, removeButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Favorites", box);
    }

    private Tab studentReminderTab() {
        TableView<BorrowRecord> table = createRecordTable(false);
        Label hint = new Label();
        hint.getStyleClass().add("muted-label");
        Button refreshButton = new Button("Refresh reminders");
        refreshButton.setOnAction(event -> {
            try {
                LibrarySettings settings = settingsService.getSettings();
                hint.setText("Showing active loans due within " + settings.getReminderDays()
                        + " day(s). Fine rate: " + settings.getFinePerOverdueDay() + " per overdue day.");
                table.setItems(FXCollections.observableArrayList(borrowService.listReminders(currentUser)));
            } catch (Exception e) {
                showError("Could not load reminders", e);
            }
        });
        refreshButton.fire();

        VBox box = new VBox(12, toolbar(refreshButton), hint, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Reminders & Fines", box);
    }

    private Tab studentLevelRequestTab() {
        ComboBox<UserLevel> levelBox = new ComboBox<>();
        TextArea reason = new TextArea();
        reason.setPrefRowCount(3);
        reason.setPromptText("Reason");
        TableView<LevelRequest> table = createLevelRequestTable(false);
        Button refreshButton = new Button("Refresh");
        Button requestButton = new Button("Submit request");
        Runnable refresh = () -> {
            loadLevels(levelBox, true);
            loadLevelRequests(table, () -> levelService.listRequests(currentUser));
        };
        refreshButton.setOnAction(event -> refresh.run());
        requestButton.setOnAction(event -> {
            UserLevel selected = levelBox.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("No level selected", "Select a level first.");
                return;
            }
            try {
                levelService.requestLevel(currentUser, selected.getCode(), reason.getText());
                reason.clear();
                refresh.run();
            } catch (Exception e) {
                showError("Could not submit request", e);
            }
        });
        refresh.run();

        Label current = new Label("Current level: " + currentUser.getUserLevel().getSummary());
        current.getStyleClass().add("muted-label");
        VBox form = new VBox(8, current, toolbar(new Label("Requested level"), levelBox, requestButton, refreshButton), reason);
        VBox box = new VBox(12, form, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Level Request", box);
    }

    private void showAdminDashboard() {
        showDashboard(
                "Admin Console",
                "Operations",
                List.of(
                        new NavItem("Overview", "Library totals, recent circulation, and request load.",
                                this::adminOverviewPage),
                        new NavItem("Circulation", "Search and audit borrowing records by student.",
                                this::adminCirculationPage),
                        new NavItem("Catalog", "Search, add, and remove catalog entries.",
                                () -> adminBookTab().getContent()),
                        new NavItem("Students", "Search students, manage suspension, and assign levels.",
                                () -> adminUsersTab().getContent()),
                        new NavItem("Requests", "Review level requests and monitor reservations.",
                                this::adminRequestsPage),
                        new NavItem("Content Review", "Inspect student book reviews.",
                                () -> adminReviewsTab().getContent()),
                        new NavItem("Settings", "Tune levels, limits, reminders, and fine settings.",
                                () -> adminLevelsSettingsTab().getContent()),
                        new NavItem("Reports", "Subject popularity and operational statistics.",
                                this::adminReportsPage)
                ),
                1360,
                880
        );
    }

    private Node adminOverviewPage() {
        Map<String, Integer> stats;
        try {
            stats = adminService.statistics();
        } catch (Exception e) {
            showError("Could not load statistics", e);
            stats = Map.of();
        }

        HBox metrics = metricRow(
                metricCard("Books", String.valueOf(stats.getOrDefault("Books", 0)), "Total catalog entries.", "primary"),
                metricCard("Active loans", String.valueOf(stats.getOrDefault("Active loans", 0)), "Currently checked out.", "info"),
                metricCard("Pending reservations", String.valueOf(stats.getOrDefault("Pending reservations", 0)), "Waiting for availability.", "warning"),
                metricCard("Pending requests", String.valueOf(stats.getOrDefault("Pending level requests", 0)), "Level changes awaiting review.", "warning")
        );

        TableView<BorrowRecord> records = createRecordTable(true);
        loadRecords(records, adminService::listAllRecords);
        TableView<LevelRequest> requests = createLevelRequestTable(true);
        loadLevelRequests(requests, adminService::listLevelRequests);

        VBox recordsSection = section("Recent Circulation", records);
        VBox requestsSection = section("Level Request Queue", requests);
        VBox.setVgrow(records, Priority.ALWAYS);
        VBox.setVgrow(requests, Priority.ALWAYS);

        VBox page = routeContent(metrics, recordsSection, requestsSection);
        VBox.setVgrow(recordsSection, Priority.ALWAYS);
        VBox.setVgrow(requestsSection, Priority.ALWAYS);
        return page;
    }

    private Node adminCirculationPage() {
        TextField queryField = new TextField();
        queryField.setPromptText("Student number or name");
        Button searchButton = primaryButton("Search records");
        Button allButton = secondaryButton("All records");
        TableView<BorrowRecord> table = createRecordTable(true);
        Runnable loadAll = () -> loadRecords(table, adminService::listAllRecords);
        searchButton.setOnAction(event -> loadRecords(table, () -> adminService.searchStudentRecords(queryField.getText())));
        allButton.setOnAction(event -> loadAll.run());
        queryField.setOnAction(event -> searchButton.fire());
        loadAll.run();

        VBox recordsSection = section("Borrowing Records", toolbar(queryField, searchButton, allButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox page = routeContent(recordsSection);
        VBox.setVgrow(recordsSection, Priority.ALWAYS);
        return page;
    }

    private Node adminRequestsPage() {
        TableView<LevelRequest> requestTable = createLevelRequestTable(true);
        TextField note = new TextField();
        note.setPromptText("Admin note");
        Button refreshRequests = secondaryButton("Refresh requests");
        Button approveButton = primaryButton("Approve");
        Button rejectButton = dangerButton("Reject");
        Runnable refresh = () -> loadLevelRequests(requestTable, adminService::listLevelRequests);
        refreshRequests.setOnAction(event -> refresh.run());
        approveButton.setOnAction(event -> reviewSelectedLevelRequest(requestTable, true, note, refresh));
        rejectButton.setOnAction(event -> reviewSelectedLevelRequest(requestTable, false, note, refresh));
        refresh.run();

        TableView<Reservation> reservationTable = createReservationTable(true);
        Button refreshReservations = secondaryButton("Refresh reservations");
        refreshReservations.setOnAction(event -> loadReservations(reservationTable, adminService::listReservations));
        refreshReservations.fire();

        VBox requestsSection = section("Level Requests", toolbar(refreshRequests, approveButton, rejectButton, note), requestTable);
        VBox reservationsSection = section("Reservations", toolbar(refreshReservations), reservationTable);
        VBox.setVgrow(requestTable, Priority.ALWAYS);
        VBox.setVgrow(reservationTable, Priority.ALWAYS);

        VBox page = routeContent(requestsSection, reservationsSection);
        VBox.setVgrow(requestsSection, Priority.ALWAYS);
        VBox.setVgrow(reservationsSection, Priority.ALWAYS);
        return page;
    }

    private Node adminReportsPage() {
        TableView<SubjectPopularity> table = createSubjectPopularityTable();
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.getStyleClass().add("report-chart");
        Button refreshPopularity = secondaryButton("Refresh popularity");
        refreshPopularity.setOnAction(event -> {
            try {
                List<SubjectPopularity> data = adminService.subjectPopularity();
                table.setItems(FXCollections.observableArrayList(data));
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                data.stream().limit(10).forEach(item -> series.getData().add(new XYChart.Data<>(item.getSubject(), item.getBorrowCount())));
                chart.setData(FXCollections.observableArrayList(series));
            } catch (Exception e) {
                showError("Could not load subject popularity", e);
            }
        });
        refreshPopularity.fire();

        VBox statsBox = new VBox(8);
        statsBox.getStyleClass().add("stats-box");
        Button refreshStats = secondaryButton("Refresh statistics");
        refreshStats.setOnAction(event -> {
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
        refreshStats.fire();

        VBox popularitySection = section("Subject Popularity", toolbar(refreshPopularity), chart, table);
        VBox statsSection = section("Statistics", toolbar(refreshStats), statsBox);
        VBox.setVgrow(chart, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox page = routeContent(popularitySection, statsSection);
        VBox.setVgrow(popularitySection, Priority.ALWAYS);
        return page;
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
            Book selected = requireSelected(table, "No book selected", "Select a book first.");
            if (selected == null) {
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
                clear(title, authors, subjects, publisher, year, edition, format, source, isbns);
                note.clear();
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
        ComboBox<UserLevel> levelBox = new ComboBox<>();
        Button searchButton = new Button("Search");
        Button suspendButton = new Button("Suspend");
        Button reactivateButton = new Button("Reactivate");
        Button assignButton = new Button("Assign level");
        TableView<User> table = createUserTable();
        Runnable refresh = () -> {
            loadUsers(table, queryField.getText());
            loadLevels(levelBox, true);
        };
        searchButton.setOnAction(event -> refresh.run());
        queryField.setOnAction(event -> refresh.run());
        suspendButton.setOnAction(event -> updateSelectedUserStatus(table, true, refresh));
        reactivateButton.setOnAction(event -> updateSelectedUserStatus(table, false, refresh));
        assignButton.setOnAction(event -> {
            User selected = requireSelected(table, "No user selected", "Select a user first.");
            UserLevel selectedLevel = levelBox.getSelectionModel().getSelectedItem();
            if (selected == null || selectedLevel == null) {
                return;
            }
            try {
                adminService.assignUserLevel(selected.getUserId(), selectedLevel.getCode());
                refresh.run();
            } catch (Exception e) {
                showError("Could not assign level", e);
            }
        });
        refresh.run();

        VBox box = new VBox(12, toolbar(queryField, searchButton, suspendButton, reactivateButton,
                new Label("Level"), levelBox, assignButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Users", box);
    }

    private Tab adminLevelRequestsTab() {
        TableView<LevelRequest> table = createLevelRequestTable(true);
        TextField note = new TextField();
        note.setPromptText("Admin note");
        Button refreshButton = new Button("Refresh");
        Button approveButton = new Button("Approve");
        Button rejectButton = new Button("Reject");
        Runnable refresh = () -> loadLevelRequests(table, adminService::listLevelRequests);
        refreshButton.setOnAction(event -> refresh.run());
        approveButton.setOnAction(event -> reviewSelectedLevelRequest(table, true, note, refresh));
        rejectButton.setOnAction(event -> reviewSelectedLevelRequest(table, false, note, refresh));
        refresh.run();

        VBox box = new VBox(12, toolbar(refreshButton, approveButton, rejectButton, note), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Level Requests", box);
    }

    private Tab adminReservationsTab() {
        TableView<Reservation> table = createReservationTable(true);
        Button refreshButton = new Button("Refresh reservations");
        refreshButton.setOnAction(event -> loadReservations(table, adminService::listReservations));
        refreshButton.fire();

        VBox box = new VBox(12, toolbar(refreshButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Reservations", box);
    }

    private Tab adminReviewsTab() {
        TableView<Review> table = createReviewTable(true);
        Button refreshButton = new Button("Refresh reviews");
        refreshButton.setOnAction(event -> loadReviews(table, adminService::listReviews));
        refreshButton.fire();

        VBox box = new VBox(12, toolbar(refreshButton), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Reviews", box);
    }

    private Tab adminLevelsSettingsTab() {
        TableView<UserLevel> levelTable = createLevelTable();
        Button refreshButton = new Button("Refresh");

        TextField reminderDays = new TextField();
        TextField finePerDay = new TextField();
        Button saveSettings = new Button("Save settings");
        saveSettings.getStyleClass().add("primary-button");
        Runnable loadSettings = () -> {
            try {
                LibrarySettings settings = adminService.getSettings();
                reminderDays.setText(String.valueOf(settings.getReminderDays()));
                finePerDay.setText(String.valueOf(settings.getFinePerOverdueDay()));
            } catch (Exception e) {
                showError("Could not load settings", e);
            }
        };
        saveSettings.setOnAction(event -> {
            try {
                adminService.updateSettings(parseInt(reminderDays, "Reminder days"), parseInt(finePerDay, "Fine per overdue day"));
                showInfo("Settings saved", "Settings were updated.");
            } catch (Exception e) {
                showError("Could not save settings", e);
            }
        });

        TextField code = new TextField();
        TextField displayName = new TextField();
        TextField maxDays = new TextField();
        TextField maxLoans = new TextField();
        CheckBox canFavorite = new CheckBox("Favorites");
        CheckBox registrationAllowed = new CheckBox("Registration");
        Button createButton = new Button("Create custom level");
        Button updateButton = new Button("Update custom level");

        levelTable.getSelectionModel().selectedItemProperty().addListener((obs, oldLevel, level) -> {
            if (level == null) {
                return;
            }
            code.setText(level.getCode());
            displayName.setText(level.getDisplayName());
            maxDays.setText(String.valueOf(level.getMaxBorrowDays()));
            maxLoans.setText(String.valueOf(level.getMaxActiveLoans()));
            canFavorite.setSelected(level.canFavorite());
            registrationAllowed.setSelected(level.isRegistrationAllowed());
        });

        Runnable loadLevels = () -> loadLevelTable(levelTable);
        refreshButton.setOnAction(event -> {
            loadLevels.run();
            loadSettings.run();
        });
        createButton.setOnAction(event -> {
            try {
                adminService.createCustomLevel(code.getText(), displayName.getText(), parseInt(maxDays, "Max days"),
                        parseInt(maxLoans, "Max loans"), canFavorite.isSelected(), registrationAllowed.isSelected());
                loadLevels.run();
            } catch (Exception e) {
                showError("Could not create level", e);
            }
        });
        updateButton.setOnAction(event -> {
            try {
                adminService.updateCustomLevel(code.getText(), displayName.getText(), parseInt(maxDays, "Max days"),
                        parseInt(maxLoans, "Max loans"), canFavorite.isSelected(), registrationAllowed.isSelected());
                loadLevels.run();
            } catch (Exception e) {
                showError("Could not update level", e);
            }
        });
        loadLevels.run();
        loadSettings.run();

        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(10);
        settingsGrid.setVgap(10);
        addRow(settingsGrid, 0, "Reminder days", reminderDays);
        addRow(settingsGrid, 1, "Fine / overdue day", finePerDay);

        GridPane levelGrid = new GridPane();
        levelGrid.setHgap(10);
        levelGrid.setVgap(10);
        addRow(levelGrid, 0, "Code", code);
        addRow(levelGrid, 1, "Display name", displayName);
        addRow(levelGrid, 2, "Max days", maxDays);
        addRow(levelGrid, 3, "Max loans", maxLoans);
        addRow(levelGrid, 4, "Can favorite", canFavorite);
        addRow(levelGrid, 5, "Registration", registrationAllowed);

        VBox form = new VBox(12,
                new Label("Settings"), settingsGrid, saveSettings,
                new Separator(),
                new Label("Custom Level"), levelGrid, toolbar(createButton, updateButton)
        );
        form.getStyleClass().add("side-panel");

        HBox content = new HBox(16, new VBox(12, toolbar(refreshButton), levelTable), form);
        HBox.setHgrow(content.getChildren().get(0), Priority.ALWAYS);
        VBox.setVgrow(levelTable, Priority.ALWAYS);
        VBox box = new VBox(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Levels & Settings", box);
    }

    private Tab adminSubjectPopularityTab() {
        TableView<SubjectPopularity> table = createSubjectPopularityTable();
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        Button refreshButton = new Button("Refresh popularity");
        refreshButton.setOnAction(event -> {
            try {
                List<SubjectPopularity> data = adminService.subjectPopularity();
                table.setItems(FXCollections.observableArrayList(data));
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                data.stream().limit(10).forEach(item -> series.getData().add(new XYChart.Data<>(item.getSubject(), item.getBorrowCount())));
                chart.setData(FXCollections.observableArrayList(series));
            } catch (Exception e) {
                showError("Could not load subject popularity", e);
            }
        });
        refreshButton.fire();

        VBox box = new VBox(12, toolbar(refreshButton), chart, table);
        VBox.setVgrow(chart, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return new Tab("Subject Popularity", box);
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

    private void showDashboard(String screenTitle, String roleLabel, List<NavItem> navItems, double width, double height) {
        Label currentRoute = new Label();
        currentRoute.getStyleClass().add("screen-title");
        Label routeDescription = new Label();
        routeDescription.getStyleClass().add("route-description");

        VBox routeHeader = new VBox(4, currentRoute, routeDescription);
        routeHeader.getStyleClass().add("route-header");

        contentPane = new StackPane();
        contentPane.getStyleClass().add("content-pane");

        VBox main = new VBox(16, routeHeader, contentPane);
        main.getStyleClass().add("main-shell");
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        List<Button> navButtons = new ArrayList<>();
        VBox navList = new VBox(4);
        navList.getStyleClass().add("nav-list");
        for (NavItem item : navItems) {
            Button navButton = new Button(item.label());
            navButton.getStyleClass().add("nav-button");
            navButton.setMaxWidth(Double.MAX_VALUE);
            navButton.setAlignment(Pos.CENTER_LEFT);
            navButton.setOnAction(event -> activateNavItem(item, navButton, navButtons, currentRoute, routeDescription));
            navButtons.add(navButton);
            navList.getChildren().add(navButton);
        }

        Label brand = new Label("Library System");
        brand.getStyleClass().add("sidebar-brand");
        Label role = new Label(roleLabel);
        role.getStyleClass().add("sidebar-role");
        VBox brandBlock = new VBox(4, brand, role);
        brandBlock.getStyleClass().add("sidebar-brand-block");

        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);
        Label level = new Label(currentUser.getLevelDisplayName());
        level.getStyleClass().add("sidebar-level");
        VBox sidebar = new VBox(18, brandBlock, navList, sidebarSpacer, level);
        sidebar.getStyleClass().add("sidebar");

        Label title = new Label(screenTitle);
        title.getStyleClass().add("top-title");
        Label userLabel = new Label(currentUser.getName() + " / " + currentUser.getStudentNo());
        userLabel.getStyleClass().add("user-chip");
        userLabel.getStyleClass().add(currentUser.isSuspended() ? "status-warning" : "status-success");
        Button logout = secondaryButton("Log out");
        logout.getStyleClass().add("logout-button");
        logout.setOnAction(event -> {
            currentUser = null;
            showAuthScreen();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(14, title, spacer, userLabel, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(top);
        root.setLeft(sidebar);
        root.setCenter(main);

        if (!navButtons.isEmpty()) {
            navButtons.get(0).fire();
        }
        showScene(root, width, height);
    }

    private void activateNavItem(NavItem item, Button activeButton, List<Button> buttons,
                                 Label currentRoute, Label routeDescription) {
        for (Button button : buttons) {
            button.getStyleClass().remove("selected");
        }
        if (!activeButton.getStyleClass().contains("selected")) {
            activeButton.getStyleClass().add("selected");
        }
        currentRoute.setText(item.label());
        routeDescription.setText(item.description());
        Node content = item.contentFactory().get();
        contentPane.getChildren().setAll(content);
    }

    private VBox routeContent(Node... nodes) {
        VBox page = new VBox(16, nodes);
        page.getStyleClass().add("route-content");
        page.setFillWidth(true);
        return page;
    }

    private VBox section(String title, Node... nodes) {
        VBox box = new VBox(10);
        box.getStyleClass().add("section-panel");
        box.getChildren().add(sectionTitle(title));
        box.getChildren().addAll(nodes);
        return box;
    }

    private Label sectionTitle(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        return label;
    }

    private HBox metricRow(Node... cards) {
        HBox row = new HBox(12, cards);
        row.getStyleClass().add("metric-row");
        for (Node card : cards) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        return row;
    }

    private VBox metricCard(String label, String value, String detail, String tone) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("metric-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("metric-value");
        Label detailNode = new Label(detail);
        detailNode.getStyleClass().add("metric-detail");
        VBox card = new VBox(6, labelNode, valueNode, detailNode);
        card.getStyleClass().add("metric-card");
        card.getStyleClass().add("metric-" + tone);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private String borrowDaysSummary() {
        List<Integer> days = borrowService.allowedBorrowDays(currentUser);
        return days.isEmpty() ? "None" : days.get(0) + "-" + days.get(days.size() - 1) + " days";
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        return button;
    }

    private Button dangerButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("danger-button");
        return button;
    }

    private TableView<Book> createBookTable() {
        TableView<Book> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("ID", book -> String.valueOf(book.getBookId()), 60),
                column("Title", Book::getTitle, 250),
                column("Authors", Book::getAuthors, 170),
                column("Subjects", Book::getSubjects, 170),
                column("Publisher", Book::getPublisher, 150),
                column("Year", book -> book.getPublishYear() == null ? "" : String.valueOf(book.getPublishYear()), 70),
                column("Edition", Book::getEdition, 100),
                column("Format", Book::getFormatDesc, 150),
                column("Source", Book::getSource, 110),
                statusColumn("Status", book -> book.getStatus().name(), 100),
                column("ISBNs", Book::getIsbnText, 170)
        );
        styleTable(table);
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
                column("Book", BorrowRecord::getBookTitle, 220),
                column("Borrowed", record -> DateUtil.format(record.getBorrowDate()), 130),
                column("Due", record -> DateUtil.format(record.getDueDate()), 130),
                column("Returned", record -> DateUtil.format(record.getReturnDate()), 130),
                column("Days", record -> String.valueOf(record.getBorrowDays()), 60),
                statusColumn("Status", BorrowRecord::getReturnStatus, 100),
                column("Overdue", record -> String.valueOf(record.getOverdueDays()), 80),
                column("Fine", record -> String.valueOf(record.getFineAmount()), 70),
                column("Review", BorrowRecord::getReviewSummary, 180)
        );
        styleTable(table);
        return table;
    }

    private TableView<User> createUserTable() {
        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("ID", user -> String.valueOf(user.getUserId()), 70),
                column("Student No", User::getStudentNo, 120),
                column("Name", User::getName, 120),
                column("Level", User::getLevelDisplayName, 130),
                column("Code", User::getLevelCode, 80),
                column("Max Days", user -> String.valueOf(user.getUserLevel().getMaxBorrowDays()), 80),
                column("Max Loans", user -> String.valueOf(user.getUserLevel().getMaxActiveLoans()), 80),
                column("Favorites", user -> user.getUserLevel().canFavorite() ? "Yes" : "No", 80),
                statusColumn("Status", user -> user.getStatus().name(), 100),
                column("Created", user -> DateUtil.format(user.getCreatedAt()), 130)
        );
        styleTable(table);
        return table;
    }

    private TableView<LevelRequest> createLevelRequestTable(boolean includeStudent) {
        TableView<LevelRequest> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(column("Request", request -> String.valueOf(request.getRequestId()), 75));
        if (includeStudent) {
            table.getColumns().addAll(
                    column("Student No", LevelRequest::getStudentNo, 110),
                    column("Name", LevelRequest::getUserName, 110),
                    column("Current", LevelRequest::getCurrentLevelCode, 90)
            );
        }
        table.getColumns().addAll(
                column("Requested", LevelRequest::getRequestedLevelCode, 100),
                column("Level Name", LevelRequest::getRequestedLevelName, 130),
                column("Reason", LevelRequest::getReason, 220),
                statusColumn("Status", LevelRequest::getStatus, 90),
                column("Note", LevelRequest::getAdminNote, 180),
                column("Requested At", request -> DateUtil.format(request.getRequestedAt()), 130),
                column("Reviewed At", request -> DateUtil.format(request.getReviewedAt()), 130),
                column("Reviewer", LevelRequest::getReviewerName, 100)
        );
        styleTable(table);
        return table;
    }

    private TableView<Reservation> createReservationTable(boolean includeStudent) {
        TableView<Reservation> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(column("Reservation", reservation -> String.valueOf(reservation.getReservationId()), 90));
        if (includeStudent) {
            table.getColumns().addAll(
                    column("Student No", Reservation::getStudentNo, 110),
                    column("Name", Reservation::getUserName, 110)
            );
        }
        table.getColumns().addAll(
                column("Book ID", reservation -> String.valueOf(reservation.getBookId()), 70),
                column("Book", Reservation::getBookTitle, 250),
                statusColumn("Status", Reservation::getStatus, 100),
                column("Requested", reservation -> DateUtil.format(reservation.getRequestedAt()), 130),
                column("Notified", reservation -> DateUtil.format(reservation.getNotifiedAt()), 130)
        );
        styleTable(table);
        return table;
    }

    private TableView<Review> createReviewTable(boolean includeStudent) {
        TableView<Review> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Review", review -> String.valueOf(review.getReviewId()), 70),
                column("Record", review -> String.valueOf(review.getRecordId()), 70)
        );
        if (includeStudent) {
            table.getColumns().addAll(
                    column("Student No", Review::getStudentNo, 110),
                    column("Name", Review::getUserName, 110)
            );
        }
        table.getColumns().addAll(
                column("Book ID", review -> String.valueOf(review.getBookId()), 70),
                column("Book", Review::getBookTitle, 220),
                column("Rating", review -> String.valueOf(review.getRating()), 70),
                column("Comment", Review::getComment, 260),
                column("Created", review -> DateUtil.format(review.getCreatedAt()), 130)
        );
        styleTable(table);
        return table;
    }

    private TableView<UserLevel> createLevelTable() {
        TableView<UserLevel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Code", UserLevel::getCode, 90),
                column("Display", UserLevel::getDisplayName, 160),
                column("Max Days", level -> String.valueOf(level.getMaxBorrowDays()), 80),
                column("Max Loans", level -> String.valueOf(level.getMaxActiveLoans()), 80),
                column("Favorites", level -> level.canFavorite() ? "Yes" : "No", 80),
                column("Admin", level -> level.isAdmin() ? "Yes" : "No", 70),
                column("Registration", level -> level.isRegistrationAllowed() ? "Yes" : "No", 100),
                column("Custom", level -> level.isCustomLevel() ? "Yes" : "No", 80)
        );
        styleTable(table);
        return table;
    }

    private TableView<SubjectPopularity> createSubjectPopularityTable() {
        TableView<SubjectPopularity> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Subject", SubjectPopularity::getSubject, 220),
                column("Borrow Count", popularity -> String.valueOf(popularity.getBorrowCount()), 120)
        );
        styleTable(table);
        return table;
    }

    private <T> TableColumn<T, String> column(String title, Function<T, String> mapper, double prefWidth) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(nullToBlank(mapper.apply(data.getValue()))));
        column.setPrefWidth(prefWidth);
        return column;
    }

    private <T> TableColumn<T, String> statusColumn(String title, Function<T, String> mapper, double prefWidth) {
        TableColumn<T, String> column = column(title, mapper, prefWidth);
        column.setCellFactory(tableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null || status.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(status);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add(statusStyle(status));
                setText(null);
                setGraphic(badge);
            }
        });
        return column;
    }

    private void styleTable(TableView<?> table) {
        table.getStyleClass().add("data-table");
        table.setPlaceholder(new Label("No records to display"));
        table.setFixedCellSize(38);
    }

    private String statusStyle(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("AVAILABLE") || normalized.contains("RETURNED")
                || normalized.contains("ACTIVE") || normalized.contains("APPROVED")
                || normalized.contains("FULFILLED")) {
            return "status-success";
        }
        if (normalized.contains("BORROWED") || normalized.contains("PENDING")
                || normalized.contains("NOTIFIED")) {
            return "status-warning";
        }
        if (normalized.contains("SUSPENDED") || normalized.contains("REMOVED")
                || normalized.contains("OVERDUE") || normalized.contains("REJECTED")
                || normalized.contains("CANCELLED")) {
            return "status-danger";
        }
        return "status-info";
    }

    private void loadBooks(TableView<Book> table, String query) {
        loadBooks(table, () -> bookService.search(query));
    }

    private void loadBooks(TableView<Book> table, BookLoader loader) {
        try {
            table.setItems(FXCollections.observableArrayList(loader.load()));
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

    private void loadReviews(TableView<Review> table, ReviewLoader loader) {
        try {
            table.setItems(FXCollections.observableArrayList(loader.load()));
        } catch (Exception e) {
            showError("Could not load reviews", e);
        }
    }

    private void loadReservations(TableView<Reservation> table, ReservationLoader loader) {
        try {
            table.setItems(FXCollections.observableArrayList(loader.load()));
        } catch (Exception e) {
            showError("Could not load reservations", e);
        }
    }

    private void loadLevelRequests(TableView<LevelRequest> table, LevelRequestLoader loader) {
        try {
            table.setItems(FXCollections.observableArrayList(loader.load()));
        } catch (Exception e) {
            showError("Could not load level requests", e);
        }
    }

    private void loadRegistrationLevels(ComboBox<UserLevel> levelBox, Label message) {
        try {
            levelBox.setItems(FXCollections.observableArrayList(authService.listRegistrationLevels()));
            levelBox.getSelectionModel().selectFirst();
        } catch (Exception e) {
            message.setText(readableError(e));
        }
    }

    private void loadLevels(ComboBox<UserLevel> levelBox, boolean assignableOnly) {
        try {
            List<UserLevel> levels = assignableOnly ? levelService.listAssignableLevels() : levelService.listAllLevels();
            levelBox.setItems(FXCollections.observableArrayList(levels));
            levelBox.getSelectionModel().selectFirst();
        } catch (Exception e) {
            showError("Could not load levels", e);
        }
    }

    private void loadLevelTable(TableView<UserLevel> table) {
        try {
            table.setItems(FXCollections.observableArrayList(adminService.listLevels()));
        } catch (Exception e) {
            showError("Could not load levels", e);
        }
    }

    private void updateSelectedUserStatus(TableView<User> table, boolean suspend, Runnable refresh) {
        User selected = requireSelected(table, "No user selected", "Select a user first.");
        if (selected == null) {
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

    private void reviewSelectedLevelRequest(TableView<LevelRequest> table, boolean approve, TextField note, Runnable refresh) {
        LevelRequest selected = requireSelected(table, "No request selected", "Select a level request first.");
        if (selected == null) {
            return;
        }
        try {
            adminService.reviewLevelRequest(currentUser, selected.getRequestId(), approve, note.getText());
            note.clear();
            refresh.run();
        } catch (Exception e) {
            showError("Could not review request", e);
        }
    }

    private ComboBox<Integer> borrowDaysBox() {
        ComboBox<Integer> box = new ComboBox<>(FXCollections.observableArrayList(borrowService.allowedBorrowDays(currentUser)));
        if (!box.getItems().isEmpty()) {
            box.getSelectionModel().select(box.getItems().size() - 1);
        }
        return box;
    }

    private TextArea readOnlyArea(int rows) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(rows);
        return area;
    }

    private String formatBookDetails(Book book) {
        if (book == null) {
            return "";
        }
        return """
                ID: %d
                Title: %s
                Authors: %s
                Subjects: %s
                Publisher: %s
                Year: %s
                Edition: %s
                Format: %s
                Source: %s
                ISBNs: %s
                Status: %s
                Note: %s
                """.formatted(
                book.getBookId(),
                nullToBlank(book.getTitle()),
                nullToBlank(book.getAuthors()),
                nullToBlank(book.getSubjects()),
                nullToBlank(book.getPublisher()),
                book.getPublishYear() == null ? "" : book.getPublishYear(),
                nullToBlank(book.getEdition()),
                nullToBlank(book.getFormatDesc()),
                nullToBlank(book.getSource()),
                nullToBlank(book.getIsbnText()),
                book.getStatus(),
                nullToBlank(book.getNote())
        );
    }

    private <T> T requireSelected(TableView<T> table, String title, String message) {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo(title, message);
        }
        return selected;
    }

    private VBox formBox(javafx.scene.Node... nodes) {
        VBox box = new VBox(14, nodes);
        box.setPadding(new Insets(28));
        box.setMaxWidth(540);
        return box;
    }

    private VBox labeled(String label, javafx.scene.Node control) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        return new VBox(6, labelNode, control);
    }

    private HBox toolbar(javafx.scene.Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        for (javafx.scene.Node node : nodes) {
            if (node instanceof TextField textField) {
                HBox.setHgrow(textField, Priority.ALWAYS);
            }
            if (node instanceof Button button) {
                applyDefaultButtonStyle(button);
            }
        }
        box.getStyleClass().add("toolbar");
        return box;
    }

    private void applyDefaultButtonStyle(Button button) {
        if (button.getStyleClass().contains("primary-button")
                || button.getStyleClass().contains("secondary-button")
                || button.getStyleClass().contains("danger-button")
                || button.getStyleClass().contains("nav-button")) {
            return;
        }
        String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
        if (text.contains("remove") || text.contains("cancel") || text.contains("suspend") || text.contains("reject")) {
            button.getStyleClass().add("danger-button");
        } else {
            button.getStyleClass().add("secondary-button");
        }
    }

    private void addRow(GridPane gridPane, int row, String label, javafx.scene.Node node) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        gridPane.add(labelNode, 0, row);
        gridPane.add(node, 1, row);
        GridPane.setHgrow(node, Priority.ALWAYS);
    }

    private int parseInt(TextField field, String label) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    private void clear(TextField... fields) {
        for (TextField field : fields) {
            field.clear();
        }
    }

    private void showScene(javafx.scene.Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app.css")).toExternalForm());
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

    private String nullToBlank(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record NavItem(String label, String description, Supplier<Node> contentFactory) {
    }

    @FunctionalInterface
    private interface BookLoader {
        List<Book> load() throws SQLException;
    }

    @FunctionalInterface
    private interface RecordLoader {
        List<BorrowRecord> load() throws SQLException;
    }

    @FunctionalInterface
    private interface ReviewLoader {
        List<Review> load() throws SQLException;
    }

    @FunctionalInterface
    private interface ReservationLoader {
        List<Reservation> load() throws SQLException;
    }

    @FunctionalInterface
    private interface LevelRequestLoader {
        List<LevelRequest> load() throws SQLException;
    }
}
