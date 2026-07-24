# Deployment Guide

Version: R24

---

# Purpose

This document describes the deployment and local startup model currently
supported by the Cinema Booking System repository.

The current active milestone is:

> **R24 — Inventory Service Implementation**

Implemented runtime applications:

- Config Server
- Discovery Server
- API Gateway
- Movie Service

Inventory Service is under implementation. User, Booking, Payment and
Notification services must not be treated as deployable until their respective
rounds are complete.

---

# Current Deployment Status

| Component            | Round | Status      |
| -------------------- | ----- | ----------- |
| Config Server        | R20   | DONE        |
| Discovery Server     | R21   | DONE        |
| API Gateway          | R22   | DONE        |
| Movie Service        | R23   | DONE        |
| Inventory Service    | R24   | IN PROGRESS |
| User Service         | R25   | NOT STARTED |
| Booking Service      | R26   | NOT STARTED |
| Payment Service      | R27   | NOT STARTED |
| Notification Service | R28   | NOT STARTED |

Do not deploy a service merely because its placeholder module exists in the
Maven reactor.

---

# Local Requirements

- Java 21
- Maven 3.9 or later
- Git
- MySQL 8
- Redis
- Kafka

Docker and Docker Compose may be used to run infrastructure after a verified
Compose manifest is added to the repository. The current repository does not
contain a root Compose manifest, so `docker compose up` is not currently a
repository-supported startup command.

---

# Configuration Model

Config Server uses the native Spring profile and reads configuration from:

```text
infrastructure/config-service/src/main/resources/config-repo
```

All applications that import Config Server use:

```text
CONFIG_SERVER_URL=http://localhost:8888
```

The localhost value is the development default. Override it when applications
run on different hosts or inside containers.

Sensitive values must be provided through environment variables. Do not commit
database passwords, tokens or other secrets to:

- `application.yml`
- Config Server configuration
- source code
- deployment documentation

---

# Required Environment Variables

## Infrastructure applications

| Variable                     | Required | Development default       |
| ---------------------------- | -------- | ------------------------- |
| `CONFIG_SERVER_PORT`         | No       | `8888`                    |
| `CONFIG_SERVER_URL`          | No       | `http://localhost:8888`   |
| `DISCOVERY_SERVICE_PORT`     | No       | `8761`                    |
| `GATEWAY_SERVICE_PORT`       | No       | `8080`                    |
| `EUREKA_SERVER_URL`          | No       | `http://localhost:8761/eureka/` |
| `SPRING_PROFILES_ACTIVE`     | No       | `native`                  |
| `TRACING_SAMPLING_PROBABILITY` | No    | `1.0`                     |
| `OTEL_EXPORTER_ENABLED`      | No       | `false`                   |

## Movie Service

| Variable                | Required | Development default |
| ----------------------- | -------- | ------------------- |
| `MOVIE_SERVICE_PORT`    | No       | `8081`              |
| `MOVIE_DB_URL`          | No       | Local MySQL URL for `cinema_movie_db` |
| `MOVIE_DB_USERNAME`     | Yes      | None                |
| `MOVIE_DB_PASSWORD`     | Yes      | None                |
| `APP_VERSION`           | No       | `1.0.0-SNAPSHOT`    |

Example local shell values:

```bash
export MOVIE_DB_USERNAME=cinema_movie
export MOVIE_DB_PASSWORD='<your-password>'
```

Do not use `root` as the documented application credential.

## Inventory Service

The committed R24 configuration currently defines:

| Variable                 | Required | Development default |
| ------------------------ | -------- | ------------------- |
| `INVENTORY_SERVICE_PORT` | No       | `8083`              |

Database, Kafka, Redis and service-registration variables must be documented
here only after their R24 configuration has been implemented and verified.

---

# Default Ports

| Component         | Port   |
| ----------------- | ------ |
| API Gateway       | `8080` |
| Movie Service     | `8081` |
| Inventory Service | `8083` |
| Config Server     | `8888` |
| Discovery Server  | `8761` |
| Kafka             | `9092` |
| Redis             | `6379` |
| MySQL             | `3306` |

These are development defaults. Environment variables may override application
ports.

Kafka UI is not part of the current verified deployment configuration and
therefore has no authoritative port in this document.

---

# Database Preparation

Create a dedicated database and least-privileged application account for each
service that owns persisted data.

For Movie Service, the current database name is:

```text
cinema_movie_db
```

Flyway owns schema evolution. Hibernate must validate the schema and must not
create or update it automatically:

```yaml
spring:
    jpa:
        hibernate:
            ddl-auto: validate
    flyway:
        enabled: true
        validate-on-migrate: true
```

Do not edit an applied Flyway migration. Add a new versioned migration for every
schema change.

---

# Build

Run the full repository verification from the repository root:

```bash
mvn clean verify
```

To build Movie Service and every required upstream module:

```bash
mvn -pl services/movie-service -am clean package
```

During R24, build Inventory Service and every required upstream module with:

```bash
mvn -pl services/inventory-service -am clean test
```

The Inventory Service package must not be considered deployable until all R24
exit criteria pass.

---

# Local Startup Order

Start infrastructure dependencies before Spring applications:

1. MySQL
2. Redis
3. Kafka

Then start the applications in this order:

1. Config Server
2. Discovery Server
3. API Gateway
4. Movie Service
5. Inventory Service, only while developing or verifying R24

Config Server must become healthy before applications that import centralized
configuration are started.

Discovery Server must become healthy before registration-dependent application
verification.

---

# Run Config Server

From the repository root:

```bash
mvn -pl infrastructure/config-service -am spring-boot:run
```

Verify:

```text
http://localhost:8888/actuator/health
```

The response must report a healthy application before continuing.

---

# Run Discovery Server

From the repository root:

```bash
mvn -pl infrastructure/discovery-service -am spring-boot:run
```

Verify:

```text
http://localhost:8761/actuator/health
```

The Eureka dashboard is available at:

```text
http://localhost:8761
```

---

# Run API Gateway

From the repository root:

```bash
mvn -pl infrastructure/gateway-service -am spring-boot:run
```

Verify:

```text
http://localhost:8080/actuator/health
```

Gateway route verification must use the routes defined in centralized
configuration. Do not bypass the Gateway when validating a public API flow.

---

# Run Movie Service

Set the required database credentials:

```bash
export MOVIE_DB_USERNAME=cinema_movie
export MOVIE_DB_PASSWORD='<your-password>'
```

Then run:

```bash
mvn -pl services/movie-service -am spring-boot:run
```

Verify the service directly:

```text
http://localhost:8081/actuator/health
```

Verify all of the following:

- Config Server configuration was loaded.
- Flyway migrations completed successfully.
- Hibernate schema validation passed.
- Movie Service registered with Discovery Server.
- The Gateway route reaches Movie Service.

Swagger UI is available in development when enabled by the service
configuration:

```text
http://localhost:8081/swagger-ui.html
```

---

# Run Inventory Service During R24

Inventory Service is still under implementation. Start it only after its
bootstrap application and required runtime configuration exist.

Run:

```bash
mvn -pl services/inventory-service -am spring-boot:run
```

The current configured development port is:

```text
8083
```

Before R24 is marked complete, verify:

- Inventory database connectivity
- Flyway migration success
- Hibernate schema validation
- Discovery registration
- Gateway routing
- Redis connectivity
- Kafka connectivity
- Outbox publishing
- Idempotent event processing
- Atomic `ShowSeat` transitions
- Concurrent reservation of the same seat

The approved normal `ShowSeat` transitions are:

```text
AVAILABLE → HELD → BOOKED
```

The approved release transition is:

```text
HELD → AVAILABLE
```

Redis provides coordination only. Database conditional updates or database
locking provide the final consistency guarantee.

---

# Health and Availability

Spring Boot Actuator is the health-check interface.

Standard endpoints:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
```

Readiness and liveness health groups are exposed through Actuator when enabled:

```text
/actuator/health/readiness
/actuator/health/liveness
```

Do not use `/readiness`, `/liveness` or `/health` as standalone paths unless an
application explicitly maps them.

Detailed health output may require authorization.

---

# Deployment Verification

For every deployable application:

1. Confirm the expected Java version.
2. Confirm required environment variables are present.
3. Confirm Config Server is reachable.
4. Confirm external dependencies are reachable.
5. Start the application.
6. Confirm Flyway migration success where applicable.
7. Confirm Hibernate schema validation success where applicable.
8. Confirm Actuator health.
9. Confirm Discovery registration.
10. Confirm Gateway routing where applicable.
11. Confirm logs contain no secrets.

For event-driven services, additionally verify:

1. Kafka producer and consumer connectivity.
2. Transactional outbox publishing.
3. Retry behavior.
4. Idempotent duplicate-event handling.
5. Domain changes and processed-event records commit atomically.

---

# Observability

The current shared observability baseline includes:

- Spring Boot Actuator
- Micrometer
- Prometheus endpoint exposure
- OpenTelemetry-compatible tracing configuration
- Correlation identifiers in logs

OTLP export is disabled by default:

```text
OTEL_EXPORTER_ENABLED=false
```

Enable an exporter only when a verified collector endpoint and deployment
configuration exist.

Prometheus, Grafana, a tracing backend and centralized log aggregation are not
yet defined as deployable repository infrastructure.

---

# Scaling Constraints

Stateless application instances may be scaled horizontally only after their
database, Kafka consumer-group, outbox and distributed-lock behavior have been
verified with multiple instances.

Inventory Service scaling must preserve:

- ordered Redis seat-lock acquisition
- database-enforced atomic seat-state transitions
- idempotent event processing
- single logical ownership of `show_seats`

Booking Service must never acquire Inventory seat locks or directly update
Inventory tables.

---

# Production Deployment

A production container image, Kubernetes manifests, Ingress configuration,
secret-management integration and production infrastructure topology are not
yet implemented in the repository.

Do not describe the following as current deployment capabilities:

- Kubernetes deployment
- multi-availability-zone deployment
- zero-downtime rollout
- Redis Cluster
- Kafka Cluster
- automated database backup and restore
- centralized ELK or OpenSearch logging

These require separate approved implementation and verification work.

---

# R24 Exit Gate

Inventory Service becomes deployable only when:

- all approved Inventory domain behavior is implemented
- Flyway migrations pass
- Hibernate schema validation passes
- unit tests pass
- integration tests pass
- concurrency tests pass
- Redis and Kafka integration pass
- outbox behavior passes
- idempotent processing passes
- security requirements pass
- `mvn clean verify` passes from the repository root
- deployment configuration is complete
- documentation is synchronized

Do not start R25 User Service before all R24 exit criteria defined in
`docs/10_ROADMAP.md` pass.
