# Coding Conventions

Version: R25

---

# Purpose

This document defines the coding conventions for all common, infrastructure,
and business-service modules in the Cinema Booking System.

These rules apply to new code and material refactoring. Existing violations are
technical debt and must not be copied into new implementation.

---

# General Principles

- Prefer clear code over clever code.
- Keep one authoritative owner for every domain rule.
- Use composition before inheritance.
- Apply SOLID, DRY, and KISS where they improve maintainability.
- Avoid speculative abstractions.
- Keep common modules technical and business services domain-specific.
- Do not bypass an accepted ADR or service boundary for convenience.
- Make invalid states difficult to represent and easy to diagnose.

---

# Java Baseline

Use Java 21 and compile with the root Maven release configuration.

Conventions:

- Use records for immutable request, response, event, configuration-value, or
  small value types where framework requirements allow them.
- Use classes for JPA entities and mutable domain objects.
- Prefer immutable fields and constructor injection.
- Avoid field injection.
- Avoid unnecessary inheritance.
- Use interfaces where multiple implementations or a stable boundary are
  meaningful, not automatically for every class.
- Use `Optional` as a return type where absence is expected; do not use it for
  entity fields or request DTO fields.
- Do not return `null` collections.
- Keep methods small enough that ownership, transaction scope, and failure
  behavior remain clear.

---

# Package Naming

Packages are lowercase and follow module ownership.

Examples:

```text
com.cinema.common.security
com.cinema.inventory.controller
com.cinema.inventory.service
com.cinema.inventory.repository
com.cinema.movie.mapper
com.cinema.user.authorization
```

Do not use another service's package as a shared-contract location.

Recommended service package structure:

```text
controller
dto.request
dto.response
entity
enums
exception or error
mapper
repository
service
service.impl
config
event
consumer
producer
```

Add a package only when it represents a real responsibility.

---

# Class Naming

Use responsibility-oriented names:

| Responsibility         | Convention                     | Example                          |
| ---------------------- | ------------------------------ | -------------------------------- |
| REST controller        | `*Controller`                  | `ShowSeatController`             |
| Service contract       | `*Service`                     | `CinemaService`                  |
| Service implementation | `*ServiceImpl`                 | `CinemaServiceImpl`              |
| Repository             | `*Repository`                  | `ShowSeatRepository`             |
| MapStruct mapper       | `*Mapper`                      | `CinemaMapper`                   |
| Configuration          | `*Configuration` or `*Config`  | `SecurityConfiguration`          |
| Validator              | `*Validator`                   | `AudienceValidator`              |
| Properties             | `*Properties`                  | `SecurityProperties`             |
| Event consumer         | `*Consumer` or `*EventHandler` | `SeatReservedConsumer`           |
| Error code             | `*ErrorCode`                   | `InventoryErrorCode`             |
| Integration test       | `*IntegrationTest`             | `InventoryFlywayIntegrationTest` |

Avoid vague names such as `Helper`, `Manager`, `Processor`, or `Util` unless the
class responsibility truly matches and no clearer name exists.

---

# Method and Variable Naming

- Methods use `verb + object` or a clear domain action.
- Variables use `camelCase`.
- Constants use `UPPER_SNAKE_CASE`.
- Boolean methods and fields use readable predicates such as `active`,
  `isExpired`, `hasPermission`, or `canTransitionTo`.
- Do not encode types in variable names.
- Use domain language consistently across code, migrations, APIs, events, and
  documentation.

Examples:

```java
showSeatService.holdShowSeat(request);
showtimeRepository.existsOverlappingShowtime(...);
private static final String REQUIRED_AUDIENCE = "cinema-api";
```

---

# DTO Conventions

Use operation-specific API models:

```text
CreateCinemaRequest
UpdateCinemaRequest
CinemaResponse
HoldShowSeatRequest
ShowSeatResponse
```

Rules:

- Do not expose JPA entities from controllers.
- Do not reuse one mutable DTO for create, update, persistence, and event
  payloads.
- Request DTOs contain transport validation, not repositories or services.
- Response DTOs contain API data, not lazy JPA proxies.
- Event payloads are separate versioned integration contracts.
- Cross-service IDs remain UUIDs and do not become entity associations.
- Use ISO-8601 for date/time values.

---

# API Response Convention

Use the accepted `common-response` and `common-api` contract.

Successful and failed responses must use the standard response shape where the
API architecture requires it.

Do not:

- Return an entity directly.
- Construct a new ad hoc error schema in one controller.
- Leak a framework stack trace or exception message to the client.
- Return `200 OK` for a failed business operation.

HTTP status, error category, stable error code, and message must describe the
same outcome.

---

# Exception and Error-Code Convention

Expected domain, validation, resource, security, and application failures use
`common-exception`.

Shared exception types include:

- `ValidationException`
- `NotFoundException`
- `ConflictException`
- `UnauthorizedException`
- `ForbiddenException`
- `ResourceLockedException`
- `InternalServerException`

Each business service defines a service-owned error-code enum implementing the
shared `ErrorCode` contract.

Example:

```java
throw new ConflictException(
        InventoryErrorCode.SHOW_SEAT_NOT_AVAILABLE);
```

Rules:

- Do not throw `IllegalArgumentException` for expected application, domain, API,
  configuration, serialization, search, storage, or security failures.
- Do not throw a generic `RuntimeException` directly.
- Do not throw `IllegalStateException` as a domain state-transition contract.
- Do not expose raw framework exceptions through the API.
- Translate low-level failures into the most specific shared exception at the
  owning boundary.
- Preserve the original cause when it is safe and useful for diagnostics.
- Never include passwords, tokens, client secrets, or signing-key material in
  exception messages.

Existing uses of `IllegalArgumentException` or `IllegalStateException` in
production code are migration candidates. They do not authorize new uses.

Tests may use assertion failures supplied by JUnit or AssertJ. Test helper
failure behavior should still be explicit and descriptive.

---

# Validation Convention

Transport validation uses Jakarta Bean Validation.

Examples:

```java
@NotBlank
@Size(max = 150)
@Positive
@Future
```

Rules:

- Reusable, domain-neutral annotations and validators belong in
  `common-validation`.
- Cross-field and business invariants belong to the owning service/domain.
- Database constraints remain necessary for authoritative uniqueness and
  referential integrity.
- Do not trust client-provided identifiers merely because they pass format
  validation.
- Validation failures use the standard error response and error-code contract.

---

# Mapping Convention

Use MapStruct 1.6.3 for entity/DTO and model/DTO mapping.

Do not use:

```java
BeanUtils.copyProperties(source, target);
```

Do not introduce reflection-based automatic mapping.

Rules:

- Mapper interfaces belong to the service that owns their source and target
  models.
- Shared mapper configuration belongs in `common-mapper`.
- Prefer explicit mappings when names or semantics differ.
- Ignore entity identifiers, audit fields, version columns, and relationships
  intentionally when creating new entities.
- Do not hide business state transitions inside generated mappings.
- Use `@MappingTarget` only when update semantics are explicit and tested.
- Generated `*MapperImpl` classes must be produced by Maven annotation
  processing and registered as Spring beans when required.
- Do not commit generated sources to compensate for a broken build.

Mapper tests must cover conversions whose behavior is not trivial.

---

# Lombok Policy

ADR-008 prohibits Lombok in common modules.

Common modules use explicit constructors, accessors, equality, and logging
fields where required.

Business-service use of Lombok requires an explicit project decision. Until
then, do not add it merely to reduce line count.

Annotation-processing configuration must not make MapStruct generation depend
on an unused Lombok processor.

---

# JSON Convention

Use the configured `ObjectMapper` from `common-jackson` through dependency
injection.

Rules:

- Do not create an unconfigured `ObjectMapper` inside application code.
- Use ISO-8601 for Java time types.
- Keep event serialization stable and version-aware.
- Do not serialize JPA entities into Kafka events or public responses.
- Convert serialization failures to the accepted shared exception contract.
- Do not log full sensitive payloads.

Test utilities may own a deliberately isolated mapper only when they reproduce
the required platform configuration.

---

# Controller Convention

Controllers are transport adapters.

Controllers may:

- Validate requests.
- Resolve authenticated context through approved security helpers.
- Call one or more application services where orchestration is transport-level.
- Map the result to the standard API response.
- Set correct HTTP status and headers.

Controllers must not:

- Access repositories directly.
- Start database transactions.
- Implement domain state transitions.
- Decode or trust raw JWT claims independently.
- Read another service's database.
- Catch every exception and manufacture a custom response.

Use explicit parameter annotations such as `@PathVariable("showSeatId")` when
the contract benefits from clarity. The Maven compiler must retain parameter
metadata consistently.

---

# Service Convention

Services own application use cases and transaction boundaries.

Rules:

- Keep service interfaces focused on use cases.
- Use constructor injection.
- Mark read-only transactions where useful and correct.
- Make state-transition preconditions explicit.
- Return domain or response models appropriate to the layer.
- Do not call another service's repository.
- Do not keep a local database transaction open across a slow remote call.
- Use an approved API or event contract for cross-service communication.

Domain ownership is more important than reducing the number of service calls.

---

# Persistence Convention

JPA entities belong to one service and must not know about Kafka, controllers,
or another service's entities.

Rules:

- Keep `spring.jpa.open-in-view=false`.
- Flyway owns the schema; Hibernate uses `ddl-auto=validate` in managed
  environments.
- Repositories contain persistence queries, not business workflows.
- Use database constraints for authoritative uniqueness.
- Do not create cross-database foreign keys.
- Store remote service references as stable UUID values.
- Avoid eager loading by default; fetch intentionally for a use case.
- Define equality and hash behavior carefully for mutable JPA entities.
- Do not expose lazy collections to API serialization.

---

# Transaction Convention

Application service methods own `@Transactional` boundaries.

Rules:

- Never place `@Transactional` on a controller.
- Repository methods participate in the service transaction unless a narrowly
  justified repository setting is required.
- Do not attempt one transaction across multiple service databases.
- Aggregate changes and Outbox rows commit in the same local transaction.
- State-changing consumer work and its processed-event marker commit in the
  same local transaction.
- Avoid self-invocation that silently bypasses Spring transaction proxies.
- Keep transaction scope small and deterministic.

---

# Concurrency Convention

The owning service defines concurrency control.

Inventory rules:

- Inventory Service is the sole owner of ShowSeat transitions.
- Use database transactions and `PESSIMISTIC_WRITE` where the accepted
  transition requires serialized access.
- Use Redis/Redisson only for workflows that require distributed coordination.
- Redis locks complement database constraints and transactions; they do not
  replace them.
- Lock keys are stable, collision-resistant, and domain-owned.
- Acquire multiple locks in deterministic order.
- Always release locks safely.
- Verify competing operations with concurrency integration tests.

Do not represent a technical Redis lock as a business seat status.

---

# Kafka and Event Convention

Do not publish directly to Kafka after changing authoritative database state in
the same use case.

Use Transactional Outbox:

```text
domain change + outbox insert -> local commit -> publisher -> Kafka
```

Rules:

- Use stable UUID event IDs.
- Include explicit event versions.
- Use the accepted event envelope.
- Keep business event contracts out of generic technical modules.
- Do not put JPA entities into events.
- Consumers must tolerate duplicate delivery.
- State-changing consumers must be idempotent.
- Do not assume Kafka delivery provides exactly-once business processing.
- Follow the topic and evolution rules in `docs/07_EVENT_CATALOG.md`.

---

# Security Convention

User Service is the authoritative OAuth2/OpenID Connect issuer under ADR-013.
Gateway and protected services are Resource Servers.

Rules:

- Reuse `common-security` for JWT validation and authority conversion.
- Validate signature, issuer, timestamps, and required `cinema-api` audience.
- Use UUID for authenticated user identifiers.
- Normalize role and permission claims by removing blank and duplicate values.
- Do not parse raw JWT claims separately in every service.
- Do not trust a client-provided user ID when authenticated identity determines
  ownership.
- Use `UnauthorizedException` when authentication is missing or invalid.
- Use `ForbiddenException` when authentication exists but permission is
  insufficient.
- Return the standard JSON `401` and `403` contracts.
- Keep servlet security components separate from reactive Gateway security.
- Do not issue tokens from `common-security`.
- Do not use a shared HMAC secret.
- Never log tokens, passwords, client secrets, or signing private keys.

Method and domain authorization complement endpoint rules. A valid JWT does not
authorize every operation.

---

# Configuration Convention

- Use typed `@ConfigurationProperties` for grouped application settings.
- Validate required settings during startup.
- Use environment variables or secret references for sensitive values.
- Do not commit real credentials or private keys.
- Keep development defaults non-sensitive.
- Do not provide a production-capable default password.
- Use separate configuration classes for servlet and reactive technologies.
- Avoid one configuration class that becomes a hidden service locator.

Security configuration must use issuer/JWK/audience properties. It must not
fall back to an embedded token-signing secret.

---

# Logging Convention

Use SLF4J and parameterized messages.

Explicit logger example:

```java
private static final Logger LOGGER =
        LoggerFactory.getLogger(CurrentClass.class);
```

Rules:

- Never use `System.out.println` in application code.
- Do not concatenate expensive log strings when parameterized logging works.
- Include correlation and trace identifiers through shared infrastructure.
- Do not log secrets, bearer tokens, refresh tokens, passwords, private keys,
  or complete payment details.
- Avoid logging the same exception at every layer.
- Use appropriate levels: `DEBUG` for diagnostic detail, `INFO` for meaningful
  lifecycle events, `WARN` for recoverable abnormal behavior, and `ERROR` for
  failures requiring attention.

---

# Testing Convention

Use JUnit Jupiter, Mockito, Spring Test, Spring Security Test, Reactor Test where
applicable, and Testcontainers for production-like integration behavior.

Test categories:

| Category                | Purpose                                                     |
| ----------------------- | ----------------------------------------------------------- |
| Unit                    | Isolated business, conversion, and validation behavior      |
| Controller slice        | Request mapping, validation, response, and servlet security |
| Reactive slice          | Gateway routing/filter/security behavior                    |
| Repository              | Queries, constraints, and persistence behavior              |
| Migration integration   | Flyway schema, indexes, keys, and constraints               |
| Application integration | Spring wiring and configuration                             |
| Concurrency integration | Competing state transitions                                 |
| Messaging integration   | Outbox, Kafka, retry, and idempotency                       |

Rules:

- Name tests by expected behavior, for example
  `bookShouldRequireServiceRole`.
- Use Arrange, Act, Assert structure where it improves readability.
- Do not depend on test execution order.
- Keep tests deterministic.
- Do not use a stale generated mapper implementation from `target/` as hidden
  test setup.
- Investigate the first ApplicationContext exception; the context failure
  threshold message is normally secondary.
- Use `@MockBean` or the current Spring bean-override mechanism only for real
  slice-test boundaries.
- Verify `401`, `403`, and successful access for protected endpoints.
- Use MySQL Testcontainers when testing MySQL-specific constraints or locking.
- Clean up thread pools and synchronization resources in concurrency tests.

Final verification:

```bash
mvn clean verify
```

---

# Maven and Generated Sources

- Root dependency management owns shared versions.
- Pin build-plugin versions at the root.
- Keep compiler and test behavior consistent across modules.
- Configure MapStruct annotation processing explicitly.
- Do not depend on Maven's latest compatible plugin discovery.
- Do not commit `target/` or generated annotation sources.
- A focused module build is not the final project acceptance signal.

Useful focused command:

```bash
mvn -pl services/inventory-service -am clean test
```

Required project command:

```bash
mvn clean verify
```

---

# Git Convention

- Keep one coherent change per commit.
- Use a meaningful imperative commit message.
- Do not combine unrelated formatting, refactoring, and feature changes without
  reason.
- Do not commit secrets, `.env` files, IDE output, build output, or generated
  sources.
- Review `git diff --check` before committing.
- Preserve user-owned unrelated changes in a dirty worktree.

Example messages:

```text
feat(security): validate JWT audience
refactor(inventory): reuse shared authority mapping
docs: synchronize R25 security architecture
test(inventory): verify concurrent ShowSeat hold
```

---

# Documentation Convention

- Durable architectural decisions require an ADR.
- `docs/10_ROADMAP.md` defines planned scope and completion criteria.
- `docs/11_CHANGELOG.md` records completed history.
- `docs/00_PROJECT_CONTEXT.md` and `docs/01_AI_CONTEXT.md` reflect the current
  active checkpoint.
- Update architecture, security, module, deployment, and dependency documents
  when their contracts change.
- Do not mark future runtime behavior as implemented because it is documented.
- When a full documentation file changes during guided work, provide the full
  updated file for review and replacement.

---

# Review Checklist

- [ ] Code belongs to the correct owning module
- [ ] No business-service compile-time dependency was introduced
- [ ] Request, response, entity, and event models remain separate
- [ ] Expected failures use `common-exception`
- [ ] No new `IllegalArgumentException`, `IllegalStateException`, or generic
      `RuntimeException` represents an expected application failure
- [ ] Public APIs use the standard response and error contract
- [ ] MapStruct generates required Spring beans from a clean build
- [ ] Transactions are owned by application services
- [ ] Database constraints and Flyway migrations are correct
- [ ] Cross-service references remain stable UUIDs
- [ ] Kafka publication uses Transactional Outbox where required
- [ ] State-changing consumers are idempotent
- [ ] Security uses shared issuer/JWK/audience validation
- [ ] Tokens and secrets are neither committed nor logged
- [ ] Tests cover success, validation, failure, and authorization paths
- [ ] `mvn clean verify` passes
- [ ] Affected documentation is synchronized

---

# Related Documentation

```text
docs/02_ARCHITECTURE.md
docs/03_TECHNOLOGY_STACK.md
docs/04_MODULES.md
docs/08_SECURITY.md
docs/10_ROADMAP.md
docs/12_DEPENDENCY_RULES.md
docs/decisions/ADR-013-spring-authorization-server.md
```
