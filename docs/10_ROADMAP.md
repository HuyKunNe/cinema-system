# Project Roadmap

**Version:** R24
**Current target:** R24 Inventory Service implementation
**Last updated:** 2026-07-24

---

# Purpose

This roadmap defines the approved implementation order for the Cinema Booking
System.

It is the source of truth for:

- round status;
- round scope;
- dependencies between rounds;
- completion criteria;
- verification requirements;
- the boundary between implemented and planned capabilities.

The roadmap does not mark a capability as complete merely because:

- a Maven module exists;
- an interface or placeholder class exists;
- code compiles in isolation;
- documentation describes the intended behavior;
- a happy-path request succeeds manually.

A round is complete only after its exit criteria and the shared quality gates are
satisfied.

---

# Status Legend

| Symbol | Status | Meaning |
|---|---|---|
| ✅ | Completed | Scope is implemented and accepted for the completed round |
| 🚧 | Implementation | The round is the active implementation target |
| 🧪 | Stabilization | Main implementation exists, but one or more exit criteria remain |
| ⏳ | Planned | Approved future work; implementation has not started |
| ⛔ | Blocked | Work cannot continue until a named dependency or decision is resolved |

Only one business-service round should be the active implementation target at a
time unless the roadmap is explicitly revised.

---

# Fixed Technical Baseline

All rounds must preserve the approved project baseline:

- Java 21
- Spring Boot 3.5.16
- Maven Multi Module
- Spring Data JPA
- Hibernate 6
- MySQL 8
- Flyway
- Redis
- Redisson
- Kafka
- Spring Cloud Gateway
- Spring Security
- JWT / OAuth2 Resource Server
- OpenAPI
- MapStruct
- Jackson with ISO-8601 timestamps
- JUnit 5
- Testcontainers
- Docker Compose
- Micrometer Tracing
- OpenTelemetry
- Elasticsearch

Architecture and implementation must preserve:

- database per service;
- service ownership of its domain data;
- Inventory Service ownership of `show_seats`;
- Event-Driven Architecture;
- Saga Choreography;
- Transactional Outbox;
- Idempotent Consumer;
- UUID v7 for entity and event identifiers;
- no Lombok in common modules;
- shared response, exception, validation, Jackson, logging, mapping, security,
  Kafka and tracing conventions;
- no committed production secrets.

Changing this baseline requires an accepted Architecture Decision Record before
implementation.

---

# Global Round Lifecycle

Each round follows this lifecycle:

```text
Scope confirmed
    ↓
Dependencies verified
    ↓
Implementation
    ↓
Unit tests
    ↓
Integration tests where required
    ↓
Security and architecture review
    ↓
Full Maven verification
    ↓
Documentation synchronization
    ↓
Round completed
```

Starting the next round does not convert an unfinished round into a completed
round.

---

# Shared Definition of Done

Every implementation round must satisfy all applicable requirements below.

## Build

- The module builds using the parent Maven configuration.
- Dependency versions are managed centrally.
- No undeclared or duplicate version is introduced without justification.
- `mvn clean verify` passes from the repository root.
- `git diff --check` passes.

## Code

- Package and module boundaries follow `docs/04_MODULES.md`.
- Dependency direction follows `docs/12_DEPENDENCY_RULES.md`.
- External input uses operation-specific DTOs.
- Bean Validation and business validation are both applied where required.
- JPA entities are not exposed as API or event contracts.
- Money uses `BigDecimal`.
- Time values and expiration decisions use trusted server time.
- Public errors use the shared exception and response model.
- Logs do not expose credentials, tokens or sensitive payloads.

## Data

- The service owns its database and Flyway migrations.
- No service reads or writes another service's tables.
- Schema changes are forward migrations; applied migrations are not silently rewritten.
- Constraints and indexes support the business invariants.
- Entity, schema and repository mappings agree.

## Security

- Authentication and authorization match `docs/08_SECURITY.md`.
- Ownership is derived from the validated principal, not an arbitrary request field.
- Secrets are supplied through approved external configuration.
- CORS, CSRF, Actuator and OpenAPI exposure are environment appropriate.
- Security tests cover applicable 401, 403, ownership and administrative cases.

## Events

- Event names, producers, consumers and payloads match `docs/07_EVENT_CATALOG.md`.
- Business state and Outbox records are written in the same local transaction.
- Published events contain `eventId`, event type, version, aggregate identity,
  occurrence time and correlation information as defined by the event contract.
- Consumers validate contracts and process duplicate `eventId` values idempotently.
- At-least-once delivery is assumed; end-to-end exactly-once delivery is not claimed.

## Tests

- Unit tests cover business rules and failure branches.
- Controller security behavior is tested.
- Persistence behavior uses integration tests where database semantics matter.
- Kafka, Redis and MySQL integration uses Testcontainers where applicable.
- Regression tests accompany bug fixes.

## Documentation

The following files are updated when affected:

- `README.md`
- `docs/00_PROJECT_CONTEXT.md`
- `docs/01_AI_CONTEXT.md`
- `docs/02_ARCHITECTURE.md`
- `docs/03_TECHNOLOGY_STACK.md`
- `docs/04_MODULES.md`
- `docs/05_CODING_CONVENTIONS.md`
- `docs/06_DATABASE_DESIGN.md`
- `docs/07_EVENT_CATALOG.md`
- `docs/08_SECURITY.md`
- `docs/09_OUTBOX.md`
- `docs/10_ROADMAP.md`
- `docs/11_CHANGELOG.md`
- `docs/12_DEPENDENCY_RULES.md`
- `docs/13_SEQUENCE_DIAGRAMS.md`
- `docs/14_DEPLOYMENT.md`
- `docs/decisions/`

Documentation must distinguish current implementation from future requirements.

---

# Phase 1 — Foundation Layer

## ✅ R1 — Parent Project

### Scope

- root Maven aggregator;
- module hierarchy;
- Java 21 compiler configuration;
- Spring Boot 3.5.16 dependency management;
- shared plugin and dependency versions;
- repository-wide build conventions.

### Outcome

All project modules inherit one consistent build baseline.

## ✅ R2 — common-core

### Scope

- core constants and shared primitives;
- UUID v7 support;
- framework-independent foundational utilities.

### Outcome

Domain-neutral primitives are available without creating service coupling.

## ✅ R3 — common-jpa

### Scope

- base entity conventions;
- UUID identifier integration;
- created and updated timestamp auditing;
- shared JPA auditing configuration.

### Outcome

Persistence modules use one auditable entity baseline.

## ✅ R4 — common-exception

### Scope

- business exception hierarchy;
- shared error-code contract;
- standard exception categories.

### Outcome

Services express failures without leaking internal implementation details.

## ✅ R5 — common-response

### Scope

- `ApiResponse`;
- standardized error body;
- validation error model;
- pagination response and mapping.

### Outcome

Synchronous APIs share one public response envelope.

## ✅ R6 — common-api

### Scope

- global exception handling;
- exception-to-status mapping;
- response construction integration;
- shared API constants and web-layer support.

### Outcome

Services apply the common exception and response contracts consistently.

## ✅ R7 — common-validation

### Scope

- reusable Bean Validation annotations and validators;
- shared validation messages;
- validation test support.

### Outcome

Common structural validation is reusable while business validation remains service-owned.

## ✅ R8 — common-jackson

### Scope

- approved shared `ObjectMapper` configuration;
- Java Time support;
- ISO-8601 timestamp serialization;
- JSON conversion utilities.

### Outcome

HTTP, persistence payloads and messaging use consistent JSON behavior.

## ✅ R9 — common-logging

### Scope

- logging aspect;
- correlation context support;
- consistent parameterized logging conventions.

### Outcome

Service logs are traceable without logging protected values.

## ✅ R10 — common-mapper

### Scope

- MapStruct baseline;
- shared mapper configuration;
- mapper tests.

### Outcome

DTO and domain mapping is compile-time generated and consistent.

---

# Phase 2 — Common Infrastructure

## ✅ R11 — common-security

### Scope

- shared security configuration;
- authenticated principal abstraction;
- JWT claim and token support;
- security context utilities;
- authorization helpers.

### Outcome

Services share security mechanics while retaining ownership of business authorization.

Security hardening continues to be governed by `docs/08_SECURITY.md`; completion
of R11 does not mean every future service endpoint is already secured.

## ✅ R12 — common-lock

### Scope

- distributed-lock abstraction;
- Redisson implementation;
- bounded wait and lease behavior;
- lock release safety.

### Outcome

Business services can coordinate selected concurrent operations across instances.

Distributed locks do not replace database constraints, atomic state transitions
or idempotency.

## ✅ R13 — common-kafka

### Scope

- shared producer and consumer configuration;
- event envelope and serialization conventions;
- topic integration support;
- common Kafka abstraction.

### Outcome

Services can publish and consume explicit, versioned contracts consistently.

## ✅ R14 — common-outbox

### Scope

- Transactional Outbox entity and repository;
- outbox creation service;
- scheduled publisher;
- Kafka acknowledgement handling;
- retry state and status transitions.

### Current implemented baseline

```text
PENDING → PROCESSING → SENT
                     ↘ FAILED
```

Known hardening gaps are tracked in `docs/09_OUTBOX.md`. R14 completion records
the accepted implementation round; it does not falsely claim that leasing,
multi-instance claiming, backoff, DLT, cleanup or manual replay controls already exist.

## ✅ R15 — common-search

### Scope

- Elasticsearch abstraction;
- search client integration;
- full-text search support;
- paging and approved sorting;
- indexing conventions.

### Outcome

Searchable services can integrate without coupling domain contracts to the search engine.

## ✅ R16 — common-openapi

### Scope

- shared OpenAPI metadata;
- bearer authentication scheme;
- service-specific documentation properties;
- environment-controlled Swagger integration.

### Outcome

API documentation follows one shared baseline.

## ✅ R17 — common-test

### Scope

- JUnit 5 shared configuration;
- unit and integration test annotations;
- reusable MySQL, Kafka and Redis Testcontainers support;
- JSON testing utilities;
- integration-test base classes.

### Outcome

Service rounds can test real infrastructure behavior consistently.

## ✅ R18 — common-tracing

### Scope

- Micrometer Tracing integration;
- OpenTelemetry bridge;
- OTLP exporter support;
- trace and span context access.

### Outcome

HTTP and messaging flows can propagate approved trace context.

## ✅ R19 — common-storage

### Scope

- file-storage abstraction;
- storage metadata contract;
- upload and download integration boundary;
- validation and provider-independent behavior.

### Outcome

Services can use controlled storage without exposing provider credentials or
trusting client filenames as object paths.

---

# Phase 3 — Infrastructure Services

## ✅ R20 — Config Service

### Scope

- centralized externalized configuration;
- service-specific configuration repository;
- local and Git-backed configuration modes.

### Outcome

Services can retrieve shared and environment-specific configuration.

Production secret handling and authenticated encrypted transport remain deployment requirements.

## ✅ R21 — Discovery Service

### Scope

- service registration;
- service discovery;
- health-aware instance lookup.

### Outcome

Infrastructure and business services can discover approved instances.

Discovery registration is not caller authentication.

## ✅ R22 — API Gateway

### Scope

- central route definitions;
- service discovery routing;
- request correlation handling;
- gateway error handling;
- edge security integration.

### Outcome

External API traffic enters through one controlled routing boundary.

Business services must still validate authentication and authorization; the
Gateway is not the sole security boundary.

---

# Phase 4 — Business Services

## ✅ R23 — Movie Service

Movie Service owns:

- movies;
- genres;
- movie lifecycle state;
- movie metadata;
- its MySQL schema and Flyway migrations.

### Completed scope

- Movie Service Spring Boot application module;
- movie and genre JPA entities;
- repositories;
- operation-specific create and update request DTOs;
- movie and genre response DTOs;
- MapStruct mappers;
- movie and genre service interfaces and implementations;
- Movie and Genre REST controllers;
- Bean Validation and business validation;
- shared response and exception integration;
- invalid request-body and invalid UUID handling;
- Movie Service error codes;
- slug generation and duplicate handling;
- pagination and filtering support;
- Flyway schema migration;
- Flyway default genre seed migration;
- JPA auditing;
- Config Server and Eureka integration;
- OpenAPI and Swagger documentation;
- manual Swagger verification of success and failure cases.

### Architectural boundary

Movie Service owns only catalog data. It does not own cinemas, rooms, seats,
showtimes or `show_seats`.

Inventory Service stores `movie_id` only as an external UUID reference. No
cross-service database foreign key is permitted.

### Outcome

Movie Service is the authoritative owner of movie and genre catalog data.

---

## 🚧 R24 — Inventory Service

Inventory Service is the active implementation target.

Inventory Service owns:

- cinemas;
- rooms or auditoriums;
- fixed physical seats;
- room seat layouts;
- showtimes;
- `show_seats`;
- seat availability and reservation state;
- Inventory Outbox records;
- Inventory consumer-processing records where required.

### Approved domain model

```text
Cinema
    ↓
Room
    ↓
Seat
    ↓
Showtime
    ↓
ShowSeat
```

The relationship is interpreted as follows:

- A Cinema contains one or more Rooms.
- A Room owns a fixed physical seat layout.
- A Seat is a permanent physical position inside a Room.
- A Showtime schedules one Movie in one Room for a time range.
- A ShowSeat represents one physical Seat for one specific Showtime.
- Booking changes reservation state through Inventory commands or events.
- Booking Service never updates `show_seats` directly.

### Data rules

Physical seat layouts must not be hard-coded in Java source.

Initial development data may be created through explicit Flyway seed migrations.
Production seat layouts must be managed through approved administrative operations.

`showtimes.movie_id` is an external Movie Service UUID reference.

The Inventory database must not define a foreign key to the Movie Service database.

A Showtime must use a Room owned by the same Inventory Service database.

Creating a Showtime generates exactly one ShowSeat for every active Seat in its Room.

A ShowSeat must retain the physical seat identity required for display and
reservation even when the original Seat is later deactivated.

### Approved initial entities

#### Cinema

- `id` — UUID v7
- `name`
- `code`
- `address`
- `city`
- `status`
- audit fields

#### Room

- `id` — UUID v7
- `cinema_id`
- `name`
- `code`
- `room_type`
- `status`
- audit fields

#### Seat

- `id` — UUID v7
- `room_id`
- `row_label`
- `seat_number`
- `seat_type`
- `status`
- audit fields

A unique constraint must prevent duplicate seat positions inside the same Room.

```text
UNIQUE(room_id, row_label, seat_number)
```

#### Showtime

- `id` — UUID v7
- `movie_id` — external Movie Service reference
- `room_id`
- `start_time`
- `end_time`
- `status`
- audit fields

Showtime creation must reject invalid time ranges and overlapping active showtimes
in the same Room.

#### ShowSeat

- `id` — UUID v7
- `showtime_id`
- `seat_id`
- `seat_label`
- `seat_type`
- `status`
- optional hold metadata where required by the approved design
- audit fields

Recommended unique constraints:

```text
UNIQUE(showtime_id, seat_id)
UNIQUE(showtime_id, seat_label)
```

### Approved status models

#### CinemaStatus

```text
ACTIVE
INACTIVE
```

#### RoomStatus

```text
ACTIVE
INACTIVE
MAINTENANCE
```

#### SeatStatus

```text
ACTIVE
INACTIVE
MAINTENANCE
```

#### ShowtimeStatus

```text
SCHEDULED
OPEN
CLOSED
CANCELLED
COMPLETED
```

#### ShowSeatStatus

```text
AVAILABLE
HELD
BOOKED
UNAVAILABLE
```

`HELD` is the temporary business reservation state.

Redis locks are a technical coordination mechanism and must not appear as a
business status such as `LOCKED`.

### Active implementation scope

1. Inventory Service Maven module.
2. Spring Boot application entry point.
3. Config Server and Discovery integration.
4. Inventory database configuration.
5. Initial Flyway schema.
6. Cinema entity, repository, DTOs, mapper, service and controller.
7. Room entity, repository, DTOs, mapper, service and controller.
8. Seat entity, repository, DTOs, mapper, service and controller.
9. Fixed seat-layout creation and validation.
10. Showtime entity, repository, DTOs, mapper, service and controller.
11. ShowSeat entity, repository and generation service.
12. Atomic ShowSeat state transitions.
13. Shared response and exception integration.
14. Bean Validation and business validation.
15. JPA auditing.
16. OpenAPI documentation.
17. Unit tests.
18. Persistence and Flyway integration tests.
19. Concurrency tests where database state transitions matter.
20. Documentation synchronization.

### Required business rules

- Cinema code is unique.
- Room code is unique within its Cinema.
- Seat position is unique within its Room.
- A Room cannot be removed while active Showtimes depend on it.
- A Seat layout cannot be changed in a way that corrupts existing ShowSeats.
- Showtime start time must be earlier than end time.
- Active Showtimes in one Room cannot overlap.
- A Showtime cannot be created for an inactive Room.
- ShowSeat generation is idempotent.
- One active Seat produces exactly one ShowSeat.
- ShowSeat state transitions use the current persisted state.
- Booking Service cannot directly persist or update Inventory entities.
- Cross-service requests must not trust arbitrary user ownership fields.
- Administrative corrections require authorization and audit coverage.

### Approved ShowSeat transition baseline

```text
AVAILABLE   → HELD
HELD        → AVAILABLE
HELD        → BOOKED
AVAILABLE   → UNAVAILABLE
HELD        → UNAVAILABLE
UNAVAILABLE → AVAILABLE
```

Invalid examples include:

```text
BOOKED    → AVAILABLE
BOOKED    → HELD
AVAILABLE → BOOKED
```

Any exception to these transitions requires an explicit business use case and
documentation update.

### Concurrency requirements

Inventory must not rely on Redis locks alone.

Correctness requires:

- database constraints;
- transactional state checks;
- atomic conditional updates or equivalent optimistic/pessimistic control;
- bounded Redis lock usage only where it improves coordination;
- idempotent command or event processing;
- safe handling of duplicate and delayed messages.

The final seat reservation operation must succeed for at most one competing
request when only one saleable seat remains.

### R24 exit criteria

R24 may be marked complete only when:

- Cinema, Room, Seat, Showtime and ShowSeat schemas and JPA mappings agree;
- all identifiers use UUID v7;
- a Room can own a fixed, validated seat layout;
- duplicate seat positions are rejected;
- invalid and overlapping showtimes are rejected;
- creating a Showtime generates exactly one ShowSeat per active Seat;
- repeated generation does not create duplicate ShowSeats;
- only approved ShowSeat transitions are possible;
- Inventory Service is the only service that modifies `show_seats`;
- no Inventory code reads or writes another service's database;
- `movie_id` has no cross-service database foreign key;
- API validation and public errors follow shared contracts;
- applicable 401, 403 and administrative authorization tests pass;
- persistence behavior is verified with MySQL Testcontainers;
- Flyway initializes a clean MySQL container successfully;
- concurrency tests verify competing seat operations;
- root `mvn clean verify` passes;
- `git diff --check` passes;
- README, architecture, modules, database, event, security, sequence, deployment,
  roadmap and changelog documents agree on R24 status.

---

## ⏳ R25 — User Service

User Service will own:

- user accounts;
- user profiles;
- credentials when local authentication is used;
- roles and account status;
- verification and password-recovery state;
- refresh-token or session state according to the accepted authentication design.

### Planned scope

- account registration;
- authentication integration;
- password hashing;
- account verification;
- profile management;
- role and permission enforcement;
- refresh rotation and revocation where applicable;
- administrative account actions;
- audit coverage for privileged changes.

### Required decisions before implementation

- authoritative token issuer;
- OAuth2 or authorization-server topology;
- access and refresh token model;
- issuer, audience and signing-key ownership;
- MFA scope for privileged accounts.

R25 must satisfy the token and authorization test cases in `docs/08_SECURITY.md`.

---

## ⏳ R26 — Booking Service

Booking Service will own:

- bookings;
- booking items or booking-seat references;
- booking lifecycle state;
- booking expiration;
- Booking Outbox records;
- Booking consumer-processing records.

### Planned scope

- authenticated booking creation;
- ownership enforcement;
- seat-count and request validation;
- client request idempotency;
- coordination with Inventory Service;
- reservation expiration;
- Transactional Outbox publication;
- Saga state transitions;
- idempotent handling of Inventory and Payment events;
- cancellation behavior;
- booking query APIs.

Booking Service must not:

- accept an arbitrary request `userId` as ownership;
- read or update `show_seats`;
- receive Payment provider credentials;
- mark a Booking confirmed without validated current state.

---

## ⏳ R27 — Payment Service

Payment Service will own:

- payment attempts;
- provider references;
- payment state;
- refund state;
- webhook processing evidence;
- Payment Outbox records;
- Payment consumer-processing records.

### Planned scope

- payment initiation;
- provider token or hosted-payment integration;
- provider idempotency;
- authenticated webhook verification;
- duplicate callback protection;
- payment success and failure events;
- refund and reconciliation controls;
- idempotent Booking event consumption;
- audit coverage for financial administration.

Payment Service must never store CVV or publish card credentials in events.

R27 completes the principal Booking–Payment Saga path when its integration tests
verify success, failure, timeout, duplicate event and delayed event scenarios.

---

## ⏳ R28 — Notification Service

Notification Service will own:

- notification requests;
- delivery attempts;
- template references;
- approved contact snapshots;
- delivery state;
- consumer-processing records.

### Planned scope

- idempotent consumption of approved business events;
- email and other approved delivery channels;
- retry and bounded failure handling;
- template rendering with untrusted-data protection;
- delivery observability;
- retention and personal-data minimization.

Notification failure must not silently change authoritative Booking or Payment state.

---

# Cross-Service Integration Milestones

These milestones do not replace round exit criteria.

## Milestone A — Catalog Ready

### Requires

- R23 complete.

### Result

Clients can query and administer movie and genre data according to authorization policy.

## Milestone B — Seat Inventory Ready

### Requires

- R24 complete.

### Result

Cinemas, rooms, showtimes and seats have one authoritative owner and atomic
reservation behavior.

## Milestone C — Identity Ready

### Requires

- R25 complete.

### Result

End-user and administrative identities can be authenticated and authorized.

## Milestone D — Booking Saga Ready

### Requires

- R26 and R27 complete;
- Event Catalog contracts verified;
- Transactional Outbox and Idempotent Consumer integration tests passing.

### Result

A booking can reserve inventory, complete or fail payment, and converge to the
correct state without cross-database transactions.

## Milestone E — Customer Communication Ready

### Requires

- R28 complete.

### Result

Approved business outcomes trigger idempotent notifications.

---

# Phase 5 — Production Readiness

Production-readiness work begins after the business-service foundations are
implemented. It must be divided into explicitly scoped rounds before code or
infrastructure changes begin.

Approved capability areas:

- CI/CD
- Kubernetes
- Monitoring
- Metrics
- Prometheus
- Grafana
- OpenTelemetry deployment
- Performance testing
- Chaos testing

These entries are planned capability areas, not implemented features and not
authorization to add technologies outside `docs/03_TECHNOLOGY_STACK.md`.

Before this phase starts, define:

- round numbers and owners;
- environment topology;
- secret-management platform;
- TLS and service-identity model;
- Kafka, Redis and database security model;
- backup and recovery objectives;
- SLI, SLO and alerting requirements;
- deployment and rollback strategy;
- load profiles and acceptance thresholds;
- incident-response responsibilities.

---

# Required Verification Commands

At minimum, a round-closing review must run:

```bash
git diff --check
mvn clean verify
```

When the Maven wrapper is the approved repository entry point:

```bash
./mvnw clean verify
```

Windows:

```powershell
.\mvnw.cmd clean verify
```

Security and architecture searches from `docs/08_SECURITY.md` must also be
reviewed before a security-sensitive round is closed.

Command success alone is insufficient when required integration infrastructure
or tests were skipped.

---

# Roadmap Update Rules

When a round changes state:

1. Verify every exit criterion.
2. Record evidence from tests and the full build.
3. Update the status in this file.
4. Update `docs/11_CHANGELOG.md`.
5. Synchronize `README.md`, `docs/00_PROJECT_CONTEXT.md` and `docs/01_AI_CONTEXT.md`.
6. Update architecture, database, event, security, Outbox, sequence and deployment documents when affected.
7. Add or update an ADR when an architectural decision changes.
8. Commit the documentation with the implementation or stabilization work it describes.

Do not:

- mark a round complete before verification;
- rewrite history to hide unfinished stabilization;
- start a later service by bypassing an ownership dependency;
- describe planned infrastructure as deployed;
- weaken tests or security to make a round appear complete;
- add scope to a completed round without documenting the new work;
- use roadmap text as a substitute for implementation evidence.

---

# Current Snapshot

| Phase | Rounds | Status |
|---|---|---|
| Foundation Layer | R1–R10 | ✅ Completed |
| Common Infrastructure | R11–R19 | ✅ Completed rounds; documented hardening gaps remain where stated |
| Infrastructure Services | R20–R22 | ✅ Completed |
| Movie Service | R23 | ✅ Completed |
| Inventory Service | R24 | 🚧 Active implementation |
| User Service | R25 | ⏳ Planned |
| Booking Service | R26 | ⏳ Planned |
| Payment Service | R27 | ⏳ Planned |
| Notification Service | R28 | ⏳ Planned |
| Production Readiness | To be assigned | ⏳ Planned |

The active target is:

```text
R24 Inventory Service implementation
```
