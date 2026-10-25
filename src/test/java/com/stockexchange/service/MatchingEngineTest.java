package com.stockexchange.service;

import com.stockexchange.model.Order;
import com.stockexchange.model.Trade;
import com.stockexchange.model.enums.OrderStatus;
import com.stockexchange.model.enums.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MatchingEngineTest {

    private OrderBook orderBook;
    private MatchingEngine matchingEngine;

    @BeforeEach
    public void setup() {
        orderBook = new OrderBook();
        matchingEngine = new MatchingEngine(orderBook);
    }

    @Test
    public void testNormalMatch() {

        Order buyOrder = new Order(
            1, 1, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("500"),
            LocalDateTime.now().minusMinutes(1),
            OrderStatus.PENDING
        );

        Order sellOrder = new Order(
            2, 2, 1,
            OrderType.SELL,
            100, 100,
            new BigDecimal("490"),
            LocalDateTime.now(),
            OrderStatus.PENDING
        );

        matchingEngine.processOrder(buyOrder);

        List<Trade> trades = matchingEngine.processOrder(sellOrder);

        assertEquals(1, trades.size());

        Trade trade = trades.get(0);

        assertEquals(100, trade.getQuantity());

        // BUY order was resting/older, so trade price is 500
        assertEquals(
            new BigDecimal("500"),
            trade.getPrice()
        );
    }

    @Test
    public void testNoMatch() {

        Order buyOrder = new Order(
            1, 1, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("480"),
            LocalDateTime.now(),
            OrderStatus.PENDING
        );

        Order sellOrder = new Order(
            2, 2, 1,
            OrderType.SELL,
            100, 100,
            new BigDecimal("490"),
            LocalDateTime.now(),
            OrderStatus.PENDING
        );

        matchingEngine.processOrder(buyOrder);

        List<Trade> trades =
            matchingEngine.processOrder(sellOrder);

        assertTrue(trades.isEmpty());

        assertEquals(
            OrderStatus.PENDING,
            buyOrder.getStatus()
        );

        assertEquals(
            OrderStatus.PENDING,
            sellOrder.getStatus()
        );
    }

    @Test
    public void testPartialFill() {

        Order buyOrder = new Order(
            1, 1, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("500"),
            LocalDateTime.now().minusMinutes(1),
            OrderStatus.PENDING
        );

        Order sellOrder = new Order(
            2, 2, 1,
            OrderType.SELL,
            40, 40,
            new BigDecimal("490"),
            LocalDateTime.now(),
            OrderStatus.PENDING
        );

        matchingEngine.processOrder(buyOrder);

        List<Trade> trades =
            matchingEngine.processOrder(sellOrder);

        assertEquals(1, trades.size());

        Trade trade = trades.get(0);

        assertEquals(40, trade.getQuantity());

        // Older BUY order determines trade price
        assertEquals(
            new BigDecimal("500"),
            trade.getPrice()
        );

        assertEquals(60, buyOrder.getRemainingQuantity());
        assertEquals(0, sellOrder.getRemainingQuantity());

        assertEquals(
            OrderStatus.PARTIALLY_FILLED,
            buyOrder.getStatus()
        );

        assertEquals(
            OrderStatus.COMPLETED,
            sellOrder.getStatus()
        );
    }

    @Test
    public void testMultipleMatches() {

        Order buyOrder1 = new Order(
            1, 1, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("500"),
            LocalDateTime.now().minusMinutes(4),
            OrderStatus.PENDING
        );

        Order buyOrder2 = new Order(
            2, 2, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("490"),
            LocalDateTime.now().minusMinutes(3),
            OrderStatus.PENDING
        );

        Order sellOrder1 = new Order(
            3, 3, 1,
            OrderType.SELL,
            40, 40,
            new BigDecimal("480"),
            LocalDateTime.now().minusMinutes(2),
            OrderStatus.PENDING
        );

        Order sellOrder2 = new Order(
            4, 4, 1,
            OrderType.SELL,
            60, 60,
            new BigDecimal("490"),
            LocalDateTime.now().minusMinutes(1),
            OrderStatus.PENDING
        );

        List<Trade> allTrades = new ArrayList<>();

        matchingEngine.processOrder(buyOrder1);
        matchingEngine.processOrder(buyOrder2);

        allTrades.add(
            matchingEngine.processOrder(sellOrder1).get(0)
        );

        allTrades.addAll(
            matchingEngine.processOrder(sellOrder2)
        );

        assertEquals(2, allTrades.size());

        assertEquals(40, allTrades.get(0).getQuantity());
        assertEquals(60, allTrades.get(1).getQuantity());

        assertEquals(
            new BigDecimal("500"),
            allTrades.get(0).getPrice()
        );

        assertEquals(
            new BigDecimal("500"),
            allTrades.get(1).getPrice()
        );
    }

    @Test
    public void testPricePriority() {

        Order buyOrder1 = new Order(
            1, 1, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("500"),
            LocalDateTime.now().minusMinutes(2),
            OrderStatus.PENDING
        );

        Order buyOrder2 = new Order(
            2, 2, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("490"),
            LocalDateTime.now().minusMinutes(1),
            OrderStatus.PENDING
        );

        matchingEngine.processOrder(buyOrder1);
        matchingEngine.processOrder(buyOrder2);

        Order sellOrder = new Order(
            3, 3, 1,
            OrderType.SELL,
            100, 100,
            new BigDecimal("490"),
            LocalDateTime.now(),
            OrderStatus.PENDING
        );

        List<Trade> trades =
            matchingEngine.processOrder(sellOrder);

        assertEquals(1, trades.size());

        Trade trade = trades.get(0);

        // Highest BUY price gets priority.
        assertEquals(
            buyOrder1.getOrderId(),
            trade.getBuyOrderId()
        );

        // The older/resting BUY price is used.
        assertEquals(
            new BigDecimal("500"),
            trade.getPrice()
        );
    }

    @Test
    public void testTimePriority() {

        Order buyOrder1 = new Order(
            1, 1, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("490"),
            LocalDateTime.now().minusMinutes(2),
            OrderStatus.PENDING
        );

        Order buyOrder2 = new Order(
            2, 2, 1,
            OrderType.BUY,
            100, 100,
            new BigDecimal("490"),
            LocalDateTime.now().minusMinutes(1),
            OrderStatus.PENDING
        );

        matchingEngine.processOrder(buyOrder1);
        matchingEngine.processOrder(buyOrder2);

        Order sellOrder = new Order(
            3, 3, 1,
            OrderType.SELL,
            100, 100,
            new BigDecimal("490"),
            LocalDateTime.now(),
            OrderStatus.PENDING
        );

        List<Trade> trades =
            matchingEngine.processOrder(sellOrder);

        assertEquals(1, trades.size());

        Trade trade = trades.get(0);

        // Same price -> earlier BUY gets priority.
        assertEquals(
            buyOrder1.getOrderId(),
            trade.getBuyOrderId()
        );

        assertEquals(
            new BigDecimal("490"),
            trade.getPrice()
        );
    }
}