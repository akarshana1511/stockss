# Stock Exchange System

## Project Overview
The Stock Exchange System is a Java-based application designed to facilitate trading of stocks in a simulated environment. It provides functionalities for users to manage their portfolios, execute trades, and view transaction histories.

## Objective
To create a robust stock trading application that allows users to buy and sell stocks while ensuring data integrity and performance through efficient algorithms and database management.

## Features
- User Registration and Login
- Stock Trading (Buy/Sell)
- Portfolio Management
- Transaction History
- Admin Stock Management
- CSV Export of Transaction History

## Technologies Used
- Java 17
- Maven
- JavaFX
- MySQL
- JDBC

## System Architecture
The application follows a layered architecture:
- **Presentation Layer:** JavaFX GUI
- **Business Logic Layer:** Services and Matching Engine
- **Data Access Layer:** DAO classes for database interaction

## Project Structure
```
StockExchangeSystem/
├── database/
│   ├── schema.sql
│   └── sample_data.sql
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/stockexchange/
│   │   │   │   ├── model/
│   │   │   │   ├── service/
│   │   │   │   ├── database/
│   │   │   │   └── exception/
│   │   └── resources/
│   └── test/
│       └── java/
└── pom.xml
```

## Database Setup
1. Create a MySQL database named `stock_exchange`.
2. Execute `schema.sql` to create the necessary tables.
3. Execute `sample_data.sql` to populate initial stock data.

## MySQL Configuration
- Ensure MySQL is running and accessible.
- Update the database connection details in `DatabaseConnection.java` if necessary.

## How to Run the Project
1. Clone the repository.
2. Navigate to the project directory.
3. Use Maven to build the project: `mvn clean install`.
4. Run the application using JavaFX.

## Sample Login Credentials
- **User:**
  - Email: user@example.com
  - Password: password123
- **Admin:**
  - Email: admin@example.com
  - Password: admin123

## How the Order Matching Works
The matching engine processes orders based on price-time priority:
- **BUY Orders:** Highest price first, then earliest timestamp.
- **SELL Orders:** Lowest price first, then earliest timestamp.

A trade occurs when the best BUY price is greater than or equal to the best SELL price. The trade price is determined by the resting/older order's price.

## Price-Time Priority Explanation
- **BUY Orders:** Prioritize higher prices; if prices are equal, prioritize the earliest time.
- **SELL Orders:** Prioritize lower prices; if prices are equal, prioritize the earliest time.

## Partial Order Matching
The system supports partial fills, allowing for trades to execute even when the entire order quantity isn't matched.

## Portfolio Management
Users can view their holdings, including stock quantities and average prices.

## Transaction Management
All transactions are recorded in the database, allowing users to export their transaction history.

## Multithreading
The application supports concurrent order processing to simulate multiple users placing orders simultaneously.

## Java Collections Usage
Utilizes various Java collections for managing orders, stocks, and transactions efficiently.

## Streams and Lambda Usage
Java Streams and Lambda expressions are used for processing collections and enhancing code readability.

## JDBC Usage
JDBC is used for database interactions, ensuring data persistence and retrieval.

## JavaFX Usage
JavaFX is used for creating a responsive and modern GUI for user interactions.

## File Handling
The application allows users to export their transaction history to a CSV file using Java file handling APIs.

## Exception Handling
Custom exceptions are implemented to handle various error scenarios gracefully.

## Team Member Responsibilities
- **Member 1:** Core business logic, order book, matching engine, portfolio, trades, validation, exceptions, and concurrency.
- **Member 2:** Database, JDBC, DAO layer, JavaFX GUI, controllers, and file export.
