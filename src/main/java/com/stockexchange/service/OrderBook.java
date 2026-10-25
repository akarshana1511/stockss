package com.stockexchange.service;

import com.stockexchange.model.Order;
import com.stockexchange.model.enums.OrderType;

import java.util.*;

public class OrderBook {

    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;

    public OrderBook() {
        buyOrders = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o2.getPrice().compareTo(o1.getPrice());
            if (priceCompare != 0) {
                return priceCompare;
            }
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        });

        sellOrders = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o1.getPrice().compareTo(o2.getPrice());
            if (priceCompare != 0) {
                return priceCompare;
            }
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        });
    }

    public synchronized void addBuyOrder(Order order) {
        if (order.getOrderType() != OrderType.BUY) {
            throw new IllegalArgumentException("Order is not a BUY order");
        }
        buyOrders.add(order);
    }

    public synchronized void addSellOrder(Order order) {
        if (order.getOrderType() != OrderType.SELL) {
            throw new IllegalArgumentException("Order is not a SELL order");
        }
        sellOrders.add(order);
    }

    public synchronized Order getBestBuyOrder() {
        return buyOrders.peek();
    }

    public synchronized Order getBestSellOrder() {
        return sellOrders.peek();
    }

    public synchronized boolean removeBuyOrder(Order order) {
        return buyOrders.remove(order);
    }

    public synchronized boolean removeSellOrder(Order order) {
        return sellOrders.remove(order);
    }

    public synchronized List<Order> getBuyOrders() {
        return new ArrayList<>(buyOrders);
    }

    public synchronized List<Order> getSellOrders() {
        return new ArrayList<>(sellOrders);
    }
}
