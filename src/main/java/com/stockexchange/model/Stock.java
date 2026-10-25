package com.stockexchange.model;

import java.math.BigDecimal;

public class Stock {
    private int stockId;
    private String symbol;
    private String name;
    private BigDecimal currentPrice;
    private int availableQuantity;

    public Stock(int stockId, String symbol, String name, BigDecimal currentPrice, int availableQuantity) {
        this.stockId = stockId;
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.availableQuantity = availableQuantity;
    }

    public int getStockId() {
        return stockId;
    }

    public void setStockId(int stockId) {
        this.stockId = stockId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
