package com.stockexchange.service;

import com.stockexchange.exception.InsufficientBalanceException;
import com.stockexchange.exception.InsufficientStockException;
import com.stockexchange.exception.InvalidOrderException;
import com.stockexchange.model.Holding;
import com.stockexchange.model.Order;
import com.stockexchange.model.User;
import com.stockexchange.model.enums.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class AccountService {

    private final Map<Integer, User> users = new HashMap<>();
    private final Map<Integer, Map<Integer, Holding>> userHoldings = new HashMap<>();

    public void addUser(User user) {
        users.put(user.getUserId(), user);
        userHoldings.computeIfAbsent(user.getUserId(), ignored -> new HashMap<>());
    }

    public User getUser(int userId) {
        return users.get(userId);
    }

    public void validateOrder(Order order) throws InvalidOrderException, InsufficientBalanceException, InsufficientStockException {
        if (order.getQuantity() <= 0) {
            throw new InvalidOrderException("Quantity must be greater than 0");
        }
        if (order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Price must be greater than 0");
        }

        User user = users.get(order.getUserId());
        if (user == null) {
            throw new InvalidOrderException("User not found");
        }

        if (order.getOrderType() == OrderType.BUY) {
            BigDecimal totalCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            if (user.getBalance().compareTo(totalCost) < 0) {
                throw new InsufficientBalanceException("Insufficient balance for BUY order");
            }
        } else if (order.getOrderType() == OrderType.SELL) {
            Map<Integer, Holding> holdings = userHoldings.get(order.getUserId());
            Holding holding = holdings != null ? holdings.get(order.getStockId()) : null;
            if (holding == null || holding.getQuantity() < order.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock holdings for SELL order");
            }
        }
    }

    public void processTrade(Order buyOrder, Order sellOrder, int quantity, BigDecimal price) {
        User buyer = users.get(buyOrder.getUserId());
        User seller = users.get(sellOrder.getUserId());

        BigDecimal tradeAmount = price.multiply(BigDecimal.valueOf(quantity));

        // Update buyer balance
        buyer.setBalance(buyer.getBalance().subtract(tradeAmount));

        // Update seller balance
        seller.setBalance(seller.getBalance().add(tradeAmount));

        // Update buyer holdings
        Map<Integer, Holding> buyerHoldings = userHoldings.get(buyer.getUserId());
        Holding buyerHolding = buyerHoldings.get(buyOrder.getStockId());
        if (buyerHolding == null) {
            buyerHolding = new Holding(0, buyer.getUserId(), buyOrder.getStockId(), 0, BigDecimal.ZERO);
            buyerHoldings.put(buyer.getUserId(), buyerHolding);
        }
        int newQuantity = buyerHolding.getQuantity() + quantity;
        BigDecimal totalCost = buyerHolding.getAveragePrice().multiply(BigDecimal.valueOf(buyerHolding.getQuantity()))
                .add(price.multiply(BigDecimal.valueOf(quantity)));
        BigDecimal newAvgPrice = totalCost.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);
        buyerHolding.setQuantity(newQuantity);
        buyerHolding.setAveragePrice(newAvgPrice);

        // Update seller holdings
        Map<Integer, Holding> sellerHoldings = userHoldings.get(seller.getUserId());
        Holding sellerHolding = sellerHoldings.get(sellOrder.getStockId());
        if (sellerHolding == null || sellerHolding.getQuantity() < quantity) {
            throw new IllegalArgumentException("Seller has insufficient stock holdings.");
        }
        sellerHolding.setQuantity(sellerHolding.getQuantity() - quantity);
    }
}
