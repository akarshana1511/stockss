package com.stockexchange.service;

import com.stockexchange.model.Stock;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockService {
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
