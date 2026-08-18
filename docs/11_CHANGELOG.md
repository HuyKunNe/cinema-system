# Changelog

**Version:** 0.6
**Current baseline:** R25.1–R25.10 and R25.11.1–R25.11.7 completed; R25.11.8 active
**Last reviewed:** 2026-08-13

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

| Status         | Meaning                                                                            |
| -------------- | ---------------------------------------------------------------------------------- |
| Completed      | The round was accepted as the repository baseline                                  |
| Implementation | The round is the active implementation target                                      |
| Stabilization  | Main implementation exists, but the round has open exit criteria                   |
| Documentation  | Architecture or operating rules were aligned without claiming new runtime behavior |

Current status:

```text
R1-R24   Completed
R25.1–R25.10 Completed
R25.11   Refresh rotation and revocation in progress
R25.12+  Planned User Service implementation
R26-R28  Planned
```

---

# Unreleased

## 2026-08-17

### Sensitive-Change Authorization Revocation

- Completed R25.11.8 account, credential, OAuth2 client and administrative revocation
  triggers.
- Revoked applicable user authorizations after account lock, account disablement,
  password change and password reset.
- Revoked applicable client authorizations after client deactivation and client-secret
  rotation.
- Enforced `user:manage` for administrative user and client authorization revocation.
- Added durable revocation audit events with actor resolution, safe target references,
  explicit reason codes and revoked-authorization counts.
- Kept revocation audit persistence in the same transaction as the sensitive change and
  authorization invalidation.
- Added integration coverage for all revocation reasons and rollback coverage for audit
  persistence failure.

### Durable Security-Event Recording

- Completed R25.11.9 durable security-event recording.
- Added `security_audit_events` through Flyway migration V11.
- Added append-oriented audit entities, enums, repository and durable recorder.
- Resolved audit actors from Spring Security as `SYSTEM`, `USER` or `CLIENT`.
- Resolved request correlation from MDC correlation or trace identifiers.
- Recorded refresh-token reuse, role and permission changes, OAuth2 client
  registration and lifecycle changes, and form-authentication outcomes.
- Preserved the specialized `oauth2_revocation_audit_events` model.
- Kept security audit data internal to User Service rather than publishing it as a
  Kafka business event.
- Excluded passwords, hashes, raw tokens, token hashes, client secrets, credential
  material and unrestricted request data.
- Added transaction rollback verification for assignment, client lifecycle,
  registration and refresh-token reuse audit failures.

### Concurrent Refresh and Reuse Verification

- Completed R25.11.10 concurrent refresh and reuse verification.
- Rechecked refresh-token history state after acquiring the pessimistic lock.
- Returned standard OAuth2 `invalid_grant` when a request lost the rotation race.
- Prevented a losing request from committing a second successor.
- Verified one successful response and one successor under concurrent refresh.
- Verified concurrent rotated-token reuse revokes the authorization family once.
- Verified one `REUSED` history row, one `REVOKED` successor and one durable reuse
  audit event.
- Added repeated MySQL concurrency execution to detect timing-dependent failures.

### Verification

- User Service clean verification passes.
- Root Maven reactor clean verification passes.
- `git diff --check` passes.

### Remaining R25.11 Work

```text
R25.11.11 — Cleanup, full verification and documentation closure
```

## 2026-08-13

### Completed

- Completed R25.11.7 logout and explicit token revocation.
- Added OAuth2 explicit token revocation through `/oauth2/revoke`.
- Added OpenID Connect RP-Initiated Logout through `/connect/logout`.
- Verified the affected User Service and root Maven reactor tests.

### OAuth2 Explicit Token Revocation

- Added authenticated token revocation for approved confidential clients.
- Invalidated applicable OAuth2 authorization token metadata.
- Synchronized refresh-token history from `ACTIVE` to `REVOKED`.
- Rejected subsequent use of revoked refresh tokens with `invalid_grant`.
- Kept unknown token revocation non-oracular.
- Rejected revocation requests with missing or invalid client authentication.

### OpenID Connect Logout

- Enabled the OIDC provider configuration and advertised the
  `/connect/logout` end-session endpoint.
- Added a shared `SessionRegistry` for authorization-code issuance and logout
  validation.
- Added stable `CinemaUserDetails` equality based on immutable UUID identity so a
  JDBC-deserialized authorization principal can resolve its authenticated session.
- Added and validated the hashed OIDC `sid` claim.
- Validated ID-token hints and exact registered post-logout redirect URIs.
- Preserved the approved logout `state` in the post-logout redirect.
- Invalidated the HTTP session and applicable access, refresh and ID-token metadata
  after successful logout.
- Reused the authorization-service tracking path to revoke refresh-token history.
- Rejected refresh attempts after OIDC logout.
- Kept invalid ID-token hints and unregistered redirect URIs from exposing session,
  authorization or token-history state.

### Logging

- Classified expected validation, authentication, authorization, conflict, locking
  and not-found business exceptions as warnings without stack traces.
- Retained error-level stack traces for unexpected and internal failures.

### Verification

- Added end-to-end MySQL integration coverage for explicit revocation and OIDC
  logout.
- Verified session invalidation, authorization-token invalidation, refresh-history
  revocation and rejection of revoked refresh tokens.
- Verified OIDC discovery metadata, session-principal equality and security failure
  cases.
- Verified `git diff --check` and `mvn clean verify`.

### Remaining R25.11 Work

- Account, password-reset and OAuth2 client revocation triggers.
- Durable auditable security-event recording.
- Concurrent refresh and reuse verification.
- Expired-history cleanup, retention policy and final documentation closure.

## 2026-08-12

### Completed

- Completed R25.9 controlled OAuth2 client persistence and grant policies.
- Completed R25.10 RS256 signing, JWK publication, JWT claim customization and
  end-to-end Client Credentials and Authorization Code with PKCE token issuance.
- Completed R25.11.1–R25.11.6 authorization-session persistence, confidential BFF
  policy, refresh rotation, hashed history tracking, reuse detection and token-family
  invalidation.

### Authorization Server and JWT

- Added externally configured PKCS#8 private-key and X.509 public-key loading.
- Added RS256 JWK source, stable key identifiers, JWT encoder and decoder.
- Added configured issuer and audience plus UUID v7 subjects, username, roles and
  authorized permission claims.
- Added JWK endpoint and public-key verification integration coverage.
- Kept public SPA clients on Authorization Code with mandatory S256 PKCE and without
  refresh tokens.
- Added confidential BFF registration with encoded client secrets, mandatory PKCE,
  Authorization Code and non-reusable 30-day refresh tokens.
- Verified Client Credentials service tokens and confidential user-token flows.

### Authorization Persistence and Refresh Security

- Added Flyway-owned JDBC OAuth2 authorization and consent tables.
- Added a dedicated allowlisted Jackson mapping for persisted `CinemaUserDetails`
  without persisting password hashes.
- Added a tracking authorization-service decorator while retaining Spring
  Authorization Server protocol behavior.
- Added refresh-token history with UUID v7 identifiers, optimistic versioning,
  database constraints, critical indexes and pessimistic token lookup.
- Persisted lowercase SHA-256 token hashes rather than raw refresh tokens in history.
- Added `ACTIVE`, `ROTATED`, `REUSED` and `REVOKED` history transitions.
- Added automatic history creation on issuance and rotation.
- Added reuse detection for rotated tokens, active-family revocation and invalidation
  of current access and refresh token metadata.
- Kept unknown refresh-token failures non-oracular.
- Added unit, Flyway, repository and MySQL protocol integration tests.

### Verification

- Verified public-client absence of refresh tokens.
- Verified confidential-client refresh rotation and rejection of old refresh tokens.
- Verified refresh requests require confidential client authentication.
- Verified current family tokens become unusable after rotated-token reuse.
- Verified OAuth2 persistence uses JDBC and raw history tokens are not stored.
- Verified the root reactor with `mvn clean verify`.

### Remaining R25.11 Work at This Checkpoint

At the end of the 2026-08-12 checkpoint, the remaining work was:

- Logout and explicit token revocation.
- Revocation triggered by account disablement, password reset/change and client
  deactivation or secret rotation.
- Durable auditable security-event publication for refresh-token reuse.
- Concurrent refresh/reuse integration verification.
- Expired-history cleanup, retention policy and final documentation closure.

Logout and explicit token revocation were completed on 2026-08-13.

## 2026-08-10

### Completed

- Completed R25.2 authentication architecture and documentation baseline.
- Completed R25.3 User Service bootstrap and application-context verification.
- Completed R25.4 user, profile and credential persistence with Flyway and UUID v7.
- Completed R25.5 role, permission and assignment foundations with optimized
  effective-authority loading.
- Completed R25.6 password authentication foundation with delegating encoding,
  bcrypt, JPA-backed user details, DAO authentication and hash upgrade support.
- Completed R25.7 account lifecycle and secure email verification.
- Completed R25.8 Spring Authorization Server and OpenID Connect foundation.

### Authorization Server and OAuth2 Clients

- Added separate high-priority Authorization Server and application security filter chains.
- Added externalized canonical issuer configuration and enabled the OIDC baseline.
- Integrated the existing DAO authentication provider and account-status enforcement.
- Added Flyway-owned `oauth2_registered_client` persistence backed by
  `JdbcRegisteredClientRepository`.
- Added controlled server-side registration for public PKCE and confidential
  service clients; dynamic client registration remains disabled.
- Enforced exact HTTPS or loopback HTTP redirect URIs, encoded service secrets,
  non-human service scopes, consent for public clients, 15-minute user access
  tokens, five-minute service access tokens and non-reusable 30-day refresh tokens.
- Added unit, configuration, repository and MySQL Testcontainers integration coverage.
- Restricted Authorization Server metadata to Authorization Code, Refresh Token
  and Client Credentials instead of advertising Spring's additional default grants.
- Added protocol-policy integration tests for S256 PKCE enforcement, consent,
  service-client grant isolation and invalid client authentication.

### Email Verification

- Added positive configurable verification-token lifetime with a 24-hour default.
- Added 256-bit raw token generation using `SecureRandom` and unpadded Base64 URL encoding.
- Persisted only lowercase SHA-256 token hashes.
- Added expiration, revocation and single-use token semantics.
- Added pessimistic token locking for reissue and consumption.
- Made invalid, unknown, expired, revoked and used token failures non-oracular.
- Kept token consumption and email-verification account transition in one transaction.
- Redacted raw tokens from issued-token string output.

### Verification

- Added unit tests for token encoding, hashing, redaction and service behavior.
- Added repository and Flyway verification for the email-verification schema.
- Added MySQL Testcontainers integration tests for issue, reissue, confirmation,
  replay rejection, revoked-token rejection and unknown-token rejection.
- Verified the complete root reactor with `mvn clean verify`.

### Known Boundary

- Public registration, verification controllers, email delivery and password
  recovery are not implemented by R25.7.
- Locking active token rows does not serialize two concurrent first-issue requests
  when no token row exists. User-row locking or an equivalent database invariant
  is required before claiming concurrent first-issue safety.
- R25.9 protocol-level grant-flow verification is the active checkpoint.

## 2026-08-05

### Completed

- Completed R25.1 — `common-security` hardening before User Service
  implementation.
- Removed JWT self-issuance from `common-security`.
- Removed the shared HMAC-secret model and JJWT dependency from the shared
  Resource Server module.
- Centralized JWT principal creation and role/permission authority mapping.
- Added issuer, JWK, timestamp, and required-audience validation foundations.
- Added standard servlet JSON responses for unauthenticated and forbidden
  requests.
- Integrated Inventory Service with the shared Resource Server components.
- Verified the root Maven reactor with `mvn clean verify` after stabilizing the
  compiler, annotation-processing, and test execution configuration.

### Added

- Added `SecurityProperties` for issuer, JWK Set URI, and audience
  configuration.
- Added `AudienceValidator` with the required `cinema-api` audience contract.
- Added shared `CinemaJwtAuthenticationConverter` and
  `CinemaJwtGrantedAuthoritiesConverter` components.
- Added `SecurityErrorCode` and shared-exception integration for security
  context failures.
- Added `CinemaAuthenticationEntryPoint`, `CinemaAccessDeniedHandler`, and the
  shared security response writer.
- Added separate servlet auto-configuration through
  `ServletSecurityConfiguration` so servlet-only dependencies do not leak into
  reactive applications.
- Added unit and integration-oriented coverage for shared security
  configuration, authority conversion, audience validation, security-context
  behavior, and standard `401`/`403` responses.

### Changed

- Changed the authenticated user identifier from `Long` to `UUID` to match the
  platform identity strategy.
- Changed `SecurityContextUtils` to use shared `UnauthorizedException` and
  `ForbiddenException` contracts instead of generic argument or state errors.
- Normalized JWT role and permission claims by removing blanks and duplicates.
- Changed Inventory Service security configuration to reuse shared JWT
  conversion and validation instead of duplicating that logic.
- Pinned Maven compiler, Surefire, Failsafe, and Spring Boot Maven plugin
  versions at the root build to keep annotation processing and test discovery
  deterministic.

### Architecture Decision

- Accepted ADR-013: User Service integrates Spring Authorization Server and is
  the authoritative OAuth2/OpenID Connect issuer.
- Approved Authorization Code with PKCE, Refresh Token, and controlled Client
  Credentials grants.
- Prohibited Resource Owner Password Credentials.
- Approved RS256 access tokens, an RSA key of at least 3072 bits, the
  `cinema-api` audience, and a 15-minute access-token lifetime.
- Approved opaque, rotated, revocable refresh tokens with a maximum lifetime of
  30 days.
- Confirmed that `common-security` contains Resource Server mechanics only and
  never owns token issuance, signing private keys, OAuth2 clients, consent, or
  refresh-token persistence.

### Documentation

- Started R25.2 documentation synchronization.
- Updated project and AI context to show R25.1 completed and R25.2 active.
- Updated architecture, module, technology-stack, security, and roadmap
  documentation for the accepted Authorization Server topology.
- Added ADR-013 for the Spring Authorization Server decision.
- Kept R25.3 and later User Service runtime implementation explicitly planned;
  documentation changes do not claim that those features already exist.

### Verified

- `common-security` no longer provides an API for issuing JWTs.
- JWT authorities are mapped consistently from role and permission claims.
- Blank and duplicate authorities are removed.
- Resource Server validation includes the configured issuer and required
  audience.
- Missing authentication and insufficient authorization use the shared
  exception and API response contracts.
- Servlet security handlers are isolated from reactive security configuration.
- Inventory security tests use the shared security components.
- Focused Inventory tests and root `mvn clean verify` pass.

### Current Work

- R25.2 documentation synchronization remains active until all affected files
  are aligned and reviewed.
- R25.3 — User Service foundation is the next implementation checkpoint after
  R25.2 closes.

## 2026-08-04

### Completed

- Completed R24.5.1–R24.5.9 stabilization and exit-criteria verification.
- Completed R24 — Inventory Service.
- Accepted R24.5.9 — Documentation synchronization and R24 closure as the
  latest completed checkpoint.
- Set R25 — User Service as the next approved round; implementation has not
  started.

### Added

- Added ShowSeat endpoint-authorization coverage for public, management and
  service-only operations.
- Added JWT role and permission authority-mapping verification.
- Added duplicate event-processing idempotency verification.
- Added repository, integration, security and concurrency evidence required by
  the R24 exit criteria.

### Changed

- Finalized ShowSeat authorization policy:
  - approved query endpoints are public;
  - generation and administrative availability changes require
    `inventory:manage`;
  - hold, book and release require `ROLE_SERVICE`.
- JWT roles and permissions are mapped to granted authorities while blank and
  duplicate authorities are removed.
- Updated the repository baseline from active Inventory implementation to
  completed Inventory Service.

### Verified

- Inventory Service owns Cinema, Room, fixed Seat, Showtime and ShowSeat data.
- Flyway migration and Hibernate schema validation pass.
- Cinema, Room, Seat, Showtime and ShowSeat behavior is covered.
- Showtime time-range and overlap invariants are enforced.
- ShowSeat generation is transactional and idempotent.
- ShowSeat state transitions are atomic and use `PESSIMISTIC_WRITE` where
  mutable state is loaded.
- Hold ownership and expiration rules are enforced.
- Concurrent requests for the same ShowSeat allow at most one successful hold.
- Duplicate event processing is idempotent.
- Endpoint authorization behavior is covered by applicable `401`, `403` and
  successful-access tests.
- Unit, mapper, controller, repository, integration, security and concurrency
  tests pass.
- Full Maven verification passes.
- R24 documentation is synchronized.

### R25 Preparation

- R25 — User Service is the next approved round.
- Before implementation, confirm the authoritative token issuer, OAuth2 or
  authorization-server topology, access-token issuer and audience validation,
  signing-key ownership and rotation, token lifecycle, and privileged-account
  security requirements.

## 2026-08-03

### Added

- Completed transactional ShowSeat booking transitions:
  - `AVAILABLE → HELD`;
  - `HELD → BOOKED`;
  - `HELD → AVAILABLE`.
- Completed administrative availability transitions:
  - `AVAILABLE → UNAVAILABLE`;
  - `HELD → UNAVAILABLE`;
  - `UNAVAILABLE → AVAILABLE`.
- Added ShowSeat hold ownership and expiration validation.
- Added administrative ShowSeat availability endpoints.
- Added Inventory-specific error codes for invalid availability transitions.
- Added entity, service and controller coverage for ShowSeat transitions.
- Added a MySQL concurrency integration test proving that only one of two
  competing hold requests can succeed.

### Changed

- ShowSeat transition services now load current state using
  `findByIdForUpdate(...)`.
- ShowSeat transitions execute inside Spring-managed transactions.
- Added the missing transactional boundary to the ShowSeat release operation.
- `HELD → UNAVAILABLE` now clears `heldByBookingId` and `holdExpiresAt`.
- Invalid administrative transitions now use the shared `ConflictException` and
  `InventoryErrorCode` contract.
- Database transaction and `PESSIMISTIC_WRITE` row locking are the current
  correctness mechanism for single-row ShowSeat transitions.
- Redis locking was not added as a redundant second lock for these operations.

### Documentation

- Synchronized the roadmap and changelog with the current implementation
  sequence.
- Preserved the database-per-service boundary.
- Preserved Movie Service ownership of movies and genres.
- Preserved Inventory Service ownership of cinema, room, seat, showtime, and
  ShowSeat data.
- Recorded that R24 was still in progress at this historical checkpoint.

### Verified

- ShowSeat entity tests pass.
- ShowSeat transition service tests pass.
- ShowSeat controller tests pass.
- MySQL concurrency integration testing verifies one successful hold and one
  conflict for two concurrent requests targeting the same ShowSeat.
- R24.4.13.6 and R24.4.13.7 are accepted as completed checkpoints.

### In Progress at This Checkpoint

- Started R24.4.13.8 — ShowSeat endpoint authorization.
- The active authorization work covers:
  - public ShowSeat query operations;
  - `inventory:manage` for generation and administrative availability changes;
  - `SERVICE` for hold, book and release operations;
  - applicable `401`, `403` and successful authorization tests.

### Remaining at This Checkpoint

- R24 as a whole remains in implementation.
- R24.4.13.8 security verification is not yet complete.
- The root Maven verification and remaining R24 exit criteria must pass before
  R24 can be marked completed.
- R25 has not started.

---

# R25 - User Service and Identity Platform

**Status:** Implementation
**Started:** 2026-08-05

### R25.1 - common-security Hardening

**Status:** Completed

Before implementing User Service, the shared security boundary was hardened so
that it could be safely reused by Gateway and business Resource Servers.

Accepted baseline:

- Authenticated user identifiers use UUID.
- Shared Resource Servers validate JWTs through issuer and JWK configuration.
- Access tokens require the `cinema-api` audience.
- Roles and permissions use one shared authority-mapping contract.
- Security context failures use `common-exception`.
- Servlet applications receive standard JSON `401` and `403` responses.
- `common-security` cannot issue access or refresh tokens.
- Signing keys and user/account persistence do not belong to
  `common-security`.

### R25.2 - Architecture and Documentation Synchronization

**Status:** Completed

The documentation was synchronized before User Service runtime work began.
ADR-013 records the accepted identity topology:

- User Service hosts Spring Authorization Server.
- User Service is the authoritative OAuth2/OpenID Connect issuer.
- Gateway and protected business services are Resource Servers.
- Access tokens use RS256 and public JWK discovery.
- Authorization Code with PKCE, Refresh Token, and controlled Client
  Credentials are the approved grants.
- Privileged production access requires MFA policy enforcement.

### Remaining R25 Scope

R25.8 and later checkpoints remain. They include Authorization Server
configuration, registered clients, consent, JWT/JWK issuance, refresh-token
rotation, profile/account APIs, Gateway and Resource Server integration,
protocol verification, operational readiness and final documentation closure.

### R25.3–R25.7 - Identity Foundation

**Status:** Completed

Implemented service bootstrap, identity persistence, roles and permissions,
password authentication, account lifecycle and email verification. OAuth2/OIDC
protocol endpoints and token issuance remain R25.8 and later work.

---

# R24 - Inventory Service

**Status:** Completed
**Started:** 2026-07-24
**Completed:** 2026-08-04

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

### Completion State

R24.5.1–R24.5.9, R24.5 and R24 are complete. Inventory Service implementation,
stabilization, schema verification, business-invariant verification, API and
security verification, concurrency testing, full Maven verification and
documentation synchronization have been accepted.

R25 — User Service is the next approved round and has not started.

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

At this historical checkpoint, R23 was accepted as completed and R24 Inventory
Service became the active round.

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
- Selected R24 Inventory Service as the active round at this historical
  checkpoint.
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
