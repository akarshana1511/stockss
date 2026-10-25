package com.stockexchange.service;

import com.stockexchange.exception.UserNotFoundException;
import com.stockexchange.model.User;
import com.stockexchange.model.enums.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    private final Connection connection;

    public UserService(Connection connection) {
        this.connection = connection;
    }

    public User getUserByEmail(String email)
            throws SQLException, UserNotFoundException {

        String sql = "SELECT * FROM users WHERE email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    UserRole role = UserRole.valueOf(
                        rs.getString("role").toUpperCase()
                    );

                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getBigDecimal("balance"),
                        rs.getBigDecimal("reserved_balance"),
                        role
                    );

                } else {
                    throw new UserNotFoundException("User not found");
                }
            }
        }
    }
}