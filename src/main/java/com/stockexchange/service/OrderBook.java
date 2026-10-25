package com.stockexchange.service;

import com.stockexchange.model.Order;
import com.stockexchange.model.enums.OrderStatus;
import com.stockexchange.model.enums.OrderType;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

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

    public void addBuyOrder(Order order) {
        if (order.getOrderType() != OrderType.BUY) {
            throw new IllegalArgumentException("Order is not a BUY order");
        }
        buyOrders.add(order);
    }

    public void addSellOrder(Order order) {
        if (order.getOrderType() != OrderType.SELL) {
            throw new IllegalArgumentException("Order is not a SELL order");
        }
        sellOrders.add(order);
    }

    public Order getBestBuyOrder() {
        return buyOrders.peek();
    }

    public Order getBestSellOrder() {
        return sellOrders.peek();
    }

    public boolean removeBuyOrder(Order order) {
        return buyOrders.remove(order);
    }

    public boolean removeSellOrder(Order order) {
        return sellOrders.remove(order);
    }

    public List<Order> getBuyOrders() {
        return new ArrayList<>(buyOrders);
    }

    public List<Order> getSellOrders() {
        return new ArrayList<>(sellOrders);
    }
}
