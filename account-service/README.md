# account-service

Manages customers, accounts and balances for the Core Banking Platform.
Spring Boot 4.1 · Java 21 · PostgreSQL 17 · Liquibase

## Prerequisites

- Java 21
- Docker Desktop (running)

No local Maven install is needed — use the bundled `./mvnw` wrapper.

## Run locally

**1. Start Postgres** (from the repo root, `banking-platform/`):

```bash
docker compose up -d
docker compose ps        # STATUS should be "Up", PORTS 0.0.0.0:5433->5432/tcp
```

**2. Start the app** (from `account-service/`):

```bash
./mvnw spring-boot:run
```

Or run `AccountServiceApplication` from IntelliJ.

The app starts on **http://localhost:4100**. On startup Liquibase applies any
pending migrations from `src/main/resources/db/changelog/`.

## Database

| Setting  | Value                 |
|----------|-----------------------|
| Host     | `localhost`           |
| Port     | `5433` (host) → `5432` (container) |
| Database | `account_postgres_db` |
| User     | `admin`               |
| Password | `password` (local dev only) |

JDBC URL: `jdbc:postgresql://localhost:5433/account_postgres_db`

Port `5433` is used on the host to avoid clashing with any other Postgres on
the default `5432`.

## Migrations (Liquibase)

- Master changelog: `src/main/resources/db/changelog/db.changelog-master.xml`
- Individual changes: `src/main/resources/db/changelog/changes/NNN-description.xml`
- To change the schema, **add a new file** (e.g. `002-create-accounts.xml`) and
  `<include>` it in the master changelog.
- **Never edit a changeSet that has already run** — Liquibase checksums each one
  and startup will fail.
- Hibernate does not manage the schema (`ddl-auto=none`); Liquibase is the only
  source of schema changes.

Applied migrations are recorded in the `databasechangelog` table.

## Stopping / resetting

```bash
docker compose down      # stop containers, keep data
docker compose down -v   # stop and DELETE all database data
```

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `./mvnw: no such file or directory` | You're in the repo root — `cd account-service` first |
| `Failed to configure a DataSource` | `spring.datasource.*` missing in `application.properties` |
| `password authentication failed` | Connecting to a different Postgres — check you're on port `5433` |
| `database "..." does not exist` | DB name doesn't match `POSTGRES_DB` in `docker-compose.yaml` |
| Container stuck `Restarting` | Check `docker compose logs account-service-db` |
| `no changelog could be found` | `spring.liquibase.change-log` path doesn't match the actual file |
