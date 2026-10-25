package com.stockexchange.service;

import com.stockexchange.model.enums.OrderType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TradingService {
    private final Connection connection;

    public TradingService(Connection connection) {
        this.connection = connection;
    }

    public int placeOrder(int userId, int stockId, OrderType type, int quantity, BigDecimal price)
            throws SQLException {
        if (quantity <= 0 || price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity and price must be greater than zero.");
        }
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            lockStock(stockId);
            if (type == OrderType.BUY) {
                reserveFunds(userId, price.multiply(BigDecimal.valueOf(quantity)));
            } else {
                reserveShares(userId, stockId, quantity);
            }

            int orderId = insertOrder(userId, stockId, type, quantity, price);
            int executed = matchOrder(orderId, userId, stockId, type, quantity, price);
            connection.commit();
            return executed;
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public List<String> getOrderBook(int stockId) throws SQLException {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT order_type, price, SUM(remaining_quantity) AS quantity "
                + "FROM orders WHERE stock_id = ? AND order_status IN ('PENDING', 'PARTIALLY_FILLED') "
                + "GROUP BY order_type, price ORDER BY order_type, "
                + "CASE WHEN order_type = 'BUY' THEN price END DESC, "
                + "CASE WHEN order_type = 'SELL' THEN price END ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    lines.add(String.format("%-5s %12s  x %d",
                            results.getString("order_type"), results.getBigDecimal("price"),
                            results.getInt("quantity")));
                }
            }
        }
        return lines;
    }

    public String getMarketStatistics(int stockId) throws SQLException {
        String sql = "SELECT s.current_price, COALESCE(SUM(t.quantity), 0) AS volume, "
                + "COALESCE(MIN(t.price), s.current_price) AS low_price, "
                + "COALESCE(MAX(t.price), s.current_price) AS high_price, "
                + "COUNT(t.trade_id) AS trade_count FROM stocks s "
                + "LEFT JOIN trades t ON t.stock_id = s.stock_id "
                + "WHERE s.stock_id = ? GROUP BY s.stock_id, s.current_price";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("The selected stock could not be found.");
                }
                return String.format("Last: $%s  |  Volume: %d  |  High: $%s  |  Low: $%s  |  Trades: %d",
                        results.getBigDecimal("current_price"), results.getLong("volume"),
                        results.getBigDecimal("high_price"), results.getBigDecimal("low_price"),
                        results.getInt("trade_count"));
            }
        }
    }

    public List<String> getOpenOrders(int userId) throws SQLException {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT s.symbol, o.order_type, o.price, o.remaining_quantity, o.order_status "
                + "FROM orders o JOIN stocks s ON s.stock_id = o.stock_id "
                + "WHERE o.user_id = ? AND o.order_status IN ('PENDING', 'PARTIALLY_FILLED') "
                + "ORDER BY o.created_at DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    lines.add(String.format("%s  %s  %d @ %s  (%s)",
                            results.getString("symbol"), results.getString("order_type"),
                            results.getInt("remaining_quantity"), results.getBigDecimal("price"),
                            results.getString("order_status")));
                }
            }
        }
        return lines;
    }

    private void lockStock(int stockId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock_id FROM stocks WHERE stock_id = ? FOR UPDATE")) {
            statement.setInt(1, stockId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new IllegalArgumentException("The selected stock no longer exists.");
                }
            }
        }
    }

    private void reserveFunds(int userId, BigDecimal amount) throws SQLException {
        String sql = "SELECT balance, reserved_balance FROM users WHERE user_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new IllegalArgumentException("User account was not found.");
                }
                BigDecimal available = results.getBigDecimal("balance")
                        .subtract(results.getBigDecimal("reserved_balance"));
                if (available.compareTo(amount) < 0) {
                    throw new IllegalArgumentException("Insufficient available balance for this order.");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET reserved_balance = reserved_balance + ? WHERE user_id = ?")) {
            statement.setBigDecimal(1, amount);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private void reserveShares(int userId, int stockId, int quantity) throws SQLException {
        int held;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT quantity FROM holdings WHERE user_id = ? AND stock_id = ? FOR UPDATE")) {
            statement.setInt(1, userId);
            statement.setInt(2, stockId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new IllegalArgumentException("You do not hold this stock.");
                }
                held = results.getInt("quantity");
            }
        }
        String sql = "SELECT COALESCE(SUM(remaining_quantity), 0) AS reserved FROM orders "
                + "WHERE user_id = ? AND stock_id = ? AND order_type = 'SELL' "
                + "AND order_status IN ('PENDING', 'PARTIALLY_FILLED')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, stockId);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                if (held - results.getInt("reserved") < quantity) {
                    throw new IllegalArgumentException("Insufficient unreserved shares for this order.");
                }
            }
        }
    }

    private int insertOrder(int userId, int stockId, OrderType type, int quantity, BigDecimal price)
            throws SQLException {
        String sql = "INSERT INTO orders (user_id, stock_id, order_type, quantity, remaining_quantity, "
                + "price, order_status) VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setInt(2, stockId);
            statement.setString(3, type.name());
            statement.setInt(4, quantity);
            statement.setInt(5, quantity);
            statement.setBigDecimal(6, price);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the new order ID.");
                }
                return keys.getInt(1);
            }
        }
    }

    private int matchOrder(int orderId, int userId, int stockId, OrderType type,
                           int quantity, BigDecimal price) throws SQLException {
        String sql;
        if (type == OrderType.BUY) {
            sql = "SELECT order_id, user_id, price, remaining_quantity FROM orders "
                    + "WHERE stock_id = ? AND order_type = 'SELL' AND user_id <> ? "
                    + "AND order_status IN ('PENDING', 'PARTIALLY_FILLED') AND price <= ? "
                    + "ORDER BY price ASC, created_at ASC, order_id ASC FOR UPDATE";
        } else {
            sql = "SELECT order_id, user_id, price, remaining_quantity FROM orders "
                    + "WHERE stock_id = ? AND order_type = 'BUY' AND user_id <> ? "
                    + "AND order_status IN ('PENDING', 'PARTIALLY_FILLED') AND price >= ? "
                    + "ORDER BY price DESC, created_at ASC, order_id ASC FOR UPDATE";
        }

        int remaining = quantity;
        int tradeCount = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockId);
            statement.setInt(2, userId);
            statement.setBigDecimal(3, price);
            try (ResultSet orders = statement.executeQuery()) {
                while (remaining > 0 && orders.next()) {
                    int restingId = orders.getInt("order_id");
                    int restingUser = orders.getInt("user_id");
                    BigDecimal restingPrice = orders.getBigDecimal("price");
                    int restingQuantity = orders.getInt("remaining_quantity");
                    int fill = Math.min(remaining, restingQuantity);
                    int buyId = type == OrderType.BUY ? orderId : restingId;
                    int sellId = type == OrderType.SELL ? orderId : restingId;
                    int buyerId = type == OrderType.BUY ? userId : restingUser;
                    int sellerId = type == OrderType.SELL ? userId : restingUser;
                    BigDecimal executionPrice = restingPrice;
                    executeTrade(buyId, sellId, buyerId, sellerId, stockId, fill, executionPrice,
                            type == OrderType.BUY ? price : restingPrice);
                    updateOrderFill(orderId, fill, remaining);
                    updateOrderFill(restingId, fill, restingQuantity);
                    remaining -= fill;
                    tradeCount++;
                }
            }
        }
        if (remaining > 0) {
            updateRemaining(orderId, remaining);
        }
        return tradeCount;
    }

    private void executeTrade(int buyOrderId, int sellOrderId, int buyerId, int sellerId,
                              int stockId, int quantity, BigDecimal price, BigDecimal buyLimit) throws SQLException {
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal buyerReserve = buyLimit.multiply(BigDecimal.valueOf(quantity));
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET balance = balance - ?, reserved_balance = reserved_balance - ? "
                        + "WHERE user_id = ? AND balance >= ? AND reserved_balance >= ?")) {
            statement.setBigDecimal(1, amount);
            statement.setBigDecimal(2, buyerReserve);
            statement.setInt(3, buyerId);
            statement.setBigDecimal(4, amount);
            statement.setBigDecimal(5, buyerReserve);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Buyer account changed while the trade was being processed.");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET balance = balance + ? WHERE user_id = ?")) {
            statement.setBigDecimal(1, amount);
            statement.setInt(2, sellerId);
            statement.executeUpdate();
        }
        changeHolding(buyerId, stockId, quantity, price, true);
        changeHolding(sellerId, stockId, quantity, price, false);

        int tradeId;
        String tradeSql = "INSERT INTO trades (buy_order_id, sell_order_id, stock_id, buyer_id, "
                + "seller_id, quantity, price) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(tradeSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, buyOrderId);
            statement.setInt(2, sellOrderId);
            statement.setInt(3, stockId);
            statement.setInt(4, buyerId);
            statement.setInt(5, sellerId);
            statement.setInt(6, quantity);
            statement.setBigDecimal(7, price);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the new trade ID.");
                }
                tradeId = keys.getInt(1);
            }
        }
        insertTransaction(buyerId, tradeId, stockId, "BUY", quantity, price, amount);
        insertTransaction(sellerId, tradeId, stockId, "SELL", quantity, price, amount);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stocks SET current_price = ? WHERE stock_id = ?")) {
            statement.setBigDecimal(1, price);
            statement.setInt(2, stockId);
            statement.executeUpdate();
        }
    }

    private void changeHolding(int userId, int stockId, int quantity, BigDecimal price, boolean buy)
            throws SQLException {
        if (buy) {
            String sql = "INSERT INTO holdings (user_id, stock_id, quantity, average_price) "
                    + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE average_price = "
                    + "(average_price * quantity + VALUES(average_price) * VALUES(quantity)) "
                    + "/ (quantity + VALUES(quantity)), quantity = quantity + VALUES(quantity)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                statement.setInt(2, stockId);
                statement.setInt(3, quantity);
                statement.setBigDecimal(4, price);
                statement.executeUpdate();
            }
        } else {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE holdings SET quantity = quantity - ? "
                            + "WHERE user_id = ? AND stock_id = ? AND quantity >= ?")) {
                statement.setInt(1, quantity);
                statement.setInt(2, userId);
                statement.setInt(3, stockId);
                statement.setInt(4, quantity);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Seller holdings changed while the trade was being processed.");
                }
            }
        }
    }

    private void insertTransaction(int userId, int tradeId, int stockId, String type,
                                   int quantity, BigDecimal price, BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO transactions "
                + "(user_id, trade_id, stock_id, transaction_type, quantity, price, amount) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, tradeId);
            statement.setInt(3, stockId);
            statement.setString(4, type);
            statement.setInt(5, quantity);
            statement.setBigDecimal(6, price);
            statement.setBigDecimal(7, amount);
            statement.executeUpdate();
        }
    }

    private void updateOrderFill(int orderId, int filled, int previousRemaining) throws SQLException {
        int remaining = previousRemaining - filled;
        String status = remaining == 0 ? "COMPLETED" : "PARTIALLY_FILLED";
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE orders SET remaining_quantity = ?, order_status = ? WHERE order_id = ?")) {
            statement.setInt(1, remaining);
            statement.setString(2, status);
            statement.setInt(3, orderId);
            statement.executeUpdate();
        }
    }

    private void updateRemaining(int orderId, int remaining) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE orders SET remaining_quantity = ?, order_status = "
                        + "CASE WHEN remaining_quantity < quantity THEN 'PARTIALLY_FILLED' ELSE 'PENDING' END "
                        + "WHERE order_id = ?")) {
            statement.setInt(1, remaining);
            statement.setInt(2, orderId);
            statement.executeUpdate();
        }
    }
}
