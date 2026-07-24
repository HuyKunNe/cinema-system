Project Roadmap
Version: R23

Current target: R23 Movie Service stabilization

Purpose
This roadmap defines the approved implementation order for Cinema Booking
System.

It is the source of truth for:

round status;

round scope;

dependencies between rounds;

completion criteria;

verification requirements;

the boundary between implemented and planned capabilities.

The roadmap does not mark a capability as complete merely because:

a Maven module exists;

an interface or placeholder class exists;

code compiles in isolation;

documentation describes the intended behavior;

a happy-path request succeeds manually.

A round is complete only after its exit criteria and the shared quality gates are
satisfied.

Status Legend
Symbol	Status	Meaning
✅	Completed	Scope is implemented and accepted for the completed round
🚧	Stabilization	Main implementation exists, but one or more exit criteria remain
⏳	Planned	Approved future work; implementation has not started
⛔	Blocked	Work cannot continue until a named dependency or decision is resolved
Only one business-service round should be the active implementation target at a
time unless the roadmap is explicitly revised.

Fixed Technical Baseline
All rounds must preserve the approved project baseline:

Java 21
Spring Boot 3.5.4
Maven Multi Module
Spring Data JPA
Hibernate 6
MySQL 8
Flyway
Redis
Redisson
Kafka
Spring Cloud Gateway
Spring Security
JWT / OAuth2 Resource Server
OpenAPI
MapStruct
Jackson with ISO-8601 timestamps
JUnit 5
Testcontainers
Docker Compose
Micrometer Tracing
OpenTelemetry
Elasticsearch
Architecture and implementation must preserve:

database per service;

service ownership of its domain data;

Inventory Service ownership of show_seats;

Event-Driven Architecture;

Saga Choreography;

Transactional Outbox;

Idempotent Consumer;

UUID v7 for entity and event identifiers;

no Lombok in common modules;

shared response, exception, validation, Jackson, logging, mapping, security,
Kafka and tracing conventions;

no committed production secrets.

Changing this baseline requires an accepted Architecture Decision Record before
implementation.

Global Round Lifecycle
Each round follows this lifecycle:

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
Starting the next round does not convert an unfinished round into a completed
round.

Shared Definition of Done
Every implementation round must satisfy all applicable requirements below.

Build
The module builds using the parent Maven configuration.

Dependency versions are managed centrally.

No undeclared or duplicate version is introduced without justification.

mvn clean verify passes from the repository root.

git diff --check passes.

Code
Package and module boundaries follow docs/04_MODULES.md.

Dependency direction follows docs/12_DEPENDENCY_RULES.md.

External input uses operation-specific DTOs.

Bean Validation and business validation are both applied where required.

JPA entities are not exposed as API or event contracts.

Money uses BigDecimal.

Time values and expiration decisions use trusted server time.

Public errors use the shared exception and response model.

Logs do not expose credentials, tokens or sensitive payloads.

Data
The service owns its database and Flyway migrations.

No service reads or writes another service's tables.

Schema changes are forward migrations; applied migrations are not silently
rewritten.

Constraints and indexes support the business invariants.

Entity, schema and repository mappings agree.

Security
Authentication and authorization match docs/08_SECURITY.md.

Ownership is derived from the validated principal, not an arbitrary request
field.

Secrets are supplied through approved external configuration.

CORS, CSRF, Actuator and OpenAPI exposure are environment appropriate.

Security tests cover applicable 401, 403, ownership and administrative
cases.

Events
Event names, producers, consumers and payloads match
docs/07_EVENT_CATALOG.md.

Business state and Outbox records are written in the same local transaction.

Published events contain eventId, event type, version, aggregate identity,
occurrence time and correlation information as defined by the event contract.

Consumers validate contracts and process duplicate eventId values
idempotently.

At-least-once delivery is assumed; end-to-end exactly-once delivery is not
claimed.

Tests
Unit tests cover business rules and failure branches.

Controller security behavior is tested.

Persistence behavior uses integration tests where database semantics matter.

Kafka, Redis and MySQL integration uses Testcontainers where applicable.

Regression tests accompany bug fixes.

Documentation
The following files are updated when affected:

README.md
docs/00_PROJECT_CONTEXT.md
docs/01_AI_CONTEXT.md
docs/02_ARCHITECTURE.md
docs/03_TECHNOLOGY_STACK.md
docs/04_MODULES.md
docs/05_CODING_CONVENTIONS.md
docs/06_DATABASE_DESIGN.md
docs/07_EVENT_CATALOG.md
docs/08_SECURITY.md
docs/09_OUTBOX.md
docs/10_ROADMAP.md
docs/11_CHANGELOG.md
docs/12_DEPENDENCY_RULES.md
docs/13_SEQUENCE_DIAGRAMS.md
docs/14_DEPLOYMENT.md
docs/decisions/
Documentation must distinguish current implementation from future requirements.

Phase 1 — Foundation Layer
✅ R1 — Parent Project
Scope:

root Maven aggregator;

module hierarchy;

Java 21 compiler configuration;

Spring Boot 3.5.4 dependency management;

shared plugin and dependency versions;

repository-wide build conventions.

Outcome:

all project modules can inherit one consistent build baseline.

✅ R2 — common-core
Scope:

core constants and shared primitives;

UUID v7 support;

framework-independent foundational utilities.

Outcome:

domain-neutral primitives are available without creating service coupling.

✅ R3 — common-jpa
Scope:

base entity conventions;

UUID identifier integration;

created and updated timestamp auditing;

shared JPA auditing configuration.

Outcome:

persistence modules use one auditable entity baseline.

✅ R4 — common-exception
Scope:

business exception hierarchy;

shared error-code contract;

standard exception categories.

Outcome:

services express failures without leaking internal implementation details.

✅ R5 — common-response
Scope:

ApiResponse;

standardized error body;

validation error model;

pagination response and mapping.

Outcome:

synchronous APIs share one public response envelope.

✅ R6 — common-api
Scope:

global exception handling;

exception-to-status mapping;

response construction integration;

shared API constants and web-layer support.

Outcome:

services apply the common exception and response contracts consistently.

✅ R7 — common-validation
Scope:

reusable Bean Validation annotations and validators;

shared validation messages;

validation test support.

Outcome:

common structural validation is reusable while business validation remains
service-owned.

✅ R8 — common-jackson
Scope:

approved shared ObjectMapper configuration;

Java Time support;

ISO-8601 timestamp serialization;

JSON conversion utilities.

Outcome:

HTTP, persistence payloads and messaging use consistent JSON behavior.

✅ R9 — common-logging
Scope:

logging aspect;

correlation context support;

consistent parameterized logging conventions.

Outcome:

service logs are traceable without logging protected values.

✅ R10 — common-mapper
Scope:

MapStruct baseline;

shared mapper configuration;

mapper tests.

Outcome:

DTO and domain mapping is compile-time generated and consistent.

Phase 2 — Common Infrastructure
✅ R11 — common-security
Scope:

shared security configuration;

authenticated principal abstraction;

JWT claim and token support;

security context utilities;

authorization helpers.

Outcome:

services share security mechanics while retaining ownership of business
authorization.

Security hardening continues to be governed by docs/08_SECURITY.md; completion
of R11 does not mean every future service endpoint is already secured.

✅ R12 — common-lock
Scope:

distributed-lock abstraction;

Redisson implementation;

bounded wait and lease behavior;

lock release safety.

Outcome:

business services can coordinate selected concurrent operations across
instances.

Distributed locks do not replace database constraints, atomic state transitions
or idempotency.

✅ R13 — common-kafka
Scope:

shared producer and consumer configuration;

event envelope and serialization conventions;

topic integration support;

common Kafka abstraction.

Outcome:

services can publish and consume explicit, versioned contracts consistently.

✅ R14 — common-outbox
Scope:

Transactional Outbox entity and repository;

outbox creation service;

scheduled publisher;

Kafka acknowledgement handling;

retry state and status transitions.

Current implemented baseline:

PENDING → PROCESSING → SENT
                     ↘ FAILED
Known hardening gaps are tracked in docs/09_OUTBOX.md. R14 completion records
the accepted implementation round; it does not falsely claim that leasing,
multi-instance claiming, backoff, DLT, cleanup or manual replay controls already
exist.

✅ R15 — common-search
Scope:

Elasticsearch abstraction;

search client integration;

full-text search support;

paging and approved sorting;

indexing conventions.

Outcome:

searchable services can integrate without coupling domain contracts to the
search engine.

✅ R16 — common-openapi
Scope:

shared OpenAPI metadata;

bearer authentication scheme;

service-specific documentation properties;

environment-controlled Swagger integration.

Outcome:

API documentation follows one shared baseline.

✅ R17 — common-test
Scope:

JUnit 5 shared configuration;

unit and integration test annotations;

reusable MySQL, Kafka and Redis Testcontainers support;

JSON testing utilities;

integration-test base classes.

Outcome:

service rounds can test real infrastructure behavior consistently.

✅ R18 — common-tracing
Scope:

Micrometer Tracing integration;

OpenTelemetry bridge;

OTLP exporter support;

trace and span context access.

Outcome:

HTTP and messaging flows can propagate approved trace context.

✅ R19 — common-storage
Scope:

file-storage abstraction;

storage metadata contract;

upload and download integration boundary;

validation and provider-independent behavior.

Outcome:

services can use controlled storage without exposing provider credentials or
trusting client filenames as object paths.

Phase 3 — Infrastructure Services
✅ R20 — Config Service
Scope:

centralized externalized configuration;

service-specific configuration repository;

local and Git-backed configuration modes.

Outcome:

services can retrieve shared and environment-specific configuration.

Production secret handling and authenticated encrypted transport remain
deployment requirements.

✅ R21 — Discovery Service
Scope:

service registration;

service discovery;

health-aware instance lookup.

Outcome:

infrastructure and business services can discover approved instances.

Discovery registration is not caller authentication.

✅ R22 — API Gateway
Scope:

central route definitions;

service discovery routing;

request correlation handling;

gateway error handling;

edge security integration.

Outcome:

external API traffic enters through one controlled routing boundary.

Business services must still validate authentication and authorization; the
Gateway is not the sole security boundary.

Phase 4 — Business Services
🚧 R23 — Movie Service
Movie Service owns:

movies;

genres;

movie lifecycle state;

movie metadata;

its MySQL schema and Flyway migrations.

Implemented feature scope:

Movie Service application module;

movie and genre entities;

repositories;

request and response DTOs;

MapStruct mappers;

movie and genre services;

REST controllers;

validation and shared response integration;

Flyway schema and default genre data.

R23 is in stabilization, not complete.

Remaining R23 work
verify no real credential remains committed;

add unit tests for movie and genre business rules;

add controller tests for validation, authentication and authorization;

add persistence integration tests for constraints, mappings and Flyway;

test duplicate slug, duplicate genre and invalid state behavior;

confirm pagination and sort-field allowlists;

verify error responses contain no internal details;

run the complete repository build;

synchronize all documentation affected by the final implementation.

R23 exit criteria
Movie Service tests cover success and failure behavior.

Applicable authorization matrix cases pass.

Flyway initializes a clean MySQL container successfully.

No Movie Service code accesses another service's database.

No hard-coded production secret exists.

Root mvn clean verify passes.

git diff --check passes.

README, architecture, database, security, deployment, roadmap and changelog
agree on R23 status.

Only after these criteria pass may R23 be changed to ✅ and R24 become the
active round.

⏳ R24 — User Service
User Service will own:

user accounts;

user profiles;

credentials when local authentication is used;

roles and account status;

verification and password-recovery state;

refresh-token or session state according to the accepted authentication
design.

Planned scope:

account registration;

authentication integration;

password hashing;

account verification;

profile management;

role and permission enforcement;

refresh rotation and revocation where applicable;

administrative account actions;

audit coverage for privileged changes.

Required decisions before implementation:

authoritative token issuer;

OAuth2 / authorization-server topology;

access and refresh token model;

issuer, audience and signing-key ownership;

MFA scope for privileged accounts.

R24 must satisfy the token and authorization test cases in
docs/08_SECURITY.md.

⏳ R25 — Inventory Service
Inventory Service will own:

cinemas;

auditoriums;

seats;

showtimes;

show_seats;

seat availability and reservation state.

Planned scope:

cinema and auditorium management;

seat layout management;

showtime creation;

show-seat generation;

atomic seat state transitions;

distributed lock integration where required;

reservation expiration;

inventory events and idempotent commands;

administrative seat correction with audit controls.

Critical invariant:

Inventory Service is the only service allowed to modify show_seats.

Booking Service must never access the Inventory database directly.

R25 must be complete before R26 implements the final cross-service seat
reservation flow.

⏳ R26 — Booking Service
Booking Service will own:

bookings;

booking items or booking-seat references;

booking lifecycle state;

booking expiration;

booking Outbox and consumer-processing records.

Planned scope:

authenticated booking creation;

ownership enforcement;

seat-count and request validation;

client request idempotency;

coordination with Inventory Service;

reservation expiration;

Transactional Outbox publication;

Saga state transitions;

idempotent handling of Inventory and Payment events;

cancellation behavior;

booking query APIs.

Booking Service must not:

accept an arbitrary request userId as ownership;

read or update show_seats;

receive Payment provider credentials;

mark a booking confirmed without validated current state.

⏳ R27 — Payment Service
Payment Service will own:

payment attempts;

provider references;

payment state;

refund state;

webhook processing evidence;

Payment Outbox and consumer-processing records.

Planned scope:

payment initiation;

provider token or hosted-payment integration;

provider idempotency;

authenticated webhook verification;

duplicate callback protection;

payment success and failure events;

refund and reconciliation controls;

idempotent Booking event consumption;

audit coverage for financial administration.

Payment Service must never store CVV or publish card credentials in events.

R27 completes the principal Booking–Payment Saga path when its integration tests
verify success, failure, timeout, duplicate event and delayed event scenarios.

⏳ R28 — Notification Service
Notification Service will own:

notification requests;

delivery attempts;

template references;

approved contact snapshots;

delivery state;

consumer-processing records.

Planned scope:

idempotent consumption of approved business events;

email and other approved delivery channels;

retry and bounded failure handling;

template rendering with untrusted-data protection;

delivery observability;

retention and personal-data minimization.

Notification failure must not silently change authoritative Booking or Payment
state.

Cross-Service Integration Milestones
These milestones do not replace round exit criteria.

Milestone A — Catalog Ready
Requires:

R23 complete.

Result:

clients can query and administer movie and genre data according to
authorization policy.

Milestone B — Identity Ready
Requires:

R24 complete.

Result:

end-user and administrative identities can be authenticated and authorized.

Milestone C — Seat Inventory Ready
Requires:

R25 complete.

Result:

cinemas, showtimes and seats have one authoritative owner and atomic
reservation behavior.

Milestone D — Booking Saga Ready
Requires:

R26 and R27 complete;

Event Catalog contracts verified;

Transactional Outbox and Idempotent Consumer integration tests passing.

Result:

a booking can reserve inventory, complete or fail payment, and converge to the
correct state without cross-database transactions.

Milestone E — Customer Communication Ready
Requires:

R28 complete.

Result:

approved business outcomes trigger idempotent notifications.

Phase 5 — Production Readiness
Production-readiness work begins after the business-service foundations are
implemented. It must be divided into explicitly scoped rounds before code or
infrastructure changes begin.

Approved capability areas:

CI/CD
Kubernetes
Monitoring
Metrics
Prometheus
Grafana
OpenTelemetry deployment
Performance testing
Chaos testing
These entries are planned capability areas, not implemented features and not
authorization to add technologies outside docs/03_TECHNOLOGY_STACK.md.

Before this phase starts, define:

round numbers and owners;

environment topology;

secret-management platform;

TLS and service-identity model;

Kafka, Redis and database security model;

backup and recovery objectives;

SLI, SLO and alerting requirements;

deployment and rollback strategy;

load profiles and acceptance thresholds;

incident-response responsibilities.

Required Verification Commands
At minimum, a round-closing review must run:

git diff --check
mvn clean verify
When the Maven wrapper is the approved repository entry point:

./mvnw clean verify
Windows:

.\mvnw.cmd clean verify
Security and architecture searches from docs/08_SECURITY.md must also be
reviewed before a security-sensitive round is closed.

Command success alone is insufficient when required integration infrastructure
or tests were skipped.

Roadmap Update Rules
When a round changes state:

Verify every exit criterion.

Record evidence from tests and the full build.

Update the status in this file.

Update docs/11_CHANGELOG.md.

Synchronize README.md, docs/00_PROJECT_CONTEXT.md and
docs/01_AI_CONTEXT.md.

Update architecture, database, event, security, Outbox, sequence and
deployment documents when affected.

Add or update an ADR when an architectural decision changes.

Commit the documentation with the implementation or stabilization work it
describes.

Do not:

mark a round complete before verification;

rewrite history to hide unfinished stabilization;

start a later service by bypassing an ownership dependency;

describe planned infrastructure as deployed;

weaken tests or security to make a round appear complete;

add scope to a completed round without documenting the new work;

use roadmap text as a substitute for implementation evidence.

Current Snapshot
Phase	Rounds	Status
Foundation Layer	R1–R10	✅ Completed
Common Infrastructure	R11–R19	✅ Completed rounds; documented hardening gaps remain where stated
Infrastructure Services	R20–R22	✅ Completed rounds
Movie Service	R23	🚧 Stabilization
User Service	R24	⏳ Planned
Inventory Service	R25	⏳ Planned
Booking Service	R26	⏳ Planned
Payment Service	R27	⏳ Planned
Notification Service	R28	⏳ Planned
Production Readiness	To be assigned	⏳ Planned
The active target remains:

R23 Movie Service stabilization
R24 must not be marked active until the R23 exit criteria are satisfied.
