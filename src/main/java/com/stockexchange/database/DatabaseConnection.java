package com.stockexchange.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class.getResourceAsStream(
                "/com/stockexchange/application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new SQLException("Could not read database configuration.", exception);
        }

        String url = setting("MYSQL_URL", properties, "mysql.url",
                "jdbc:mysql://localhost:3306/stock_exchange");
        String username = setting("MYSQL_USER", properties, "mysql.username", "root");
        String password = setting("MYSQL_PASSWORD", properties, "mysql.password", null);

        if (password == null) {
            throw new SQLException("Set MYSQL_PASSWORD or mysql.password in application.properties.");
        }
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String environmentName, Properties properties,
                                  String propertyName, String defaultValue) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return properties.getProperty(propertyName, defaultValue);
    }
}
