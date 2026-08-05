# Technology Stack

Version: R25

This document records the approved platform technologies, the versions pinned
by the root Maven build, and the difference between implemented and planned
capabilities.

The root `pom.xml` is authoritative for dependency and build-plugin versions.

---

# Version Baseline

| Technology                   |  Version | Status                                         |
| ---------------------------- | -------: | ---------------------------------------------- |
| Java                         |       21 | Implemented                                    |
| Spring Boot                  |   3.5.16 | Implemented                                    |
| Spring Cloud                 | 2025.0.2 | Implemented                                    |
| MySQL Connector/J            |    8.4.0 | Implemented                                    |
| Flyway                       |   12.0.0 | Implemented                                    |
| Apache Kafka client baseline |    4.0.0 | Implemented in common infrastructure           |
| Spring for Apache Kafka      |    3.3.7 | Implemented in common infrastructure           |
| Redisson                     |   3.50.0 | Implemented                                    |
| Elasticsearch                |   8.18.1 | Implemented in `common-search`                 |
| MapStruct                    |    1.6.3 | Implemented                                    |
| springdoc-openapi            |    2.8.9 | Implemented                                    |
| MinIO Java SDK               |   8.5.17 | Implemented in `common-storage`                |
| Micrometer                   |   1.15.0 | Implemented in common tracing/actuator support |
| OpenTelemetry                |   1.49.0 | Implemented in `common-tracing`                |
| Testcontainers               |   1.21.3 | Implemented                                    |
| Maven Compiler Plugin        |   3.14.0 | Pinned                                         |
| Maven Surefire Plugin        |    3.5.3 | Pinned                                         |
| Maven Failsafe Plugin        |    3.5.3 | Pinned                                         |

Do not duplicate these versions in child modules unless the dependency has a
documented compatibility reason that cannot be expressed through root
dependency management.

---

# Programming Language

Java 21 is the platform language and compiler release.

Relevant capabilities include:

- Long-term support release
- Records
- Pattern matching
- Modern switch expressions
- Virtual-thread readiness where later profiling justifies adoption

Code must compile with the root Maven compiler configuration. Local IDE success
does not replace a successful root reactor build.

---

# Build System

Apache Maven provides the multi-module reactor.

The root build owns:

- Module registration
- Dependency management
- Java release configuration
- Annotation processors
- Build-plugin versions
- Test execution policy

MapStruct and Lombok annotation processing must be configured consistently so
generated mapper implementations are available in both focused module builds
and `mvn clean verify` from the repository root.

Required final verification:

```bash
mvn clean verify
```

Focused module tests are useful during development but are not the final exit
criterion:

```bash
mvn -pl services/inventory-service -am clean test
```

---

# Application Framework

Spring Boot 3.5.16 is the application baseline.

Used capabilities include:

- Spring MVC
- Spring WebFlux for Gateway
- Spring Validation
- Spring Security
- Spring Data JPA
- Spring AOP
- Spring for Apache Kafka
- Spring Boot Actuator
- Spring Boot Test
- Spring Boot Testcontainers

Servlet-specific classes such as `jakarta.servlet.http.HttpServletResponse`
belong only in servlet-capable configuration. Reactive Gateway security must
not import servlet-only infrastructure.

---

# Spring Cloud

Spring Cloud 2025.0.2 is managed through the Spring Cloud BOM.

Implemented infrastructure:

- Spring Cloud Config Server
- Netflix Eureka Discovery Server and clients
- Spring Cloud Gateway

OpenFeign may be introduced only when an accepted service contract requires
synchronous service-to-service communication. A Feign client must not weaken
service ownership or become a substitute for an event-driven workflow.

---

# Persistence

## Database

MySQL 8 is the authoritative relational database platform.

Each business service owns its schema. The architecture forbids:

- Shared business schemas
- Cross-service table access
- Cross-database foreign keys
- One service using another service's repository or entity

UUID identifiers are stored using the accepted binary UUID representation.
UUIDv7 is the platform strategy for new entity and event identifiers.

## ORM

Spring Data JPA and Hibernate provide relational persistence.

Rules:

- Entities belong to exactly one service.
- Optimistic or pessimistic locking must match the domain concurrency rule.
- Repository integration tests must use a database that preserves production
  constraints where those constraints are under test.
- Hibernate schema generation must not replace Flyway in managed environments.

## Database Migration

Flyway 12.0.0 owns schema evolution.

Migration policy:

- Never modify an already-applied migration in a shared environment.
- Add a new versioned migration for schema changes.
- Verify indexes, foreign keys, unique constraints, and UUID column types.
- Treat Flyway checksum failures as migration-history problems, not as a reason
  to silently disable validation.

---

# Security and Identity

Spring Security provides authentication and authorization foundations.

The accepted R25 architecture is:

- User Service integrates Spring Authorization Server.
- User Service is the authoritative OAuth2 and OpenID Connect issuer.
- Gateway and protected business services are OAuth2 Resource Servers.
- `common-security` contains only reusable Resource Server mechanics.

Approved OAuth2 grants:

- Authorization Code with PKCE
- Refresh Token
- Client Credentials for approved service clients

Resource Owner Password Credentials must not be implemented.

Access-token contract:

| Property                | Requirement                                 |
| ----------------------- | ------------------------------------------- |
| Format                  | JWT                                         |
| Signature               | RS256                                       |
| RSA key size            | At least 3072 bits                          |
| Required audience       | `cinema-api`                                |
| Lifetime                | 15 minutes                                  |
| Validation              | Signature, issuer, timestamps, and audience |
| Public key distribution | User Service JWK Set                        |

Refresh tokens are opaque, revocable, rotated on use, and expire no later than
30 days after issuance.

`common-security` must not contain token issuance, signing private keys, OAuth2
client persistence, consent persistence, or refresh-token persistence. Detailed
rules are defined by `docs/08_SECURITY.md` and ADR-013.

---

# Messaging

Apache Kafka is the asynchronous messaging platform.

Implemented technical foundations include:

- Producer and consumer configuration
- Event serialization
- Retry and error handling
- Transactional Outbox publishing

Architecture patterns:

- Event-driven communication
- Saga choreography
- Transactional Outbox
- Idempotent consumers
- Versioned event envelopes

Kafka delivery alone does not guarantee exactly-once business processing.
State-changing consumers must implement idempotency.

---

# Cache and Distributed Locking

Redis provides distributed coordination and may later support approved caching
or short-lived data.

Redisson 3.50.0 implements the distributed-lock abstraction.

Inventory Service owns seat-locking policy and seat state transitions. Redis
locks complement database constraints and transactions; they do not replace
them.

---

# Search

Elasticsearch 8.18.1 is integrated through `common-search`.

Search indexes are read-optimized projections. MySQL and the owning service
remain authoritative. Index updates must be replayable and resilient to
duplicate delivery.

---

# Object Mapping

MapStruct 1.6.3 is the approved compile-time mapping technology.

Rules:

- Do not use `BeanUtils` for domain/API mapping.
- Avoid reflection-based mapping.
- Business services own their mapper interfaces.
- Shared mapper configuration may live in `common-mapper`.
- Generated implementations must be registered as Spring beans when required.

---

# JSON

Jackson is the JSON technology supplied by Spring Boot and standardized through
`common-jackson`.

Requirements:

- ISO-8601 date and time representation
- Java time support
- Stable event serialization
- No token, credential, or signing-key serialization into logs
- Backward-compatible event changes according to the event-version policy

---

# API Documentation

OpenAPI 3 and Swagger UI are provided through springdoc-openapi 2.8.9 and
`common-openapi`.

Each service documents:

- Endpoints and operations
- Request and response schemas
- OAuth2 security requirements
- Error codes
- Pagination and validation behavior

---

# Logging and Observability

SLF4J and Logback provide application logging. `common-logging` provides
correlation-ID propagation and shared logging support.

Micrometer Tracing with the OpenTelemetry bridge provides tracing foundations.
OTLP export is available through `common-tracing`.

Observability rules:

- Propagate correlation and trace identifiers.
- Never log bearer tokens, passwords, refresh tokens, or private keys.
- Do not rely on logs as the sole audit trail for privileged security actions.

Prometheus, Grafana, Zipkin, and production dashboards remain deployment work
until explicitly implemented and verified.

---

# File Storage

`common-storage` provides a storage abstraction and a MinIO implementation using
MinIO Java SDK 8.5.17.

MinIO is S3-compatible, but production object-storage deployment and
service-specific authorization policies must be completed and verified before
the capability is considered operational.

---

# Testing

The testing stack includes:

- JUnit Jupiter
- Mockito through Spring Boot Test
- Spring Security Test
- Spring Boot Test
- Testcontainers 1.21.3
- MySQL, Kafka, and Redis integration-test foundations
- MockMvc for servlet controller and security tests
- Reactor Test for reactive Gateway behavior

Testing policy:

- Unit tests verify isolated business and conversion rules.
- Slice tests verify controller and security boundaries.
- Integration tests verify database migrations, constraints, repositories,
  concurrency, messaging, and application wiring.
- Root `mvn clean verify` is mandatory before a roadmap checkpoint is closed.
- An `ApplicationContext failure threshold` message is normally secondary;
  investigate the first context-loading exception in the reports.

---

# Containerization and Deployment

Docker and Docker Compose are approved deployment technologies, but the current
repository does not yet contain a complete verified container deployment for
the entire platform.

Do not document Docker as operational merely because it is planned. Container
images, Compose topology, secret injection, health checks, and startup ordering
must be implemented and verified through the deployment roadmap.

---

# Architecture Patterns

The approved architecture uses:

- Microservices
- Database per service
- API Gateway
- Centralized configuration
- Service discovery
- Event-driven communication
- Saga choreography
- Transactional Outbox
- Idempotent consumers
- Distributed locking
- OAuth2 Authorization Server and Resource Servers
- OpenID Connect
- Domain ownership boundaries

DDD terminology may be used where it improves ownership and domain clarity. It
must not be used to justify cross-service Java dependencies or shared business
entities.

---

# Technology Adoption Rule

A technology is considered implemented only when all applicable conditions are
met:

- The dependency and configuration exist.
- The owning module uses it.
- Relevant tests pass.
- Operational configuration is documented.
- Root `mvn clean verify` passes.

Listing a technology in this document does not authorize bypassing the roadmap,
an accepted ADR, or a service ownership boundary.

---

# Related Documentation

```text
docs/02_ARCHITECTURE.md
docs/04_MODULES.md
docs/08_SECURITY.md
docs/10_ROADMAP.md
docs/12_DEPENDENCY_RULES.md
docs/14_DEPLOYMENT.md
docs/decisions/ADR-013-spring-authorization-server.md
```
