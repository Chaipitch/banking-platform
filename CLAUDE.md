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
- **Java 21 + Spring Boot 4.1**, **Maven** (via `./mvnw` wrapper) — every service uses the same
- Spring Boot 4 splits auto-configuration into modules — use the `spring-boot-starter-*` artifact (e.g. `spring-boot-starter-liquibase`), not the bare library, or it won't auto-configure
- **Money is always `BigDecimal`** — never `double`/`float`, anywhere, no exceptions
- **DB migrations only — Liquibase (XML changelogs)** for every Postgres-backed service. Hibernate `ddl-auto` stays `none`/`validate`; no `update` outside local scratch work
  - Master changelog: `src/main/resources/db/changelog/db.changelog-master.xml`, which `<include>`s files from `changes/`
  - One file per change, numbered: `changes/NNN-description.xml`
  - Never edit a changeSet that has already run — add a new one
- **Pin Docker image versions** (e.g. `postgres:17`) — never `latest`
- **DTOs at API boundaries** — never expose JPA entities directly in REST/gRPC responses
- **gRPC contracts** live in a shared `proto/` module — treat `.proto` files as the source of truth, regenerate stubs, don't hand-edit generated code
- **Kafka topics** follow `banking.<domain>.<event>` naming, e.g. `banking.transactions.completed`
- **Testcontainers** for integration tests — real Postgres/Kafka/Mongo, not H2 or embedded fakes
- **Optimistic locking** to protect `Account.balance` — `@Version` goes on a **separate** `Long version` field (never on `balance` itself; Hibernate increments it on every update) so concurrent debits must not corrupt balances

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
# from repo root (banking-platform/)
docker compose up -d           # bring up infra
docker compose ps              # check containers are "Up"
docker compose logs <service>  # first stop when a container won't start
docker compose down            # stop, keep data
docker compose down -v         # stop + WIPE volumes (all DB data)

# from a service directory (e.g. account-service/)
./mvnw spring-boot:run         # run the service
./mvnw test                    # run tests (Testcontainers will spin up real deps)

grpcurl -plaintext localhost:<port> list   # inspect available gRPC methods
```

## Ports & local services
| Service | App port | Datastore | Host port → container | DB name |
|---|---|---|---|---|
| `account-service` | 4100 | Postgres 17 (`account-service-db`) | 5433 → 5432 | `account_postgres_db` |

Host port 5432 is avoided to prevent clashes with any locally installed Postgres.
*(add a row as each service is created)*

## Working agreement
- This is a learning project. When asked to implement something non-trivial (gRPC concurrency handling, the Saga logic, fraud velocity checks), explain the approach and the trade-offs before or alongside writing code — don't just drop a finished implementation silently.
- Flag when a requested shortcut (e.g. `ddl-auto: update`, `double` for money, skipping tests) conflicts with the conventions above, rather than silently complying.
- Keep each service isolated — no shared database, no direct DB access across service boundaries. Cross-service communication only via gRPC or Kafka.
- When a task is finished, note it so the Notion board can be updated to reflect status.
