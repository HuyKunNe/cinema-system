# Deployment Guide

Version: R25

---

# Purpose

This document describes the deployment and local startup model currently
supported by the Cinema Booking System repository.

Current baseline:

- R1-R24 are completed.
- R25.1–R25.8 are completed.
- R25.9 OAuth2 clients and grant types is active.
- User Service has its Spring Authorization Server/OIDC foundation and
  controlled JDBC-backed registered-client configuration.

A placeholder Maven module or Config Server file does not make a service
deployable. Only completed roadmap checkpoints define operational capability.

---

# Current Deployment Status

| Component            | Round    | Status                                                            |
| -------------------- | -------- | ----------------------------------------------------------------- | --- |
| Config Server        | R20      | Implemented                                                       |
| Discovery Server     | R21      | Implemented                                                       |
| API Gateway          | R25.13.2 | Reactive Resource Server security and explicit routes implemented |     |
| Movie Service        | R23      | Implemented                                                       |
| Inventory Service    | R24      | Implemented; protected runtime requires issuer/JWK configuration  |
| User Service         | R25      | R25.8 foundation implemented; R25.9 client/grant work in progress |
| Booking Service      | R26      | Not implemented                                                   |
| Payment Service      | R27      | Not implemented                                                   |
| Notification Service | R28      | Not implemented                                                   |

The Gateway validates bearer access tokens as a reactive OAuth2 Resource Server.
It validates signature, issuer, timestamps and the required `cinema-api` audience
before protected API requests are routed.

Gateway validation does not replace service-level authentication or authorization.
Direct access to protected services must remain network-restricted or equivalently
secured.

---

# Local Requirements

- Java 21
- Maven 3.9 or later
- Git
- MySQL 8
- Redis when exercising distributed-lock functionality
- Kafka when exercising event-driven or Outbox publication functionality

Docker and Docker Compose are approved technologies, but the repository does
not currently contain a verified root Compose manifest. Therefore:

```bash
docker compose up
```

is not yet an authoritative full-system startup command.

The first approved Docker adoption will be introduced in a later checkpoint for
local Kafka infrastructure. Until that checkpoint is implemented and verified,
Kafka must be started through the developer's existing local installation or
another explicitly documented development environment. Docker support for
Kafka must not be interpreted as completed containerization of the Spring
services, MySQL, Redis, or the full platform.

---

# Configuration Model

Config Server uses its native profile and reads committed development
configuration from:

```text
infrastructure/config-service/src/main/resources/config-repo
```

Applications that support centralized configuration use:

```text
CONFIG_SERVER_URL=http://localhost:8888
```

The localhost value is a development default. Override it when applications run
on different hosts or inside containers.

Config files must contain secret references, not real credentials, tokens,
client secrets, or signing private keys.

---

# Required Environment Variables

## Infrastructure

| Variable                       | Required | Development default             |
| ------------------------------ | -------- | ------------------------------- |
| `CONFIG_SERVER_PORT`           | No       | `8888`                          |
| `CONFIG_SERVER_URL`            | No       | `http://localhost:8888`         |
| `DISCOVERY_SERVICE_PORT`       | No       | `8761`                          |
| `GATEWAY_SERVICE_PORT`         | No       | `8080`                          |
| `EUREKA_SERVER_URL`            | No       | `http://localhost:8761/eureka/` |
| `SPRING_PROFILES_ACTIVE`       | No       | `native` for Config Server      |
| `TRACING_SAMPLING_PROBABILITY` | No       | `1.0`                           |
| `OTEL_EXPORTER_ENABLED`        | No       | `false`                         |

Some service configurations use `EUREKA_URL`, while infrastructure
configuration uses `EUREKA_SERVER_URL`. Preserve the variable expected by the
target application until configuration naming is intentionally standardized.

## API Gateway

| Variable                      | Required                  | Development default             |
| ----------------------------- | ------------------------- | ------------------------------- |
| `GATEWAY_SERVICE_PORT`        | No                        | `8080`                          |
| `EUREKA_SERVER_URL`           | No                        | `http://localhost:8761/eureka/` |
| `CINEMA_AUTH_ISSUER`          | Yes for protected runtime | None                            |
| `CINEMA_AUTH_JWK_SET_URI`     | Yes for protected runtime | None                            |
| `CINEMA_AUTH_AUDIENCE`        | No                        | `cinema-api`                    |
| `CORS_ALLOWED_ORIGIN_PATTERN` | No                        | `http://localhost:*`            |

The issuer must exactly match the `iss` claim produced by User Service. The JWK
Set URI must expose the corresponding public verification keys. Gateway must
never receive or store the Authorization Server private signing key.

Automatic Gateway Discovery Locator routing is disabled. External service paths
must be declared explicitly in `gateway-service.yml`.

## Movie Service

| Variable             | Required | Development default               |
| -------------------- | -------- | --------------------------------- |
| `MOVIE_SERVICE_PORT` | No       | `8081`                            |
| `MOVIE_DB_URL`       | No       | Local MySQL `cinema_movie_db` URL |
| `MOVIE_DB_USERNAME`  | Yes      | None                              |
| `MOVIE_DB_PASSWORD`  | Yes      | None                              |
| `APP_VERSION`        | No       | `1.0.0-SNAPSHOT`                  |

## Inventory Service

| Variable                  | Required                  | Development default                   |
| ------------------------- | ------------------------- | ------------------------------------- |
| `INVENTORY_SERVICE_PORT`  | No                        | `8083`                                |
| `INVENTORY_DB_URL`        | No                        | Local MySQL `cinema_inventory_db` URL |
| `INVENTORY_DB_USERNAME`   | Yes                       | None                                  |
| `INVENTORY_DB_PASSWORD`   | Yes                       | None                                  |
| `EUREKA_URL`              | No                        | `http://localhost:8761/eureka/`       |
| `CINEMA_AUTH_ISSUER`      | Yes for protected runtime | None                                  |
| `CINEMA_AUTH_JWK_SET_URI` | Yes for protected runtime | None                                  |
| `CINEMA_AUTH_AUDIENCE`    | No                        | `cinema-api`                          |

`CINEMA_AUTH_ISSUER` and `CINEMA_AUTH_JWK_SET_URI` must refer to the same
trusted User Service environment. They must not point to arbitrary public test
issuers in a production-capable profile.

## Future User Service

The exact environment-variable names become authoritative only when the User
Service configuration is implemented. The deployment must eventually provide:

- User database credentials
- Environment-specific issuer URI
- Public endpoint and JWK Set endpoint configuration
- OAuth2 client credentials through secret management
- RSA signing-key references
- Key identifiers and rotation state
- MFA provider or credential configuration for privileged accounts

Do not store signing private-key material in the Config Server classpath
repository.

---

# Development Environment Examples

Bash:

```bash
export MOVIE_DB_USERNAME=cinema_movie
export MOVIE_DB_PASSWORD='<your-password>'
export INVENTORY_DB_USERNAME=cinema_inventory
export INVENTORY_DB_PASSWORD='<your-password>'
export CINEMA_AUTH_ISSUER='https://identity.cinema.local'
export CINEMA_AUTH_JWK_SET_URI='https://identity.cinema.local/oauth2/jwks'
```

PowerShell:

```powershell
$env:MOVIE_DB_USERNAME = "cinema_movie"
$env:MOVIE_DB_PASSWORD = "<your-password>"
$env:INVENTORY_DB_USERNAME = "cinema_inventory"
$env:INVENTORY_DB_PASSWORD = "<your-password>"
$env:CINEMA_AUTH_ISSUER = "https://identity.cinema.local"
$env:CINEMA_AUTH_JWK_SET_URI = "https://identity.cinema.local/oauth2/jwks"
```

These values are examples only. Do not use `root` as the documented application
database account.

---

# Default Ports

| Component            |                              Port |
| -------------------- | --------------------------------: |
| API Gateway          |                            `8080` |
| Movie Service        |                            `8081` |
| User Service         |                   `8082` reserved |
| Inventory Service    |                            `8083` |
| Booking Service      |                   `8084` reserved |
| Payment Service      |                   `8085` reserved |
| Notification Service |                   `8086` reserved |
| Config Server        |                            `8888` |
| Discovery Server     |                            `8761` |
| Kafka                | `9092` conventional local default |
| Redis                | `6379` conventional local default |
| MySQL                | `3306` conventional local default |

Reserved service ports do not imply that the corresponding service is
implemented.

---

# Database Preparation

Create a dedicated database and least-privileged application account for each
service that owns persisted data.

Current databases:

```text
cinema_movie_db
cinema_inventory_db
```

Example MySQL preparation must be adapted to the local credential policy:

```sql
CREATE DATABASE cinema_movie_db;
CREATE DATABASE cinema_inventory_db;
```

Do not use one application account with access to every service database in
production.

Flyway owns schema evolution. Hibernate validates the result:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    validate-on-migrate: true
```

Never edit an applied migration in a shared environment. Add a new versioned
migration.

---

# Build and Verification

Run the final verification from the repository root:

```bash
mvn clean verify
```

Build one service and all required upstream modules:

```bash
mvn -pl services/movie-service -am clean package
```

```bash
mvn -pl services/inventory-service -am clean package
```

Focused tests remain useful during development:

```bash
mvn -pl services/inventory-service -am clean test
```

A focused module success does not replace root `mvn clean verify`. Generated
MapStruct implementations and test discovery must work from a clean root
reactor build.

---

# Local Startup Order

Start only the dependencies required by the selected flow.

Infrastructure dependencies:

1. MySQL
2. Redis when required
3. Kafka when required

Spring applications:

1. Config Server
2. Discovery Server
3. User Service Authorization Server, after it is implemented
4. Movie Service and Inventory Service
5. API Gateway

Gateway may start earlier for route-development work, but end-to-end readiness
requires its target services to be registered.

Until User Service is implemented, authenticated Inventory runtime flows require
an explicitly approved development issuer/JWK source. Unit and integration tests
may use test-only security configuration; that configuration must not leak into
production profiles.

---

# Run Config Server

```bash
mvn -pl infrastructure/config-service -am spring-boot:run
```

Verify:

```text
http://localhost:8888/actuator/health
```

Do not continue until the health response is successful.

---

# Run Discovery Server

```bash
mvn -pl infrastructure/discovery-service -am spring-boot:run
```

Verify health:

```text
http://localhost:8761/actuator/health
```

Development Eureka dashboard:

```text
http://localhost:8761
```

---

# Run Movie Service

Set database credentials, then run:

```bash
mvn -pl services/movie-service -am spring-boot:run
```

Verify:

```text
http://localhost:8081/actuator/health
http://localhost:8081/swagger-ui.html
```

Confirm:

- Config Server configuration loaded as expected.
- Flyway migrations succeeded.
- Hibernate schema validation succeeded.
- Movie Service registered with Discovery Server.
- Gateway routes reach Movie Service when Gateway is running.

---

# Run Inventory Service

Set database and security trust configuration, then run:

```bash
mvn -pl services/inventory-service -am spring-boot:run
```

Verify:

```text
http://localhost:8083/actuator/health
http://localhost:8083/swagger-ui.html
```

Confirm:

- Inventory database connectivity.
- Flyway migration success.
- Hibernate schema validation.
- Discovery registration.
- JWT issuer, JWK Set, and `cinema-api` audience configuration.
- Public ShowSeat endpoints behave as documented.
- Management operations require `inventory:manage`.
- Hold, book, and release operations require `ROLE_SERVICE`.
- Standard JSON `401` and `403` responses are returned.

Inventory Service owns ShowSeat transitions and concurrency control. Booking
Service must never access its tables or locks.

---

# Run API Gateway

Supply the trusted Authorization Server configuration before starting Gateway.

Bash:

```bash
export CINEMA_AUTH_ISSUER='http://localhost:8082'
export CINEMA_AUTH_JWK_SET_URI='http://localhost:8082/oauth2/jwks'
export CINEMA_AUTH_AUDIENCE='cinema-api'
```

mvn -pl infrastructure/gateway-service -am spring-boot:run

```PowerShell:
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
./mvnw -pl infrastructure/gateway-service -am spring-boot:run
```

Verify the public health endpoint: http://localhost:8080/actuator/health

Protected API requests require a valid bearer access token. The token must have:
-an RS256 signature trusted through the configured JWK Set;
-an issuer exactly matching CINEMA_AUTH_ISSUER;
-the cinema-api audience;
-valid expiration and not-before timestamps.

The Gateway forwards an accepted bearer token to the selected downstream service.
The downstream service must independently validate the token and enforce its owned
authorization rules.

---

# User Service Deployment Requirements

This section combines the implemented R25.8 foundation with requirements that
remain for later R25 checkpoints.

User Service hosts Spring Authorization Server with an environment-specific
issuer. Public JWK publication and production signing-key readiness remain
R25.10 work.

Required production characteristics:

- TLS on every external identity endpoint.
- Stable issuer URI matching the `iss` claim exactly.
- RS256 signing with RSA keys of at least 3072 bits.
- Private keys readable only by the User Service identity runtime.
- Public JWK Set containing active verification keys.
- Overlapping old and new public keys during safe rotation.
- Authorization Code with PKCE for interactive clients.
- Rotated, revocable, opaque refresh tokens.
- Controlled Client Credentials for service clients.
- MFA enforcement for privileged production users.
- Durable audit events for privileged identity changes.

Resource Owner Password Credentials must not be enabled.

Key rotation must be rehearsed before production. Removing an old public key
before all access tokens signed by it expire can break every Resource Server.

---

# Health and Availability

Spring Boot Actuator is the health-check interface.

Standard endpoints when enabled:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
/actuator/health/readiness
/actuator/health/liveness
```

Do not assume `/health`, `/readiness`, or `/liveness` aliases exist unless the
application explicitly maps them.

Detailed health output should be restricted in non-development environments.
Identity-service readiness must include database and key availability without
exposing private key material or client secrets.

---

# Deployment Verification

For every deployable application:

1. Confirm Java and artifact versions.
2. Confirm required environment variables and secret references.
3. Confirm Config Server reachability where used.
4. Confirm required external dependencies.
5. Start the application.
6. Confirm Flyway success where applicable.
7. Confirm Hibernate schema validation where applicable.
8. Confirm Actuator readiness and liveness.
9. Confirm Discovery registration where applicable.
10. Confirm Gateway routing where applicable.
11. Confirm logs contain no secrets or bearer tokens.

For protected Resource Servers, additionally verify:

1. Valid issuer, signature, timestamp, and audience are accepted.
2. Wrong issuer is rejected.
3. Wrong or missing `cinema-api` audience is rejected.
4. Expired tokens are rejected.
5. Unknown signing-key IDs fail safely.
6. Roles and permissions map to expected authorities.
7. `401` and `403` responses follow the shared API contract.

For event-driven services, additionally verify Kafka connectivity, Outbox
publication, retry behavior, idempotent duplicate processing, and atomic local
state/processing-record transactions.

---

# Observability

The current shared baseline includes:

- Spring Boot Actuator
- Micrometer
- Prometheus endpoint support
- OpenTelemetry-compatible tracing
- Correlation identifiers

OTLP export is disabled by default:

```text
OTEL_EXPORTER_ENABLED=false
```

Prometheus, Grafana, a tracing backend, alerting, and centralized log aggregation
are not yet complete repository-managed deployment infrastructure.

Security logs and metrics must not expose access tokens, refresh tokens, client
secrets, passwords, signing private keys, or complete sensitive claims.

---

# Scaling Constraints

Stateless instances may be scaled horizontally only after multi-instance
database, Kafka, Outbox, lock, and cache behavior is verified.

Inventory scaling must preserve:

- Database-enforced atomic ShowSeat transitions.
- Pessimistic locking where currently required.
- Idempotent event processing.
- Single logical ownership of `show_seats`.

Future User Service scaling must preserve:

- One authoritative issuer value.
- Consistent registered-client and consent state.
- Shared durable refresh-token/revocation state.
- Safe access to active signing keys.
- Consistent JWK Set publication during rotation.

---

# Production Deployment Status

The repository does not yet provide a complete verified production deployment.

Not currently implemented as repository-owned operational capability:

- Production container images for all services
- Root Docker Compose topology
- Kubernetes manifests or Helm charts
- Ingress and certificate automation
- Secret-manager integration
- Automated signing-key rotation
- Multi-availability-zone deployment
- Zero-downtime rollout
- Redis or Kafka production clusters
- Automated database backup and restore
- Centralized production logging and alerting

Do not document these items as available until implementation, recovery tests,
security review, and operational verification are complete.

---

# R25 Deployment Gate

R25 identity runtime cannot be considered deployable until:

- User Service module and database migrations are implemented.
- Spring Authorization Server configuration is implemented.
- Approved grants and registered clients are tested.
- Issuer metadata and public JWK Set are stable.
- Signing private-key custody and rotation are verified.
- Access-token issuer, signature, timestamp, and audience validation pass across
  Gateway and protected services.
- Refresh-token rotation, reuse detection, revocation, and expiration pass.
- Privileged MFA and audit requirements pass.
- Standard `401` and `403` contracts pass.
- No secret or private key is committed or logged.
- Root `mvn clean verify` passes.
- Deployment and security documentation are synchronized.

R25.1 completion alone does not make User Service deployable. R25.2 documents
the contract; R25.3 and later checkpoints implement and verify it.

---

# Related Documentation

```text
docs/02_ARCHITECTURE.md
docs/03_TECHNOLOGY_STACK.md
docs/04_MODULES.md
docs/08_SECURITY.md
docs/10_ROADMAP.md
docs/11_CHANGELOG.md
docs/12_DEPENDENCY_RULES.md
docs/decisions/ADR-013-spring-authorization-server.md
```
