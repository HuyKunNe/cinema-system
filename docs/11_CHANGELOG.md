# Changelog

**Version:** 0.4
**Current baseline:** R24 Inventory Service implementation
**Last reviewed:** 2026-07-24

---

# Purpose

This changelog records capabilities that were added, stabilized, corrected, or
documented in the Cinema Booking System repository.

It is a historical record, not a plan. Future scope and completion criteria
belong in `docs/10_ROADMAP.md`.

A module appearing in this file does not prove that every production-readiness
requirement is complete. The status of the active round remains authoritative in
the roadmap.

---

# Status

| Status | Meaning |
|---|---|
| Completed | The round was accepted as the repository baseline |
| Implementation | The round is the active implementation target |
| Stabilization | Main implementation exists, but the round has open exit criteria |
| Documentation | Architecture or operating rules were aligned without claiming new runtime behavior |

Current status:

```text
R1-R23   Completed
R24      Inventory Service implementation
R25-R28  Planned
```

---

# Unreleased

## 2026-07-24

### Added

- Started R24 Inventory Service as the active business-service round.
- Defined Inventory Service ownership of cinemas, rooms, physical seats,
  showtimes, `show_seats`, seat availability, and reservation state.
- Defined the initial Inventory domain model:

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

- Defined fixed physical Seat data separately from per-showtime ShowSeat data.
- Defined the initial Inventory status models for Cinema, Room, Seat, Showtime,
  and ShowSeat.
- Defined `AVAILABLE`, `HELD`, `BOOKED`, and `UNAVAILABLE` as the initial
  ShowSeat business states.
- Defined the initial ShowSeat transition baseline and concurrency requirements.
- Added R24 Inventory Service scope, business invariants, and exit criteria to
  `docs/10_ROADMAP.md`.

### Changed

- Marked R23 Movie Service as completed.
- Changed the active target from R23 Movie Service stabilization to R24
  Inventory Service implementation.
- Moved Inventory Service from R25 to R24.
- Moved User Service from R24 to R25.
- Reordered the business-service milestones to:

  ```text
  R23 Movie Service
  R24 Inventory Service
  R25 User Service
  R26 Booking Service
  R27 Payment Service
  R28 Notification Service
  ```

- Confirmed that physical seat layouts must not be hard-coded in Java source.
- Confirmed that `showtimes.movie_id` is an external Movie Service UUID
  reference and must not have a cross-service database foreign key.
- Confirmed that Inventory Service is the only service allowed to modify
  `show_seats`.
- Confirmed that Redis locks are a technical coordination mechanism and that
  `HELD` is the corresponding temporary business reservation state.
- Updated `docs/10_ROADMAP.md` to version R24.

### Documentation

- Synchronized the roadmap and changelog with the current implementation
  sequence.
- Preserved the database-per-service boundary.
- Preserved Movie Service ownership of movies and genres.
- Preserved Inventory Service ownership of cinema, room, seat, showtime, and
  ShowSeat data.
- Documented that R24 is started but not completed.

### Verification

- This entry records documentation alignment and the accepted current project
  status.
- It does not claim that R24 implementation tests or the R24 root Maven quality
  gate have passed.
- R24 completion remains subject to the exit criteria in
  `docs/10_ROADMAP.md`.

---

# R24 - Inventory Service

**Status:** Implementation
**Started:** 2026-07-24

### Approved Ownership

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

### Approved Initial Scope

- Inventory Service Maven module.
- Spring Boot application entry point.
- Inventory database and Flyway migrations.
- Cinema management.
- Room management.
- Fixed seat-layout management.
- Showtime management.
- ShowSeat generation.
- Atomic ShowSeat state transitions.
- Shared exception and response integration.
- Bean Validation and business validation.
- MapStruct mapping.
- JPA auditing.
- Config Server and Eureka integration.
- OpenAPI documentation.
- Unit and integration tests.
- Concurrency verification where seat state transitions matter.

### Architecture

- A Cinema contains one or more Rooms.
- A Room owns a fixed physical seat layout.
- A Seat is a permanent physical position inside a Room.
- A Showtime schedules one external Movie Service movie in one Room.
- A ShowSeat represents one Seat for one Showtime.
- Creating a Showtime generates one ShowSeat for each active Seat in its Room.
- Booking Service accesses Inventory only through approved APIs, commands, and
  events.
- Booking Service must not read or update the Inventory database directly.
- Cross-service database foreign keys are prohibited.
- `showtimes.movie_id` stores only the Movie Service UUID reference.

### Business-State Baseline

```text
AVAILABLE
HELD
BOOKED
UNAVAILABLE
```

Redis locks are not represented as a `LOCKED` business state.

### Current State

R24 is the active implementation round. Its implementation and verification
evidence must be recorded in future changelog updates as each accepted increment
is completed.

---

# R23 - Movie Service

**Status:** Completed
**Implemented:** 2026-07-23
**Completed:** 2026-07-24

### Added

- Movie Service Spring Boot application.
- Movie and Genre REST controllers.
- Operation-specific create and update request DTOs.
- Movie and Genre response DTOs.
- Movie and Genre JPA entities.
- Movie status model.
- Repositories, service interfaces, and service implementations.
- MapStruct mappers and Movie Service mapper configuration.
- Movie-specific error codes.
- Slug generation utility.
- Pagination and filtering support.
- Flyway migration for movie and genre tables.
- Flyway seed migration for default genres.
- Config Server configuration for Movie Service.
- Integration with Eureka, OpenAPI, validation, shared error handling, JPA, and
  the existing common modules.
- Swagger/OpenAPI documentation and manual endpoint verification.

### Changed

- Updated shared UUID generation and its tests.
- Updated JPA auditing and base entity behavior required by Movie Service.
- Updated logging auto-configuration integration.
- Extended `GlobalExceptionHandler` for Movie Service request and validation
  errors.
- Added consistent handling for malformed request bodies and invalid UUID path
  values.
- Removed hard-coded active Movie Service database credentials and replaced
  them with environment-based configuration.
- Aligned documentation so Movie Service owns movie catalog data and Inventory
  Service exclusively owns and updates `show_seats`.
- Confirmed that Movie Service does not own cinemas, rooms, seats, showtimes, or
  ShowSeats.

### Verified Functional Scope

- Movie and Genre create, update, read, list, and lifecycle behavior.
- Request validation and business validation.
- Duplicate and slug-related behavior.
- Pagination and filtering behavior.
- Shared API response and error contracts.
- Flyway schema and default genre initialization.
- JPA auditing behavior.
- OpenAPI endpoint visibility and request execution.

### Outcome

Movie Service is the authoritative owner of movies, genres, movie lifecycle
state, and movie catalog metadata.

R23 is accepted as completed and R24 Inventory Service is now the active round.

---

# R22 - API Gateway

**Status:** Completed
**Implemented:** 2026-07-22

### Added

- Spring Cloud Gateway service.
- Config Server and Eureka client integration.
- Route configuration for system services.
- Request identifier propagation through a global filter.
- Actuator health integration.
- Distributed tracing integration.
- Gateway application, filter, and actuator tests.

### Stabilized

- Gateway module structure and configuration were aligned with the parent
  project and shared infrastructure conventions.

---

# R21 - Discovery Service

**Status:** Completed
**Implemented:** 2026-07-22

### Added

- Spring Cloud Netflix Eureka Server.
- Central service registration and discovery.
- Eureka dashboard.
- Config Server integration.
- Actuator health endpoints.
- Tracing integration.
- Native and container-ready configuration.
- Application context and Eureka dashboard endpoint tests.

---

# R20 - Config Server

**Status:** Completed
**Implemented:** 2026-07-22

### Added

- Spring Cloud Config Server.
- Native configuration repository.
- Git-backed configuration repository profile.
- Shared and service-specific configuration files.
- Environment-aware configuration foundation.
- Actuator health endpoints.
- Config Server application and endpoint tests.

---

# R19 - common-storage

**Status:** Completed
**Implemented:** 2026-07-22

### Added

- Storage service abstraction.
- MinIO integration.
- Upload and download operations.
- Object metadata model.
- Object existence and deletion operations.
- Presigned download URL support.
- Automatic bucket initialization.
- Storage auto-configuration.

---

# R18 - common-tracing

**Status:** Completed
**Implemented:** 2026-07-22

### Added

- Micrometer Tracing abstraction.
- OpenTelemetry bridge support.
- OTLP exporter support.
- Current trace and span context access.
- Reusable custom span service.
- Span tags, events, and error recording.
- Tracing auto-configuration.
- Trace context and tracing service unit tests.

---

# R17 - common-test

**Status:** Completed
**Implemented:** 2026-07-22

### Added

- Shared JUnit 5 test configuration.
- Reusable unit and integration test annotations.
- MySQL Testcontainers support.
- Kafka Testcontainers support.
- Redis Testcontainers support.
- JSON test utilities.
- Reusable integration test base classes.

---

# R16 - common-openapi

**Status:** Completed
**Implemented:** 2026-07-22

### Added

- Shared OpenAPI metadata configuration.
- Swagger UI integration.
- JWT bearer security scheme.
- Service-specific OpenAPI properties.
- OpenAPI auto-configuration registration.
- OpenAPI configuration tests.

---

# R15 - common-search

**Status:** Completed
**Stabilized:** 2026-07-22

### Added

- Search abstraction.
- Elasticsearch client integration.
- Full-text search support.
- Search pagination and sorting.
- Document indexing support.
- Search auto-configuration.

### Stabilized

- Module dependencies and shared search contracts were aligned with the parent
  project.

---

# R14 - common-outbox

**Status:** Completed baseline
**Stabilized:** 2026-07-22

### Added

- Transactional Outbox entity and repository.
- Outbox event payload and message contracts.
- Aggregate and outbox status models.
- Outbox service abstraction and default implementation.
- Scheduled batch processing.
- Kafka outbox publisher.
- Retry counter and terminal failure state.
- Outbox auto-configuration.

### Current Runtime Contract

- New records start in `PENDING`.
- The scheduler loads a bounded batch.
- Processing transitions records to `PROCESSING`.
- A successful Kafka completion marks a record `SENT`.
- A failed publish increments retry state and returns the record to `PENDING` or
  marks it `FAILED` when the retry limit is reached.

R14 established the baseline implementation. Production-hardening gaps are
tracked in `docs/09_OUTBOX.md` and the roadmap; they are not retroactively
claimed as completed here.

---

# R13 - common-kafka

**Status:** Completed
**Stabilized:** 2026-07-22

### Added

- Shared Kafka event model.
- Producer abstraction and default producer service.
- Event publisher abstraction.
- Producer and consumer configuration.
- Event serializer.
- Consumer error handler.
- Retry configuration.
- Kafka constants and controlled publish exceptions.

### Stabilized

- Shared event publication contracts and Kafka configuration were aligned for
  reuse by service modules and the outbox publisher.

---

# R12 - common-lock

**Status:** Completed
**Stabilized:** 2026-07-22

### Added

- Distributed lock abstraction.
- Redisson-backed implementation.
- Shared lock configuration and constants.

### Stabilized

- Lock ownership, timeout behavior, and module dependencies were aligned for
  service reuse.

---

# R11 - common-security

**Status:** Completed baseline
**Stabilized:** 2026-07-22

### Added

- Shared Spring Security configuration.
- JWT token and claim abstractions.
- Authentication principal model.
- Current-user annotation and security context utilities.
- Permission evaluator contract.
- Shared security constants.

### Stabilized

- Security module dependencies and reusable contracts were aligned with the
  parent project.

R11 provides shared infrastructure. Endpoint-specific authentication,
authorization, ownership checks, issuer and audience validation, and production
secret management remain responsibilities of the consuming application and its
deployment configuration.

---

# R10 - common-mapper

**Status:** Completed
**Stabilized:** 2026-07-22

### Added

- Shared MapStruct configuration.
- Base, collection, and page mapper contracts.
- Mapping utility support.
- Mapper tests.

### Fixed

- Corrected tests that asserted copied collections must always be different
  object instances when the contract did not require that behavior.
- Centralized MapStruct dependency management.

---

# R9 - common-logging

**Status:** Completed
**Completed:** 2026-07-22

### Added

- Logging aspect.
- Correlation ID filter and log context.
- Logging utilities and constants.
- Logging auto-configuration.
- Unit tests for shared logging behavior.

### Fixed

- Added the explicit SLF4J logger required by `LoggingAspect`.

---

# R8 - common-jackson

**Status:** Completed
**Stabilized:** 2026-07-22

### Added

- Shared Jackson configuration.
- Java Time module support.
- ISO-8601 timestamp serialization.
- Shared JSON utility methods.
- Serialization and unknown-property tests.

### Fixed

- Corrected `convert`, `fromJson`, and `toJson` implementations so every generic
  method returns its result and compiles correctly.
- Added the Java Time datatype dependency required for date and time values.
- Retained construction through `new JacksonConfiguration()` where required by
  the agreed test design.

---

# R7 - common-validation

**Status:** Completed
**Stabilized:** 2026-07-22

### Added

- Reusable validation constants.
- Create, Update, and Delete validation groups.
- Enum value validation.
- UUID v7 validation.

### Fixed

- Added the Jakarta Expression Language dependency required by Bean Validation
  in the module test environment.

---

# R6 - common-api

**Status:** Completed
**Stabilized:** 2026-07-21

### Added

- Shared `GlobalExceptionHandler`.
- Validation error mapping.
- Common API response integration.
- Standard exception-to-HTTP response behavior for service modules.

---

# R5 - common-response

**Status:** Completed
**Stabilized:** 2026-07-21

### Added

- Generic `ApiResponse`.
- Structured error body and validation errors.
- Page response and page metadata models.
- Response factory.
- Response model tests.

---

# R4 - common-exception

**Status:** Completed
**Stabilized:** 2026-07-21

### Added

- Shared error code contract and categories.
- Common error codes.
- Business and technical exception hierarchy.
- Not found, conflict, unauthorized, forbidden, validation, resource-locked, and
  internal-server exceptions.

### Fixed

- Kept Object methods out of interface default implementations where Java does
  not allow them to override `Object`.

---

# R3 - common-jpa

**Status:** Completed
**Stabilized:** 2026-07-21

### Added

- Shared JPA base entity hierarchy.
- UUID-based entity support.
- Auditing configuration and `AuditorAware`.
- Base repository contract.

### Fixed

- Corrected package and source-root alignment for JPA audit classes.

---

# R2 - common-core

**Status:** Completed
**Stabilized:** 2026-07-21

### Added

- Shared constants.
- Collection, enum, number, object, string, and JSON utilities.
- Time provider abstraction.
- UUID utilities and UUID v7 generator.
- UUID generator tests.

---

# R1 - Parent Project

**Status:** Completed
**Stabilized:** 2026-07-21

### Added

- Maven multi-module parent project.
- `common`, `infrastructure`, and `services` module groups.
- Java 21 compiler baseline.
- Spring Boot 3.5.16 dependency baseline.
- Central dependency and plugin management.
- Initial repository documentation and module structure.

### Stabilized

- Corrected dependency versions and parent POM configuration.
- Established the build order used by subsequent rounds.

---

# Cross-Round Architecture Corrections

## 2026-07-24 - Inventory Round Ordering and Domain Model

### Roadmap

- Accepted R23 Movie Service as completed.
- Selected R24 Inventory Service as the active round.
- Moved User Service to R25.
- Preserved Booking, Payment, and Notification as R26, R27, and R28.

### Inventory Ownership

- Confirmed that Inventory Service owns Cinema, Room, Seat, Showtime, and
  ShowSeat data.
- Confirmed that Seat is fixed physical room inventory.
- Confirmed that ShowSeat is generated per Showtime.
- Confirmed that Inventory Service exclusively modifies `show_seats`.
- Confirmed that Booking Service coordinates through service boundaries instead
  of direct database access.

### Cross-Service References

- Confirmed that Inventory stores Movie IDs as external UUID references.
- Prohibited cross-service database foreign keys.

### Seat State

- Defined `HELD` as the temporary business reservation state.
- Kept Redis locking as an implementation concern rather than a domain status.

## 2026-07-23 - Credentials and Service Ownership

### Security

- Removed hard-coded Movie Service database credentials from active
  configuration.
- Documented environment-provided credentials and the prohibition on application
  use of the MySQL root account.

### Data Ownership

- Confirmed database-per-service boundaries.
- Confirmed that Inventory Service exclusively owns and updates
  `show_seats`.
- Removed documentation that implied Booking Service could directly reserve or
  update inventory rows.
- Aligned the event flow so Booking Service requests a reservation and Inventory
  Service reports the result through Kafka.

### Documentation

- Aligned the README, project context, AI context, architecture, technology
  stack, modules, coding conventions, database design, event catalog, security,
  outbox, roadmap, changelog, dependency rules, sequence diagrams, and
  deployment guidance.

These corrections changed architecture and configuration documentation; they did
not claim that the planned Booking, Inventory, Payment, Notification, or User
Service workflows were already implemented.

---

# Change Recording Rules

Future entries must:

1. Record the round or independently approved correction.
2. Use repository evidence such as merged code, migrations, tests, and
   configuration.
3. Separate added, changed, fixed, removed, security, and known-gap information
   when applicable.
4. Never describe planned behavior as implemented behavior.
5. Include breaking database, API, topic, event schema, or configuration changes.
6. Reference an accepted ADR when an architectural decision changes.
7. Update `docs/10_ROADMAP.md` separately when progress or exit criteria change.
8. Avoid secrets, credentials, personal data, and sensitive operational values.
9. Record verification truthfully; do not claim a build or test run that did not
   occur.

---

# Related Documentation

```text
docs/00_PROJECT_CONTEXT.md
docs/02_ARCHITECTURE.md
docs/07_EVENT_CATALOG.md
docs/08_SECURITY.md
docs/09_OUTBOX.md
docs/10_ROADMAP.md
docs/14_DEPLOYMENT.md
docs/decisions/
```
