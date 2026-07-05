# Bank App

Bank App is a Spring Boot backend for user authentication, account management, transfers, deposits, withdrawals, receipts, statements, and admin operations.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security OAuth2 Resource Server
- Spring Data JPA
- PostgreSQL
- Flyway
- Redis
- RabbitMQ
- AWS Cognito
- Spring Mail
- Maven
- Docker

## Features

- Cognito-backed signup, login, token refresh, logout, password reset, and user updates
- JWT-secured endpoints
- Account creation and account lookup
- Deposits, withdrawals, and transfers
- Idempotent transfers using the `idempotency-key` request header
- PDF receipt download
- Account statement generation and email dispatch
- Admin user/account lock controls
- Role updates through Cognito groups
- Transaction reversal and audit log access
- Flyway database migrations
- Swagger/OpenAPI documentation

## Project Structure

```text
src/main/java/com/sylvester/bankapp
├── account          account API, entity, repository, service
├── admin            admin API and service
├── audit            audit log entity, repository, service
├── config           security, Redis, Cognito, Swagger config
├── exception        application exceptions and handler
├── notification     email support
├── rabbitmq         RabbitMQ producer, consumer, events, config
├── receipt          receipt and statement generation
├── redis            token and rate limiting support
├── transaction      transaction API, entity, repository, service
└── user             auth API, DTOs, entity, repository, service
```

Important files:

- `pom.xml`: Maven dependencies and build configuration
- `src/main/resources/application.yaml`: Spring configuration
- `src/main/resources/db/migration`: Flyway migrations
- `docker-compose.yml`: local PostgreSQL and Redis
- `Dockerfile`: application image build
- `bank_request.http`: sample HTTP requests

## Prerequisites

- JDK 21
- Docker and Docker Compose
- Maven, or the included Maven wrapper `./mvnw`
- AWS Cognito user pool and app client
- RabbitMQ broker
- SMTP credentials for mail delivery

## Local Infrastructure

The included Compose file starts PostgreSQL and Redis:

```bash
docker compose up -d
```

Current local ports:

```text
PostgreSQL: localhost:5454 -> container 5432
Redis:      localhost:6370 -> container 6379
```

Create a `.env` file for Docker Compose:

```properties
DB_NAME=bank_app
DB_USERNAME=your_username
DB_PASSWORD=your_password
REDIS_PASSWORD=your_redis_password
```

Use these values in the application environment:

```properties
DB_URL=jdbc:postgresql://localhost:5454/bank_app
DB_USERNAME=your_username
DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6370
REDIS_PASSWORD=your_redis_password
```

## Application Configuration

`application.yaml` reads configuration from environment variables. Set these before running the app:

```properties
SERVER_PORT=8070

DB_URL=jdbc:postgresql://localhost:5454/bank_app
DB_USERNAME=your_username
DB_PASSWORD=your_password
DDL_AUTO=validate

REDIS_HOST=localhost
REDIS_PORT=6370
REDIS_PASSWORD=your_redis_password

JWT_ISSUER_URI=https://cognito-idp.<region>.amazonaws.com/<user-pool-id>

AWS_REGION=<region>
AWS_COGNITO_USER_POOL=<user-pool-id>
AWS_COGNITO_CLIENT_ID=<client-id>
AWS_COGNITO_CLIENT_SECRET=<client-secret>

RABBITMQ_HOST=<host>
RABBITMQ_PORT=5671
RABBITMQ_USERNAME=<username>
RABBITMQ_PASSWORD=<password>
RABBITMQ_VIRTUAL_HOST=<vhost>

MAIL_HOST=<smtp-host>
MAIL_PORT=587
MAIL_USERNAME=<smtp-user>
MAIL_PASSWORD=<smtp-password>

TRANSFER_QUEUE=transfer.queue
TRANSFER_EXCHANGE=transfer.exchange
TRANSFER_ROUTING_KEY=transfer.key

DEPOSIT_QUEUE=deposit.queue
DEPOSIT_EXCHANGE=deposit.exchange
DEPOSIT_ROUTING_KEY=deposit.key

WITHDRAW_QUEUE=withdraw.queue
WITHDRAW_EXCHANGE=withdraw.exchange
WITHDRAW_ROUTING_KEY=withdraw.key

ACCOUNT_QUEUE=account.queue
ACCOUNT_EXCHANGE=account.exchange
ACCOUNT_ROUTING_KEY=account.key

STATEMENT_QUEUE=statement.queue
STATEMENT_EXCHANGE=statement.exchange
STATEMENT_ROUTING_KEY=statement.key
```

## Run Locally

Start PostgreSQL and Redis:

```bash
docker compose up -d
```

Run the app:

```bash
./mvnw spring-boot:run
```

Or build and run the jar:

```bash
./mvnw clean package
java -jar target/bank-app.jar
```

The API runs on:

```text
http://localhost:${SERVER_PORT}
```

## Run with Docker

Build the image:

```bash
docker build -t bank-app .
```

Run the container with environment variables:

```bash
docker run --rm -p 8070:8070 --env-file .env bank-app
```

If the app runs inside Docker and uses the Compose network, use service names instead of localhost:

```properties
DB_URL=jdbc:postgresql://postgres:5432/bank_app
REDIS_HOST=redis
REDIS_PORT=6379
```

## API Endpoints

### Auth

Base path: `/api/v1/auth`

```text
POST /public/signup
POST /public/resend
POST /public/login
POST /public/forgot-password
PUT  /public/reset-password
POST /refresh
POST /logout
GET  /user
PUT  /users/user/update
PUT  /update-password
```

### Accounts

Base path: `/api/accounts`

```text
POST /          create account
GET  /all       list accounts
GET  /me        list authenticated user's accounts
```

### Transactions

Base path: `/api/transactions`

```text
POST /transfer      requires idempotency-key header
POST /withdraw
POST /deposit
GET  /receipt       requires transactionId query param
POST /statement
GET  /me/{accountId} list recent transactions for one owned account
```

### Admin

Base path: `/api/admin`

```text
POST   /lock-user
POST   /unlock-user
POST   /lock-account
POST   /unlock-account
POST   /revert
POST   /update-role
DELETE /remove-role
DELETE /delete-account
DELETE /delete-user
GET    /transactions
GET    /users
GET    /audit-logs
GET    /admins
```

## API Documentation

Swagger UI:

```text
http://localhost:${SERVER_PORT}/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:${SERVER_PORT}/v3/api-docs
```

## Testing

Run all tests:

```bash
./mvnw clean test
```

Run one test class:

```bash
./mvnw test -Dtest=TransactionServiceTest
```

Current tests cover service logic and controller request handling with JUnit 5, Mockito, AssertJ, and standalone MockMvc. The suite includes auth/user, account, beneficiary, transaction, admin, audit, user-security, email notification, and Redis rate-limiting tests.

## Notes

- Public auth endpoints are under `/api/v1/auth/public/**`.
- Other endpoints require a valid JWT issued by the configured Cognito issuer.
- Transfers must include an `idempotency-key` header. Reusing the same key with a different transfer request is rejected.
- Flyway runs database migrations on startup.
- The Compose file currently provides PostgreSQL and Redis only. RabbitMQ must be supplied separately.
- Maven currently warns that `spring-boot-starter-data-redis` is declared twice in `pom.xml`.
