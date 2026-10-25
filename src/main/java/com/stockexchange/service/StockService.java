package com.stockexchange.service;

import com.stockexchange.model.Stock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class StockService {
    private static final BigDecimal MINIMUM_PRICE = new BigDecimal("0.0001");
    private final Connection connection;

    public StockService(Connection connection) {
        this.connection = connection;
    }

    public List<Stock> getAllStocks() throws SQLException {
        List<Stock> stocks = new ArrayList<>();
        String sql = "SELECT * FROM stocks";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Stock stock = new Stock(
                    rs.getInt("stock_id"),
                    rs.getString("symbol"),
                    rs.getString("name"),
                    rs.getBigDecimal("current_price"),
                    rs.getInt("available_quantity")
                );
                stocks.add(stock);
            }
        }
        return stocks;
    }

    public List<Stock> simulatePriceTick() throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            List<Stock> stocks = getAllStocks();
            String sql = "UPDATE stocks SET current_price = ? WHERE stock_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Stock stock : stocks) {
                    double movement = ThreadLocalRandom.current().nextDouble(-0.01, 0.01);
                    BigDecimal updatedPrice = stock.getCurrentPrice()
                            .multiply(BigDecimal.valueOf(1 + movement))
                            .setScale(4, RoundingMode.HALF_UP)
                            .max(MINIMUM_PRICE);
                    statement.setBigDecimal(1, updatedPrice);
                    statement.setInt(2, stock.getStockId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
            return getAllStocks();
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public void addStock(Stock stock) throws SQLException {
        String sql = "INSERT INTO stocks (symbol, name, current_price, available_quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, stock.getSymbol());
            stmt.setString(2, stock.getName());
            stmt.setBigDecimal(3, stock.getCurrentPrice());
            stmt.setInt(4, stock.getAvailableQuantity());
            stmt.executeUpdate();
        }
    }

    public void updateStock(Stock stock) throws SQLException {
        String sql = "UPDATE stocks SET name = ?, current_price = ?, available_quantity = ? WHERE stock_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, stock.getName());
            stmt.setBigDecimal(2, stock.getCurrentPrice());
            stmt.setInt(3, stock.getAvailableQuantity());
            stmt.setInt(4, stock.getStockId());
            stmt.executeUpdate();
        }
    }
}
