# Changelog

Version: 0.3
Current baseline: R23 stabilization
Last reviewed: 2026-07-24

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
| Stabilization | Main implementation exists, but the round has open exit criteria |
| Documentation | Architecture or operating rules were aligned without claiming new runtime behavior |

Current status:

```text
R1-R22   Completed
R23      Stabilization
R24-R28  Planned; not part of this changelog
```

---

# Unreleased Documentation Alignment

## 2026-07-24

### Changed

- Expanded `docs/09_OUTBOX.md` from a short overview into the implementation
  contract for Transactional Outbox and Idempotent Consumer behavior.
- Documented the current outbox state flow as
  `PENDING -> PROCESSING -> SENT | FAILED`.
- Recorded the current batch size, scheduler delay, retry limit, Kafka
  acknowledgement boundary, ordering rules, security requirements, observability,
  failure scenarios, and test matrix.
- Explicitly documented current outbox gaps instead of representing them as
  implemented features. These include safe multi-instance claiming, processing
  leases and recovery, `next_retry_at`, retry backoff, `last_error`, cleanup,
  manual replay controls, DLT handling, and `common-outbox` tests.
- Expanded `docs/10_ROADMAP.md` with round status definitions, shared Definition
  of Done, dependencies, quality gates, R23 exit criteria, and the approved scope
  for R24-R28.
- Confirmed that R23 remains in stabilization and that R24 must not be marked as
  started until the R23 exit criteria are satisfied.

### Verification

- Documentation changes were checked with `git diff --check`.
- Maven verification was not required for documentation-only changes.

---

# R23 - Movie Service

Status: Stabilization
Implemented: 2026-07-23

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
- Flyway migration for movie and genre tables.
- Flyway seed migration for default genres.
- Config Server configuration for Movie Service.
- Integration with Eureka, OpenAPI, validation, shared error handling, JPA, and
  the existing common modules.

### Changed

- Updated shared UUID generation and its tests.
- Updated JPA auditing and base entity behavior required by Movie Service.
- Updated logging auto-configuration integration.
- Extended `GlobalExceptionHandler` for the error handling required by the
  service.
- Removed hard-coded active Movie Service database credentials and replaced
  them with environment-based configuration.
- Aligned documentation so Movie Service owns movie catalog data and Inventory
  Service exclusively owns and updates `show_seats`.

### Known Remaining Work

- Restore and complete Movie Service unit tests.
- Add controller security and validation tests.
- Add repository and Flyway integration tests.
- Verify pagination, filtering, slug uniqueness, duplicate handling, and update
  behavior.
- Verify authentication, authorization, and administrative access rules.
- Run the complete `mvn clean verify` quality gate.
- Resolve every remaining R23 exit criterion in `docs/10_ROADMAP.md`.

The Movie Service feature implementation was merged, but removal of its initial
test skeletons means R23 is not complete.

---

# R22 - API Gateway

Status: Completed
Implemented: 2026-07-22

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

Status: Completed
Implemented: 2026-07-22

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

Status: Completed
Implemented: 2026-07-22

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

Status: Completed
Implemented: 2026-07-22

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

Status: Completed
Implemented: 2026-07-22

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

Status: Completed
Implemented: 2026-07-22

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

Status: Completed
Implemented: 2026-07-22

### Added

- Shared OpenAPI metadata configuration.
- Swagger UI integration.
- JWT bearer security scheme.
- Service-specific OpenAPI properties.
- OpenAPI auto-configuration registration.
- OpenAPI configuration tests.

---

# R15 - common-search

Status: Completed
Stabilized: 2026-07-22

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

Status: Completed baseline
Stabilized: 2026-07-22

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

Status: Completed
Stabilized: 2026-07-22

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

Status: Completed
Stabilized: 2026-07-22

### Added

- Distributed lock abstraction.
- Redisson-backed implementation.
- Shared lock configuration and constants.

### Stabilized

- Lock ownership, timeout behavior, and module dependencies were aligned for
  service reuse.

---

# R11 - common-security

Status: Completed baseline
Stabilized: 2026-07-22

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

Status: Completed
Stabilized: 2026-07-22

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

Status: Completed
Completed: 2026-07-22

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

Status: Completed
Stabilized: 2026-07-22

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

Status: Completed
Stabilized: 2026-07-22

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

Status: Completed
Stabilized: 2026-07-21

### Added

- Shared `GlobalExceptionHandler`.
- Validation error mapping.
- Common API response integration.
- Standard exception-to-HTTP response behavior for service modules.

---

# R5 - common-response

Status: Completed
Stabilized: 2026-07-21

### Added

- Generic `ApiResponse`.
- Structured error body and validation errors.
- Page response and page metadata models.
- Response factory.
- Response model tests.

---

# R4 - common-exception

Status: Completed
Stabilized: 2026-07-21

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

Status: Completed
Stabilized: 2026-07-21

### Added

- Shared JPA base entity hierarchy.
- UUID-based entity support.
- Auditing configuration and `AuditorAware`.
- Base repository contract.

### Fixed

- Corrected package and source-root alignment for JPA audit classes.

---

# R2 - common-core

Status: Completed
Stabilized: 2026-07-21

### Added

- Shared constants.
- Collection, enum, number, object, string, and JSON utilities.
- Time provider abstraction.
- UUID utilities and UUID v7 generator.
- UUID generator tests.

---

# R1 - Parent Project

Status: Completed
Stabilized: 2026-07-21

### Added

- Maven multi-module parent project.
- `common`, `infrastructure`, and `services` module groups.
- Java 21 compiler baseline.
- Spring Boot 3.5.4 dependency baseline.
- Central dependency and plugin management.
- Initial repository documentation and module structure.

### Stabilized

- Corrected dependency versions and parent POM configuration.
- Established the build order used by subsequent rounds.

---

# Cross-Round Architecture Corrections

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
