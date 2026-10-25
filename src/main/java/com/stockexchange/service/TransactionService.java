package com.stockexchange.service;

import com.stockexchange.model.User;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionService {

    private final Connection connection;

    public TransactionService(Connection connection) {
        this.connection = connection;
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