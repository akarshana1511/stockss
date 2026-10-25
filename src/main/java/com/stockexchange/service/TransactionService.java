package com.stockexchange.service;

import com.stockexchange.model.User;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionService {

    private final Connection connection;

    public TransactionService(Connection connection) {
        this.connection = connection;
    }

    public List<String> getTransactionSummary(User user) throws SQLException {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT t.created_at, s.symbol, t.transaction_type, t.quantity, t.price, t.amount "
                + "FROM transactions t JOIN stocks s ON s.stock_id = t.stock_id "
                + "WHERE t.user_id = ? ORDER BY t.created_at DESC, t.transaction_id DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, user.getUserId());
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    lines.add(String.format("%s  %-6s %-4s %d @ %s (total %s)",
                            results.getTimestamp("created_at"), results.getString("symbol"),
                            results.getString("transaction_type"), results.getInt("quantity"),
                            results.getBigDecimal("price"), results.getBigDecimal("amount")));
                }
            }
        }
        return lines;
    }

    public void exportTransactions(User user, String filePath) throws SQLException {

        String sql = "SELECT * FROM transactions WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, user.getUserId());

            try (ResultSet rs = stmt.executeQuery();
                 BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

                writer.write("Date,Stock,Type,Quantity,Price,Amount");
                writer.newLine();

                while (rs.next()) {

                    String line = String.format(
                        "%s,%s,%s,%d,%s,%s",
                        rs.getTimestamp("created_at"),
                        rs.getInt("stock_id"),
                        rs.getString("transaction_type"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("amount")
                    );

                    writer.write(line);
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            throw new SQLException("Failed to export transactions to CSV.", e);
        }
    }
}