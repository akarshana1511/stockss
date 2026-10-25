package com.stockexchange;

import com.stockexchange.database.DatabaseConnection;
import com.stockexchange.model.Holding;
import com.stockexchange.model.Stock;
import com.stockexchange.model.User;
import com.stockexchange.model.enums.OrderType;
import com.stockexchange.service.PortfolioService;
import com.stockexchange.service.StockService;
import com.stockexchange.service.TradingService;
import com.stockexchange.service.TransactionService;
import com.stockexchange.service.UserService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final long MARKET_UPDATE_INTERVAL_SECONDS = 5;
    private Connection connection;
    private UserService userService;
    private StockService stockService;
    private PortfolioService portfolioService;
    private TradingService tradingService;
    private TransactionService transactionService;
    private volatile User currentUser;
    private ListView<Stock> stockList;
    private TextArea marketInfo;
    private TextArea portfolioInfo;
    private TextArea ordersInfo;
    private TextArea historyInfo;
    private Label accountInfo;
    private TextField quantityField;
    private TextField priceField;
    private ComboBox<OrderType> orderType;
    private Stage stage;
    private ExecutorService databaseWorker;
    private ScheduledExecutorService marketScheduler;
    private ScheduledFuture<?> marketTicker;
    private final AtomicBoolean marketTickQueued = new AtomicBoolean();
    private Button placeOrderButton;
    private Button refreshButton;
    private Button exportButton;
    private Label orderStatus;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Stock Exchange System");
        databaseWorker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "stock-exchange-database");
            thread.setDaemon(true);
            return thread;
        });
        Label loading = new Label("Connecting to the market...");
        loading.getStyleClass().add("section-title");
        BorderPane loadingRoot = new BorderPane(loading);
        loadingRoot.setPadding(new Insets(40));
        Scene loadingScene = new Scene(loadingRoot, 600, 360);
        applyStyles(loadingScene);
        stage.setScene(loadingScene);
        stage.show();
        runDatabaseTask(DatabaseConnection::getConnection, connected -> {
            connection = connected;
            userService = new UserService(connection);
            stockService = new StockService(connection);
            portfolioService = new PortfolioService(connection);
            tradingService = new TradingService(connection);
            transactionService = new TransactionService(connection);
            showAuthentication();
        }, exception -> {
            showError("Database connection failed",
                    exception.getMessage() + "\n\nCheck MySQL is running, the stock_exchange database "
                            + "schema is installed, and MYSQL_URL / MYSQL_USER / MYSQL_PASSWORD are configured.");
            stage.close();
        });
    }

    private void showAuthentication() {
        Label brand = new Label("NORTHSTAR");
        brand.getStyleClass().add("brand-mark");
        Label headline = new Label("Trade with\nclarity.");
        headline.getStyleClass().add("auth-headline");
        headline.setWrapText(true);
        Label description = new Label("A simulated market for exploring stocks, limit orders, and portfolio management.");
        description.getStyleClass().add("auth-copy");
        VBox introduction = new VBox(20, brand, headline, description);
        introduction.getStyleClass().add("auth-intro");
        introduction.setAlignment(Pos.CENTER_LEFT);
        introduction.setPadding(new Insets(54));
        introduction.setPrefWidth(450);

        TextField name = new TextField();
        name.setPromptText("Full name (for new accounts)");
        TextField email = new TextField();
        email.setPromptText("Email address");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label title = new Label("Welcome");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Sign in or create an account to enter the market.");
        subtitle.getStyleClass().add("muted-text");
        Label status = new Label("New accounts start with $10,000 in simulated funds.");
        status.getStyleClass().add("status-text");
        Button login = new Button("Log in");
        login.getStyleClass().add("primary-button");
        Button register = new Button("Create account");
        register.getStyleClass().add("secondary-button");
        login.setDefaultButton(true);
        for (TextField field : List.of(name, email, password)) {
            field.getStyleClass().add("input-field");
            field.setMaxWidth(Double.MAX_VALUE);
        }

        login.setOnAction(event -> {
            char[] enteredPassword = password.getText().toCharArray();
            login.setDisable(true);
            register.setDisable(true);
            status.setText("Signing in...");
            String enteredEmail = email.getText();
            runDatabaseTask(() -> userService.authenticate(enteredEmail, enteredPassword), user -> {
                java.util.Arrays.fill(enteredPassword, '\0');
                password.clear();
                currentUser = user;
                showTradingScreen();
                startMarketTicker();
            }, exception -> {
                java.util.Arrays.fill(enteredPassword, '\0');
                password.clear();
                status.setText(exception.getMessage());
                login.setDisable(false);
                register.setDisable(false);
            });
        });
        register.setOnAction(event -> {
            char[] enteredPassword = password.getText().toCharArray();
            String enteredEmail = email.getText();
            String enteredName = name.getText();
            login.setDisable(true);
            register.setDisable(true);
            status.setText("Creating your account...");
            runDatabaseTask(() -> {
                userService.register(enteredName, enteredEmail, enteredPassword);
                return userService.authenticate(enteredEmail, enteredPassword);
            }, user -> {
                java.util.Arrays.fill(enteredPassword, '\0');
                password.clear();
                currentUser = user;
                showTradingScreen();
                startMarketTicker();
            }, exception -> {
                java.util.Arrays.fill(enteredPassword, '\0');
                password.clear();
                status.setText(exception.getMessage());
                login.setDisable(false);
                register.setDisable(false);
            });
        });

        HBox actions = new HBox(10, login, register);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox form = new VBox(14, title, subtitle, name, email, password, actions, status);
        form.getStyleClass().add("auth-card");
        form.setMaxWidth(440);
        HBox root = new HBox(introduction, form);
        root.getStyleClass().add("auth-root");
        root.setAlignment(Pos.CENTER);
        HBox.setHgrow(form, Priority.ALWAYS);
        Scene scene = new Scene(root, 1000, 640);
        applyStyles(scene);
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    private void showTradingScreen() {
        stockList = new ListView<>();
        stockList.setPrefHeight(430);
        stockList.getStyleClass().add("stock-list");
        stockList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Stock stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Label symbol = new Label(stock.getSymbol());
                    symbol.getStyleClass().add("stock-symbol");
                    Label name = new Label(stock.getName());
                    name.getStyleClass().add("stock-name");
                    Label price = new Label("$" + stock.getCurrentPrice());
                    price.getStyleClass().add("stock-price");
                    VBox details = new VBox(4, symbol, name);
                    HBox row = new HBox(details, price);
                    row.getStyleClass().add("stock-row");
                    row.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(details, Priority.ALWAYS);
                    setText(null);
                    setGraphic(row);
                    setStyle("-fx-padding: 12 10;");
                }
            }
        });

        marketInfo = readOnlyArea();
        marketInfo.setPrefRowCount(10);
        portfolioInfo = readOnlyArea();
        ordersInfo = readOnlyArea();
        historyInfo = readOnlyArea();
        for (TextArea area : List.of(portfolioInfo, ordersInfo, historyInfo)) {
            area.getStyleClass().add("data-area");
        }
        accountInfo = new Label();
        accountInfo.getStyleClass().add("account-balance");
        quantityField = new TextField();
        quantityField.setPromptText("e.g. 10 shares");
        priceField = new TextField();
        priceField.setPromptText("e.g. 150.00");
        orderType = new ComboBox<>(FXCollections.observableArrayList(OrderType.values()));
        orderType.setValue(OrderType.BUY);
        orderType.setMaxWidth(Double.MAX_VALUE);
        quantityField.getStyleClass().add("input-field");
        priceField.getStyleClass().add("input-field");
        orderType.getStyleClass().add("input-field");
        placeOrderButton = new Button("Place limit order");
        placeOrderButton.getStyleClass().add("primary-button");
        placeOrderButton.setMaxWidth(Double.MAX_VALUE);
        orderStatus = new Label("Order submissions run in the background.");
        orderStatus.getStyleClass().add("status-text");
        refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("toolbar-button");
        exportButton = new Button("Export history CSV");
        exportButton.getStyleClass().add("toolbar-button");
        Button logout = new Button("Log out");
        logout.getStyleClass().add("toolbar-button");
        placeOrderButton.setOnAction(event -> placeOrder());
        refreshButton.setOnAction(event -> refreshViews());
        exportButton.setOnAction(event -> exportHistory());
        logout.setOnAction(event -> {
            stopMarketTicker();
            currentUser = null;
            showAuthentication();
        });
        stockList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldStock, newStock) -> {
                    if (newStock != null && (oldStock == null
                            || oldStock.getStockId() != newStock.getStockId())) {
                        showOrderBook(newStock);
                    }
                });

        VBox orderEntry = new VBox(10, fieldLabel("SIDE", orderType),
                fieldLabel("QUANTITY", quantityField), fieldLabel("LIMIT PRICE", priceField),
                placeOrderButton, orderStatus);
        Node orderCard = card("Place an order", orderEntry);
        orderCard.getStyleClass().add("order-card");
        Label tickerStatus = new Label("Simulated prices update every "
                + MARKET_UPDATE_INTERVAL_SECONDS + " seconds.");
        tickerStatus.getStyleClass().add("muted-text");
        VBox marketWatch = new VBox(8, tickerStatus, stockList);
        VBox marketPane = new VBox(16,
                card("Market watch", marketWatch),
                card("Order book & market stats", marketInfo));
        marketPane.setPrefWidth(480);
        marketPane.setMinWidth(340);
        VBox tradingPane = new VBox(16, orderCard);
        tradingPane.setPrefWidth(330);
        HBox marketContent = new HBox(18, marketPane, tradingPane);
        marketContent.getStyleClass().add("page-content");
        HBox.setHgrow(marketPane, Priority.ALWAYS);

        VBox portfolioContent = new VBox(card("Your holdings", portfolioInfo));
        VBox ordersContent = new VBox(card("Orders awaiting execution", ordersInfo));
        VBox historyContent = new VBox(card("Executed trades", historyInfo));

        TabPane tabs = new TabPane(
                tab("Market", marketContent),
                tab("Portfolio", portfolioContent),
                tab("Open orders", ordersContent),
                tab("Trade history", historyContent)
        );
        tabs.getStyleClass().add("main-tabs");
        Label brand = new Label("NORTHSTAR");
        brand.getStyleClass().add("brand-mark");
        Label welcome = new Label("Market dashboard");
        welcome.getStyleClass().add("topbar-title");
        VBox identity = new VBox(3, brand, welcome);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox accountActions = new HBox(14, accountInfo, refreshButton, exportButton, logout);
        accountActions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(24, identity, spacer, accountActions);
        header.getStyleClass().add("topbar");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 24, 16, 24));
        BorderPane root = new BorderPane(tabs);
        root.getStyleClass().add("app-root");
        root.setTop(header);
        Scene scene = new Scene(root, 1180, 800);
        applyStyles(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
        refreshViews();
    }

    private VBox fieldLabel(String title, Node control) {
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        VBox field = new VBox(7, label, control);
        return field;
    }

    private VBox card(String title, Node content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");
        VBox card = new VBox(14, heading, content);
        card.getStyleClass().add("surface-card");
        VBox.setVgrow(content, Priority.ALWAYS);
        return card;
    }

    private void applyStyles(Scene scene) {
        var stylesheet = getClass().getResource("/com/stockexchange/css/dashboard.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private TextArea readOnlyArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefRowCount(18);
        area.getStyleClass().add("data-area");
        return area;
    }

    private void placeOrder() {
        Stock selectedStock = stockList.getSelectionModel().getSelectedItem();
        if (selectedStock == null) {
            showError("Select a stock", "Choose a stock before placing an order.");
            return;
        }
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            User user = currentUser;
            int userId = user.getUserId();
            int stockId = selectedStock.getStockId();
            OrderType type = orderType.getValue();
            placeOrderButton.setDisable(true);
            orderStatus.setText("Submitting order...");
            runDatabaseTask(() -> tradingService.placeOrder(userId, stockId, type, quantity, price),
                    fills -> {
                        quantityField.clear();
                        placeOrderButton.setDisable(false);
                        orderStatus.setText(fills == 0 ? "Order is now on the book."
                                : fills + " trade(s) executed; any remainder is on the book.");
                        refreshViews();
                    }, exception -> {
                        placeOrderButton.setDisable(false);
                        orderStatus.setText("Order failed.");
                        showError("Order could not be placed", exception.getMessage());
                    });
        } catch (NumberFormatException exception) {
            showError("Invalid order", "Enter a whole-number quantity and a valid positive price.");
        }
    }

    private void refreshViews() {
        if (currentUser == null) {
            return;
        }
        User user = currentUser;
        int userId = user.getUserId();
        int selectedStockId = stockList.getSelectionModel().getSelectedItem() == null
                ? -1 : stockList.getSelectionModel().getSelectedItem().getStockId();
        refreshButton.setDisable(true);
        runDatabaseTask(() -> loadDashboard(user, selectedStockId), dashboard -> {
            if (currentUser == null || currentUser.getUserId() != userId) {
                return;
            }
            refreshButton.setDisable(false);
            Stock selected = stockList.getSelectionModel().getSelectedItem();
            stockList.setItems(FXCollections.observableArrayList(dashboard.stocks()));
            Stock selectedAgain = selected == null ? null : dashboard.stocks().stream()
                    .filter(stock -> stock.getStockId() == selected.getStockId())
                    .findFirst().orElse(null);
            if (selectedAgain != null) {
                stockList.getSelectionModel().select(selectedAgain);
            } else if (!dashboard.stocks().isEmpty()) {
                stockList.getSelectionModel().select(0);
            }
            accountInfo.setText(dashboard.accountSummary());
            portfolioInfo.setText(dashboard.portfolio());
            ordersInfo.setText(dashboard.openOrders());
            historyInfo.setText(dashboard.history());
            Stock nowSelected = stockList.getSelectionModel().getSelectedItem();
            if (nowSelected != null) {
                if (selectedStockId == nowSelected.getStockId()
                        && selectedStockId == dashboard.marketStockId()) {
                    marketInfo.setText(dashboard.marketInfo());
                } else {
                    showOrderBook(nowSelected);
                }
            }
        }, exception -> {
            refreshButton.setDisable(false);
            showError("Could not refresh", exception.getMessage());
        });
    }

    private DashboardData loadDashboard(User user, int selectedStockId) throws SQLException {
        int userId = user.getUserId();
        List<Stock> stocks = stockService.getAllStocks();
        List<Holding> holdings = portfolioService.getUserPortfolio(user);
        List<String> openOrders = tradingService.getOpenOrders(userId);
        List<String> transactions = transactionService.getTransactionSummary(user);

        String accountSummary;
        String sql = "SELECT balance, reserved_balance FROM users WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("The signed-in account could not be loaded.");
                }
                BigDecimal balance = results.getBigDecimal("balance");
                BigDecimal reserved = results.getBigDecimal("reserved_balance");
                accountSummary = String.format("%s  |  Available: $%s  |  Reserved: $%s",
                        user.getName(), balance.subtract(reserved), reserved);
            }
        }

        String market = "";
        if (selectedStockId != -1) {
            market = "MARKET STATISTICS\n" + tradingService.getMarketStatistics(selectedStockId)
                    + "\n\nPRICE-TIME PRIORITY\n"
                    + formatLines(tradingService.getOrderBook(selectedStockId), "No open orders.");
        }
        return new DashboardData(stocks, accountSummary, formatPortfolio(holdings, stocks),
                formatLines(openOrders, "No pending orders."),
                formatLines(transactions, "No trades yet."), selectedStockId, market);
    }

    private void startMarketTicker() {
        stopMarketTicker();
        marketScheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "simulated-market-ticker");
            thread.setDaemon(true);
            return thread;
        });
        marketTicker = marketScheduler.scheduleAtFixedRate(() -> {
            if (currentUser == null || !marketTickQueued.compareAndSet(false, true)) {
                return;
            }
            runDatabaseTask(stockService::simulatePriceTick, stocks -> {
                marketTickQueued.set(false);
                if (currentUser != null && stockList != null) {
                    updateMarketPrices(stocks);
                }
            }, exception -> {
                marketTickQueued.set(false);
                LOGGER.log(Level.WARNING, "Could not update simulated market prices.", exception);
            });
        }, MARKET_UPDATE_INTERVAL_SECONDS, MARKET_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopMarketTicker() {
        if (marketTicker != null) {
            marketTicker.cancel(false);
            marketTicker = null;
        }
        if (marketScheduler != null) {
            marketScheduler.shutdownNow();
            marketScheduler = null;
        }
        marketTickQueued.set(false);
    }

    private void updateMarketPrices(List<Stock> stocks) {
        Stock previousSelection = stockList.getSelectionModel().getSelectedItem();
        stockList.setItems(FXCollections.observableArrayList(stocks));
        if (previousSelection == null) {
            if (!stocks.isEmpty()) {
                stockList.getSelectionModel().select(0);
            }
            return;
        }
        stocks.stream()
                .filter(stock -> stock.getStockId() == previousSelection.getStockId())
                .findFirst()
                .ifPresentOrElse(stock -> stockList.getSelectionModel().select(stock),
                        () -> stockList.getSelectionModel().clearSelection());
    }

    private String formatPortfolio(List<Holding> holdings, List<Stock> stocks) {
        if (holdings.isEmpty()) {
            return "No holdings yet.";
        }
        StringBuilder content = new StringBuilder("SYMBOL   QUANTITY   AVERAGE PRICE\n");
        for (Holding holding : holdings) {
            String symbol = stocks.stream()
                    .filter(stock -> stock.getStockId() == holding.getStockId())
                    .map(Stock::getSymbol).findFirst().orElse("Stock " + holding.getStockId());
            content.append(String.format("%-8s %-10d $%s%n",
                    symbol, holding.getQuantity(), holding.getAveragePrice()));
        }
        return content.toString();
    }

    private String formatLines(List<String> lines, String emptyMessage) {
        return lines.isEmpty() ? emptyMessage : String.join(System.lineSeparator(), lines);
    }

    private void showOrderBook(Stock stock) {
        if (stock == null || marketInfo == null) {
            return;
        }
        int stockId = stock.getStockId();
        marketInfo.setText("Loading market data...");
        runDatabaseTask(() -> "MARKET STATISTICS\n"
                        + tradingService.getMarketStatistics(stockId)
                        + "\n\nPRICE-TIME PRIORITY\n"
                        + formatLines(tradingService.getOrderBook(stockId), "No open orders."),
                info -> {
                    Stock selected = stockList.getSelectionModel().getSelectedItem();
                    if (selected != null && selected.getStockId() == stockId) {
                        marketInfo.setText(info);
                    }
                }, exception -> showError("Could not load order book", exception.getMessage()));
    }

    private void exportHistory() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export trade history");
        chooser.setInitialFileName("trade-history.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        User user = currentUser;
        exportButton.setDisable(true);
        runDatabaseTask(() -> {
            transactionService.exportTransactions(user, file.getAbsolutePath());
            return file.getAbsolutePath();
        }, path -> {
            exportButton.setDisable(false);
            showInformation("Export complete", "Trade history saved to " + path);
        }, exception -> {
            exportButton.setDisable(false);
            showError("Export failed", exception.getMessage());
        });
    }

    private <T> void runDatabaseTask(Callable<T> operation, Consumer<T> onSuccess,
                                    Consumer<Exception> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(asException(task.getException())));
        try {
            databaseWorker.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException exception) {
            onFailure.accept(exception);
        }
    }

    private Exception asException(Throwable throwable) {
        if (throwable instanceof Exception exception) {
            return exception;
        }
        return new RuntimeException(throwable);
    }

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private void showInformation(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "An unexpected error occurred." : message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        stopMarketTicker();
        if (databaseWorker != null) {
            try {
                databaseWorker.execute(() -> {
                    try {
                        if (connection != null && !connection.isClosed()) {
                            connection.close();
                        }
                    } catch (SQLException exception) {
                        LOGGER.log(Level.SEVERE, "Could not close database connection.", exception);
                    }
                });
            } catch (java.util.concurrent.RejectedExecutionException exception) {
                LOGGER.log(Level.WARNING, "Database worker stopped before connection cleanup.", exception);
            }
            databaseWorker.shutdown();
        }
    }

    private record DashboardData(List<Stock> stocks, String accountSummary, String portfolio,
                                 String openOrders, String history, int marketStockId, String marketInfo) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
