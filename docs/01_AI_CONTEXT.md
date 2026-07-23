# AI Context

This file provides the minimum authoritative context required for an AI
assistant to continue development of Cinema Booking System without relying
on previous chat history.

The `docs` directory is the project's source of truth.

---

# Current Progress

## Completed

- R1–R19 — Common modules
- R20 — Config Server
- R21 — Discovery Server
- R22 — API Gateway
- R23 — Movie Service functional implementation

## Current Round

> **R23 — Movie Service Stabilization**

Movie Service functionality has been implemented, but R23 must not be
marked complete until stabilization is finished.

Remaining work:

1. Remove all hard-coded database credentials.
2. Complete Movie Service unit tests.
3. Complete controller tests.
4. Complete mapper and utility tests.
5. Add MySQL Testcontainers integration tests.
6. Verify Flyway migrations.
7. Run `mvn clean verify`.
8. Pass security scanning.
9. Synchronize project documentation.

## Next Round

> **R24 — User Service**

Do not start R24 before R23 stabilization has passed all required checks.

---

# Locked Architecture

Do not modify the following unless the user explicitly requests it:

- Module structure
- Technology stack
- Package naming
- Database ownership
- Event contracts
- Architecture patterns
- Coding conventions
- Dependency rules
- Round scope

Do not introduce new technologies, patterns, modules, or infrastructure
without explicit approval.

---

# Technology Stack

- Java 21
- Spring Boot 3.5.4
- Maven Multi Module
- Spring Data JPA
- Hibernate 6
- MySQL 8
- Flyway
- Redis
- Redisson
- Apache Kafka
- MapStruct
- Jackson
- Spring Security
- JWT and OAuth2
- OpenAPI and Swagger
- JUnit 5
- Mockito
- Testcontainers
- Docker Compose
- Spring Cloud Config
- Eureka Discovery
- Spring Cloud Gateway
- Micrometer Tracing
- OpenTelemetry
- Elasticsearch
- MinIO

Do not change technology versions or replace technologies unless
explicitly requested.

---

# Architecture Decisions

The project uses:

- Microservices Architecture
- Event-Driven Architecture
- Saga Pattern using Choreography
- Transactional Outbox Pattern
- Idempotent Consumer Pattern
- Database per Service
- Eventual Consistency
- Distributed Lock
- UUID Version 7
- Standard `ApiResponse`
- `BusinessException` as the base business exception
- MapStruct for object mapping
- Jackson ISO-8601 date/time serialization
- No Lombok in common modules
- Flyway-managed database schemas

---

# Service Ownership Rule

Each microservice exclusively owns its domain database.

A service must not:

- Connect to another service's database
- Query another service's tables
- Modify another service's tables
- Import or reuse another service's repository
- Create physical foreign keys across service databases

Cross-service communication must use:

- APIs for synchronous requests
- Kafka events for asynchronous workflows
- Transactional Outbox for reliable publication
- Idempotent Consumer for reliable consumption

Service ownership:

| Service              | Owned data                                     |
| -------------------- | ---------------------------------------------- |
| Movie Service        | Movies, genres and movie metadata              |
| User Service         | Users, roles, permissions and refresh tokens   |
| Inventory Service    | Show-seat availability and reservation state   |
| Booking Service      | Booking lifecycle and requested seat snapshots |
| Payment Service      | Payment transactions                           |
| Notification Service | Notifications and delivery history             |

---

# Seat Inventory Ownership

Inventory Service exclusively owns:

- `show_seats`
- Seat availability state
- Seat reservation state
- Seat release operations
- Redis distributed locks for seats
- Inventory reservation result events

Booking Service must not:

- Query `show_seats`
- Update `show_seats`
- Use `ShowSeatRepository`
- Use Inventory Service entities
- Connect to `cinema_inventory_db`
- Acquire Redis seat locks
- Create a physical foreign key to `show_seats`

Booking Service may store an immutable seat snapshot for its own booking
record.

Possible snapshot fields:

```text
booking_id
inventory_seat_id
showtime_id
seat_number
seat_type
price
```

`inventory_seat_id` is an external reference, not a cross-database foreign
key.

---

# Standard Seat Reservation Flow

The following is the authoritative seat reservation flow:

```mermaid
sequenceDiagram
    participant Client
    participant Booking as Booking Service
    participant Kafka
    participant Inventory as Inventory Service

    Client->>Booking: Create booking
    Booking->>Booking: Save PENDING booking
    Booking->>Booking: Save seat snapshot
    Booking->>Booking: Save outbox event
    Booking-->>Client: Booking accepted

    Booking->>Kafka: seat-reservation-requested
    Kafka->>Inventory: Consume reservation request

    Inventory->>Inventory: Check idempotency
    Inventory->>Inventory: Acquire Redis lock
    Inventory->>Inventory: Validate show_seats

    alt Seats available
        Inventory->>Inventory: Set seats RESERVED
        Inventory->>Kafka: seat-reserved
        Kafka->>Booking: Consume success
        Booking->>Booking: Set booking RESERVED
        Booking->>Kafka: payment-requested
    else Seats unavailable
        Inventory->>Kafka: seat-reservation-rejected
        Kafka->>Booking: Consume rejection
        Booking->>Booking: Set booking REJECTED
    end
```

The old flow in which Booking Service directly updates `show_seats` is
invalid and must not be reintroduced.

---

# Booking Service Responsibilities

When creating a booking, the Booking Service local transaction performs:

1. Create the booking with status `PENDING`.
2. Store the requested seat snapshot.
3. Create a `SEAT_RESERVATION_REQUESTED` outbox event.
4. Commit the Booking database transaction.

Booking Service publishes:

```text
seat-reservation-requested
```

Suggested event contract:

```java
public record SeatReservationRequestedEvent(
        UUID eventId,
        UUID bookingId,
        UUID userId,
        UUID showtimeId,
        List<String> seatNumbers,
        OffsetDateTime occurredAt
) {
}
```

Booking Service does not validate seat availability against the Inventory
database.

---

# Inventory Service Responsibilities

When Inventory Service receives a reservation request, it performs:

1. Check idempotency using `eventId`.
2. Acquire Redis distributed locks.
3. Query Inventory-owned `show_seats`.
4. Verify that all requested seats are `AVAILABLE`.
5. Change available seats to `RESERVED`.
6. Store the result event in its outbox table.
7. Commit the Inventory database transaction.
8. Release the Redis locks.

Success topic:

```text
seat-reserved
```

Suggested success event:

```java
public record SeatReservedEvent(
        UUID eventId,
        UUID correlationId,
        UUID bookingId,
        UUID showtimeId,
        List<String> seatNumbers,
        OffsetDateTime reservedAt,
        OffsetDateTime expiresAt
) {
}
```

Rejection topic:

```text
seat-reservation-rejected
```

Suggested rejection event:

```java
public record SeatReservationRejectedEvent(
        UUID eventId,
        UUID correlationId,
        UUID bookingId,
        UUID showtimeId,
        List<String> seatNumbers,
        String reason,
        OffsetDateTime occurredAt
) {
}
```

---

# Booking Result Handling

When Booking Service consumes `seat-reserved`:

```text
PENDING → RESERVED
```

Booking Service then publishes:

```text
payment-requested
```

When Booking Service consumes `seat-reservation-rejected`:

```text
PENDING → REJECTED
```

Booking Service must not create a payment request after seat reservation is
rejected.

All consumers must implement idempotent processing.

---

# Seat Release Flow

A booking expiration, cancellation, or payment failure triggers:

```text
Booking Service
    ↓
seat-release-requested
    ↓
Inventory Service
    ↓
show_seats: RESERVED → AVAILABLE
    ↓
seat-released
```

Inventory Service remains the only service allowed to change the state of
`show_seats`.

---

# Security and Credential Rules

Never hard-code:

- Database passwords
- API keys
- JWT secrets
- Access tokens
- Private keys
- Production usernames

Use environment variables:

```yaml
spring:
    datasource:
        username: ${MOVIE_DB_USERNAME}
        password: ${MOVIE_DB_PASSWORD}
```

Password environment variables must not have real default values.

Allowed example:

```yaml
username: ${MOVIE_DB_USERNAME:cinema_movie}
password: ${MOVIE_DB_PASSWORD}
```

Not allowed:

```yaml
username: root
password: root
```

Test rules:

- Prefer Testcontainers-generated database credentials.
- Do not store test passwords in `application-test.yml`.
- Use `@DynamicPropertySource` when datasource properties must be
  registered explicitly.
- Never commit a real `.env` file.
- Keep only placeholder values in `.env.example`.
- Rotate credentials that have previously appeared in Git history.

---

# Common Layer Status

| Module            | Status |
| ----------------- | ------ |
| common-core       | DONE   |
| common-jpa        | DONE   |
| common-exception  | DONE   |
| common-response   | DONE   |
| common-api        | DONE   |
| common-validation | DONE   |
| common-jackson    | DONE   |
| common-logging    | DONE   |
| common-mapper     | DONE   |
| common-security   | DONE   |
| common-lock       | DONE   |
| common-kafka      | DONE   |
| common-outbox     | DONE   |
| common-search     | DONE   |
| common-openapi    | DONE   |
| common-test       | DONE   |
| common-tracing    | DONE   |
| common-storage    | DONE   |

Common modules R1–R19 are complete. Do not refactor completed common
modules unless required by a verified defect or explicitly requested.

---

# Infrastructure Status

| Round | Module           | Status |
| ----- | ---------------- | ------ |
| R20   | Config Server    | DONE   |
| R21   | Discovery Server | DONE   |
| R22   | API Gateway      | DONE   |

Do not rebuild or redesign completed infrastructure modules unless
explicitly requested.

---

# Business Service Status

| Round | Service              | Status        |
| ----- | -------------------- | ------------- |
| R23   | Movie Service        | STABILIZATION |
| R24   | User Service         | NOT STARTED   |
| R25   | Inventory Service    | NOT STARTED   |
| R26   | Booking Service      | NOT STARTED   |
| R27   | Payment Service      | NOT STARTED   |
| R28   | Notification Service | NOT STARTED   |

Movie Service functional code has been implemented and merged, but R23
remains open until testing and verification are complete.

---

# Coding Conventions

## Common Modules

Do not use Lombok in common modules.

Use explicit:

- Constructors
- Getters
- `equals`
- `hashCode`
- `toString` where necessary

## Logging

Use SLF4J:

```java
private static final Logger LOGGER =
        LoggerFactory.getLogger(CurrentClass.class);
```

Do not depend on Lombok-generated `log` fields in common modules.

## Mapping

Use MapStruct.

Do not manually map DTOs and entities when a MapStruct mapper is
appropriate.

## Exceptions

All business exceptions must extend the approved `BusinessException`
base.

Do not create independent exception response formats.

## API Responses

Controllers must return the standardized `ApiResponse`.

Do not create service-specific response wrappers.

## Jackson

Use the shared Jackson configuration.

Do not create `ObjectMapper` directly inside application code.

Date and time values must use ISO-8601 serialization.

## Identifiers

Use UUID v7 for:

- Entity identifiers
- Event identifiers
- Correlation identifiers where applicable

Do not return to numeric auto-increment identifiers.

## Database Migrations

Use Flyway for every schema change.

Do not rely on Hibernate to create or update production schemas.

Use:

```yaml
spring:
    jpa:
        hibernate:
            ddl-auto: validate
```

---

# Testing Requirements

Every round must include appropriate tests.

For a business service, verify at least:

- Service unit tests
- Mapper tests
- Utility tests
- Controller tests
- Repository or integration tests
- Flyway migration startup
- Testcontainers integration
- Validation behavior
- Business exception behavior
- Duplicate constraint behavior
- Full Maven verification

R23 Movie Service should include:

- `MovieServiceImplTest`
- `GenreServiceImplTest`
- `MovieControllerTest`
- `GenreControllerTest`
- `MovieMapperTest`
- `GenreMapperTest` when applicable
- `SlugUtilsTest`
- MySQL Testcontainers integration tests

Important Movie Service cases:

- Create movie successfully
- Reject duplicate movie title or slug
- Get movie by ID
- Update movie
- Delete movie
- Create genre successfully
- Reject duplicate genre name
- Delete unused genre
- Reject deletion of a genre currently used by a movie
- Validate Flyway schema against Hibernate mappings

---

# Completion Rules

A round can be marked complete only when:

1. The requested functionality is implemented.
2. Unit tests pass.
3. Integration tests pass where applicable.
4. Flyway migrations pass.
5. Maven build passes.
6. Security checks pass.
7. Documentation is synchronized.
8. No ownership boundary is violated.

A merged pull request alone does not mean the round is complete.

---

# Current Next Step

Continue R23 Movie Service Stabilization in this order:

1. Remove hard-coded credentials.
2. Verify no additional secrets remain.
3. Add missing service tests.
4. Add mapper and utility tests.
5. Add controller tests.
6. Add MySQL Testcontainers integration tests.
7. Run `mvn clean verify`.
8. Update R23 status in the roadmap and changelog.
9. Start R24 User Service only after all R23 checks pass.
