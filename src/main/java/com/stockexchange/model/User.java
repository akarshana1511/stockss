package com.stockexchange.model;

import com.stockexchange.model.enums.UserRole;
import java.math.BigDecimal;

public class User {

    private int userId;
    private String name;
    private String email;
    private String password;
    private BigDecimal balance;
    private BigDecimal reservedBalance;
    private UserRole role;

    public User(
            int userId,
            String name,
            String email,
            String password,
            BigDecimal balance,
            BigDecimal reservedBalance,
            UserRole role) {

        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getReservedBalance() {
        return reservedBalance;
    }

    public void setReservedBalance(BigDecimal reservedBalance) {
        this.reservedBalance = reservedBalance;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}