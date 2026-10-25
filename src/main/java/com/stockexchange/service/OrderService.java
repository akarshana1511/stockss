package com.stockexchange.service;

import com.stockexchange.model.Order;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OrderService {
    private final Connection connection;

    public OrderService(Connection connection) {
        this.connection = connection;
    }

    public void addOrder(Order order) throws SQLException {
        String sql = "INSERT INTO orders (user_id, stock_id, order_type, quantity, remaining_quantity, price, order_status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, order.getUserId());
            stmt.setInt(2, order.getStockId());
            stmt.setString(3, order.getOrderType().name());
            stmt.setInt(4, order.getQuantity());
            stmt.setInt(5, order.getRemainingQuantity());
            stmt.setBigDecimal(6, order.getPrice());
            stmt.setString(7, order.getStatus().name());
            stmt.executeUpdate();
        }
    }
}
