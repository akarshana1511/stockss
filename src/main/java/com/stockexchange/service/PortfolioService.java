package com.stockexchange.service;

import com.stockexchange.model.Holding;
import com.stockexchange.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PortfolioService {
    private final Connection connection;

    public PortfolioService(Connection connection) {
        this.connection = connection;
    }

    public List<Holding> getUserPortfolio(User user) throws SQLException {
        List<Holding> holdings = new ArrayList<>();
        String sql = "SELECT * FROM holdings WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getUserId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Holding holding = new Holding(
                    rs.getInt("holding_id"),
                    rs.getInt("user_id"),
                    rs.getInt("stock_id"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("average_price")
                );
                holdings.add(holding);
            }
        }
        return holdings;
    }
}
