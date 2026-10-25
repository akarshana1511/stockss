package com.stockexchange.model;

import java.math.BigDecimal;

public class Holding {
    private int holdingId;
    private int userId;
    private int stockId;
    private int quantity;
    private BigDecimal averagePrice;

    public Holding(int holdingId, int userId, int stockId, int quantity, BigDecimal averagePrice) {
        this.holdingId = holdingId;
        this.userId = userId;
        this.stockId = stockId;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }

    public int getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(int holdingId) {
        this.holdingId = holdingId;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }
}
