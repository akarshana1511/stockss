package com.stockexchange.service;

import com.stockexchange.model.Order;
import com.stockexchange.model.enums.OrderStatus;
import com.stockexchange.model.enums.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    public void setup() {
        orderBook = new OrderBook();
    }

    @Test
    public void testBuyOrderPriority() {
        Order order1 = new Order(1, 1, 1, OrderType.BUY, 100, 100, new BigDecimal("500"), LocalDateTime.now().minusMinutes(2), OrderStatus.PENDING);
        Order order2 = new Order(2, 2, 1, OrderType.BUY, 100, 100, new BigDecimal("500"), LocalDateTime.now().minusMinutes(1), OrderStatus.PENDING);
        Order order3 = new Order(3, 3, 1, OrderType.BUY, 100, 100, new BigDecimal("600"), LocalDateTime.now(), OrderStatus.PENDING);

        orderBook.addBuyOrder(order1);
        orderBook.addBuyOrder(order2);
        orderBook.addBuyOrder(order3);

        List<Order> buyOrders = orderBook.getBuyOrders();
        assertEquals(3, buyOrders.size());

        Order bestBuy = orderBook.getBestBuyOrder();
        assertEquals(order3, bestBuy, "Highest price should be best buy order");

        orderBook.removeBuyOrder(bestBuy);

        bestBuy = orderBook.getBestBuyOrder();
        assertEquals(order1, bestBuy, "Earlier timestamp should be next best buy order when prices are equal");
    }

    @Test
    public void testSellOrderPriority() {
        Order order1 = new Order(1, 1, 1, OrderType.SELL, 100, 100, new BigDecimal("500"), LocalDateTime.now().minusMinutes(2), OrderStatus.PENDING);
        Order order2 = new Order(2, 2, 1, OrderType.SELL, 100, 100, new BigDecimal("500"), LocalDateTime.now().minusMinutes(1), OrderStatus.PENDING);
        Order order3 = new Order(3, 3, 1, OrderType.SELL, 100, 100, new BigDecimal("400"), LocalDateTime.now(), OrderStatus.PENDING);

        orderBook.addSellOrder(order1);
        orderBook.addSellOrder(order2);
        orderBook.addSellOrder(order3);

        List<Order> sellOrders = orderBook.getSellOrders();
        assertEquals(3, sellOrders.size());

        Order bestSell = orderBook.getBestSellOrder();
        assertEquals(order3, bestSell, "Lowest price should be best sell order");

        orderBook.removeSellOrder(bestSell);

        bestSell = orderBook.getBestSellOrder();
        assertEquals(order1, bestSell, "Earlier timestamp should be next best sell order when prices are equal");
    }
}
