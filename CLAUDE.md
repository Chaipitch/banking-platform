# Core Banking Platform

## Project overview
A microservices-based core banking system built to practice backend engineering: Spring Boot, gRPC, Kafka, PostgreSQL, MongoDB, Redis. The person building this is a **junior developer practicing** — code should be explained, not just generated. Prefer scaffolding + review over writing full business logic outright, unless explicitly asked to just build it.

## Architecture
Client → API Gateway → services. Services talk to each other over **gRPC** (synchronous, internal) and coordinate via **Kafka** (async events). See `/docs/architecture.md` if present, otherwise refer to the phase breakdown below.

| Service | Responsibility | Data store | Talks to |
|---|---|---|---|
| `account-service` | Customers, accounts, balances | PostgreSQL | gRPC server for balance ops |
| `transaction-service` | Deposits, withdrawals, transfers, Saga orchestration | PostgreSQL | gRPC client → account-service; Kafka producer |
| `notification-service` | Sends/logs notifications | MongoDB | Kafka consumer |
| `fraud-service` | Real-time velocity/fraud checks | Redis | Kafka consumer/producer |
| `auth-service` | Login, JWT issuance, token blacklist | PostgreSQL + Redis | REST |
| `api-gateway` | Routing, JWT validation, rate limiting | — | Spring Cloud Gateway |

## Tech stack & conventions
- **Java + Spring Boot 3.x**, Maven (or Gradle — pick one and stay consistent across services)
- **Money is always `BigDecimal`** — never `double`/`float`, anywhere, no exceptions
- **DB migrations only** — Flyway or Liquibase. No `ddl-auto: update` outside local scratch work
- **DTOs at API boundaries** — never expose JPA entities directly in REST/gRPC responses
- **gRPC contracts** live in a shared `proto/` module — treat `.proto` files as the source of truth, regenerate stubs, don't hand-edit generated code
- **Kafka topics** follow `banking.<domain>.<event>` naming, e.g. `banking.transactions.completed`
- **Testcontainers** for integration tests — real Postgres/Kafka/Mongo, not H2 or embedded fakes
- **Optimistic locking** (`@Version`) on `Account.balance` — concurrent debits must not corrupt balances

## Build order (do not skip ahead)
1. Account Service (REST + Postgres)
2. gRPC between Account and Transaction Service
3. Kafka event publishing from Transaction Service
4. Notification Service (Kafka consumer + MongoDB)
5. Fraud Service (Kafka consumer + Redis)
6. Saga / compensating transactions (fixes the distributed-transaction gap from step 2)
7. Auth Service + API Gateway
8. Full docker-compose integration

Full task list with requirements and acceptance criteria per task lives in Notion: *Core Banking Platform — Learning Tasks*.

## Common commands
```bash
docker-compose up -d          # bring up infra (Postgres, Kafka, Mongo, Redis)
docker-compose down -v        # tear down + wipe volumes
./mvnw spring-boot:run         # run a service (from its module directory)
./mvnw test                    # run tests (Testcontainers will spin up real deps)
grpcurl -plaintext localhost:<port> list   # inspect available gRPC methods
```
*(adjust paths/ports once each service exists — update this section as the project grows)*

## Working agreement
- This is a learning project. When asked to implement something non-trivial (gRPC concurrency handling, the Saga logic, fraud velocity checks), explain the approach and the trade-offs before or alongside writing code — don't just drop a finished implementation silently.
- Flag when a requested shortcut (e.g. `ddl-auto: update`, `double` for money, skipping tests) conflicts with the conventions above, rather than silently complying.
- Keep each service isolated — no shared database, no direct DB access across service boundaries. Cross-service communication only via gRPC or Kafka.
- When a task is finished, note it so the Notion board can be updated to reflect status.
