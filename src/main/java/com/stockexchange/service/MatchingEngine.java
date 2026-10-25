package com.stockexchange.service;

import com.stockexchange.model.Order;
import com.stockexchange.model.Trade;
import com.stockexchange.model.enums.OrderStatus;
import com.stockexchange.model.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MatchingEngine {

    private final OrderBook orderBook;
    private int tradeCounter = 1;

    public MatchingEngine(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    public List<Trade> processOrder(Order order) {
        if (order.getOrderType() == OrderType.BUY) {
            orderBook.addBuyOrder(order);
        } else {
            orderBook.addSellOrder(order);
        }

        List<Trade> trades = new ArrayList<>();

        while (true) {
            Order bestBuy = orderBook.getBestBuyOrder();
            Order bestSell = orderBook.getBestSellOrder();

            if (bestBuy == null || bestSell == null) {
                break;
            }

            // Matching condition
            if (bestBuy.getPrice().compareTo(bestSell.getPrice()) < 0) {
                break;
            }

            // Determine trade quantity
            int tradeQuantity = Math.min(bestBuy.getRemainingQuantity(), bestSell.getRemainingQuantity());

            // Price of resting/older order
            BigDecimal tradePrice = bestBuy.getTimestamp().isBefore(bestSell.getTimestamp()) ? bestBuy.getPrice() : bestSell.getPrice();

            // Create trade
            Trade trade = new Trade(
                    tradeCounter++,
                    bestBuy.getOrderId(),
                    bestSell.getOrderId(),
                    bestBuy.getStockId(),
                    bestBuy.getUserId(),
                    bestSell.getUserId(),
                    tradeQuantity,
                    tradePrice,
                    LocalDateTime.now()
            );
            trades.add(trade);

            // Update order quantities
            bestBuy.setRemainingQuantity(bestBuy.getRemainingQuantity() - tradeQuantity);
            bestSell.setRemainingQuantity(bestSell.getRemainingQuantity() - tradeQuantity);

            // Update order statuses
            updateOrderStatus(bestBuy);
            updateOrderStatus(bestSell);

            // Remove completed orders
            if (bestBuy.getRemainingQuantity() == 0) {
                orderBook.removeBuyOrder(bestBuy);
            }
            if (bestSell.getRemainingQuantity() == 0) {
                orderBook.removeSellOrder(bestSell);
            }
        }

        return trades;
    }

    private void updateOrderStatus(Order order) {
        if (order.getRemainingQuantity() == 0) {
            order.setStatus(OrderStatus.COMPLETED);
        } else if (order.getRemainingQuantity() < order.getQuantity()) {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        } else {
            order.setStatus(OrderStatus.PENDING);
        }
    }
}
