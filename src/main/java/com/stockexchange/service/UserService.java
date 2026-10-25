package com.stockexchange.service;

import com.stockexchange.exception.UserNotFoundException;
import com.stockexchange.model.User;
import com.stockexchange.model.enums.UserRole;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class UserService {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Connection connection;

    public UserService(Connection connection) {
        this.connection = connection;
    }

    public User authenticate(String email, char[] password)
            throws SQLException, UserNotFoundException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase());
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new UserNotFoundException("No account exists for that email.");
                }
                String storedPassword = results.getString("password");
                if (!verifyPassword(password, storedPassword)) {
                    throw new UserNotFoundException("Email or password is incorrect.");
                }
                return readUser(results);
            }
        }
    }

    public User getUserByEmail(String email) throws SQLException, UserNotFoundException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase());
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new UserNotFoundException("User not found.");
                }
                return readUser(results);
            }
        }
    }

    public void register(String name, String email, char[] password) throws SQLException {
        if (name == null || name.isBlank() || email == null || email.isBlank()
                || password == null || password.length < 8) {
            throw new IllegalArgumentException("Enter a name, email, and password of at least 8 characters.");
        }
        String sql = "INSERT INTO users (name, email, password, balance, reserved_balance, role) "
                + "VALUES (?, ?, ?, 10000.0000, 0, 'USER')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            statement.setString(2, email.trim().toLowerCase());
            statement.setString(3, hashPassword(password));
            statement.executeUpdate();
        }
    }

    private static User readUser(ResultSet results) throws SQLException {
        return new User(
                results.getInt("user_id"),
                results.getString("name"),
                results.getString("email"),
                results.getString("password"),
                results.getBigDecimal("balance"),
                results.getBigDecimal("reserved_balance"),
                UserRole.valueOf(results.getString("role").toUpperCase())
        );
    }

    private static String hashPassword(char[] password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = deriveKey(password, salt);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPassword(char[] password, String storedPassword) {
        if (storedPassword == null || !storedPassword.startsWith("pbkdf2$")) {
            return false;
        }
        String[] parts = storedPassword.split("\\$", -1);
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = deriveKey(password, salt, iterations, expected.length * 8);
            if (actual.length != expected.length) {
                return false;
            }
            int difference = 0;
            for (int index = 0; index < actual.length; index++) {
                difference |= actual[index] ^ expected[index];
            }
            return difference == 0;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt) {
        return deriveKey(password, salt, ITERATIONS, KEY_LENGTH);
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations, int keyLength) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, keyLength);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        } finally {
            specification.clearPassword();
        }
    }
}
