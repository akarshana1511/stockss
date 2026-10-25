package com.stockexchange.model;

import com.stockexchange.model.enums.OrderStatus;
import com.stockexchange.model.enums.OrderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private int orderId;
    private int userId;
    private int stockId;
    private OrderType orderType;
    private int quantity;
    private int remainingQuantity;
    private BigDecimal price;
    private LocalDateTime timestamp;
    private OrderStatus status;

    public Order(int orderId, int userId, int stockId, OrderType orderType, int quantity, int remainingQuantity, BigDecimal price, LocalDateTime timestamp, OrderStatus status) {
        this.orderId = orderId;
        this.userId = userId;
        this.stockId = stockId;
        this.orderType = orderType;
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
        this.price = price;
        this.timestamp = timestamp;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getStockId() {
        return stockId;
    }

    public void setStockId(int stockId) {
        this.stockId = stockId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(int remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
