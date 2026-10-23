# Toghrak X: backend

Spring Boot 3.5 (Java 21) REST API for Toghrak X. See the [main README](../README.md) for the whole project, the configuration variables and deployment.

## Run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API listens on `http://localhost:8081/api`. The database settings, `JWT_SECRET` and the other variables are listed in the main README.

## Database migrations

The schema is managed by Flyway (`src/main/resources/db/migration`) and applied on startup. Hibernate runs with `ddl-auto=validate`: it checks the schema but never changes it. To change the schema, add a new migration `V<n>__description.sql`; never edit one that has already run.

## Tests

```bash
./mvnw test
```

Integration tests use Testcontainers to start a throwaway PostgreSQL in Docker; they are skipped when Docker is not available.

## Layout

| Package | Contents |
|---|---|
| `controller` | REST endpoints; role checks with `@PreAuthorize` |
| `service` | Business rules and transactions |
| `repository` | Spring Data JPA repositories |
| `model` | JPA entities |
| `dto` | Request and response objects |
| `security`, `config`, `filter` | JWT, Spring Security, rate limiting |
| `exception` | Error handling (RFC 9457 `ProblemDetail`) |
| `db.migration` (in `src/main/java`) | Flyway Java migrations |
