USE stock_exchange;

ALTER TABLE transactions
    ADD COLUMN price DECIMAL(19,4) NOT NULL DEFAULT 0 AFTER quantity;

UPDATE transactions t
JOIN trades tr ON tr.trade_id = t.trade_id
SET t.price = tr.price;
