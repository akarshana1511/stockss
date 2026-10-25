# Stock Exchange System

A JavaFX stock-trading simulation backed by MySQL and JDBC. Users can create an account, browse listed stocks, place limit buy/sell orders, view their portfolio and open orders, and export executed trades.

## Requirements

- JDK 17
- Maven
- MySQL 8+

## Database setup

1. Start MySQL.
2. Execute `database/schema.sql` to create the `stock_exchange` database and tables.
3. Execute `database/sample_data.sql` to insert the initial stock listings.
   If you already have the earlier database schema installed, execute
   `database/migration_add_transaction_price.sql` once before running the updated app.
4. Configure the database connection using environment variables:

   ```powershell
   $env:MYSQL_URL = "jdbc:mysql://localhost:3306/stock_exchange"
   $env:MYSQL_USER = "root"
   $env:MYSQL_PASSWORD = "<your-local-mysql-password>"
   ```

   Alternatively, set `mysql.url`, `mysql.username`, and `mysql.password` in `src/main/resources/com/stockexchange/application.properties`. Environment variables take precedence.

## Run

From the project directory:

```powershell
mvn clean javafx:run
```

Create an account in the application. New accounts receive $10,000 of simulated funds. Passwords are stored as PBKDF2 hashes.

## Trading behavior

- Buy orders are ranked by highest price, then earliest submission.
- Sell orders are ranked by lowest price, then earliest submission.
- Orders execute when the highest bid meets or exceeds the lowest ask. Trades execute at the resting order's limit price.
- Orders support partial fills; unfilled quantities remain on the book.
- Buy orders reserve their limit-price value until they fill. Unfilled sell orders reserve shares from further sell orders.
- Each fill updates balances, holdings, the stock's last price, trades, and both users' transaction histories atomically.
- The market view shows the selected stock's aggregated open order book. The portfolio, open orders, and trade history are available in their own tabs.
- Simulated market prices move randomly by up to 1% every five seconds and are saved to the database. This ticker runs in a scheduled background thread; updates are serialized through the database worker. Simulated price changes do not execute trades or change open limit orders.
- The market view also shows the current simulated market price, all-time traded volume, high/low trade prices, and number of trades.
- Trade history can be exported as a CSV file.
- Database operations run on a dedicated background worker so login, order placement, refreshes, and exports do not block the JavaFX interface. The worker serializes JDBC operations because the app uses one shared database connection.

## Tests

```powershell
mvn test
```

The automated tests cover order price/time priority, partial fills, multiple matches, and stock-specific matching. A configured and running MySQL instance is required to run the application, but not the unit tests.
