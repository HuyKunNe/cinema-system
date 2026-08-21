# Transactional Outbox

**Version:** R26.6
**Status:** Shared Outbox hardening implemented; business-service integration remains service-owned
**Last updated:** 2026-08-21

---

# Purpose

Cinema Booking System uses the Transactional Outbox Pattern to publish Kafka
integration events reliably without a distributed transaction.

The owning service writes its business state and an Outbox row in the same local
MySQL transaction. A scheduler publishes the persisted event only after that
transaction commits.

The Outbox guarantees:

- a committed business change has a durable event record;
- a rolled-back business change does not publish an event;
- Kafka publication can be retried independently;
- a service does not require a distributed transaction;
- the original event identifier and routing information survive retries.

The Outbox does not provide end-to-end exactly-once delivery.

The delivery model is:

```text
At-least-once publication
+
Idempotent consumption
```

Duplicate publication is valid. Consumers must prevent duplicate business
effects.

---

# Source of Truth

This document defines:

1. The hardened `common-outbox` infrastructure completed in R26.6.
2. The contract every Outbox-owning service must follow.
3. The transaction, claim, lease, retry and acknowledgement rules.
4. The remaining service-integration and operational work.

Related event names, producers, consumers and versions are defined by:

```text
docs/07_EVENT_CATALOG.md
```

When documentation and implementation differ:

1. Preserve database-per-service ownership.
2. Preserve the local business transaction plus Outbox insert boundary.
3. Preserve at-least-once delivery and idempotent consumption.
4. Preserve the original event ID across publication retries.
5. Do not claim exactly-once delivery.
6. Do not mark service integration complete until its transaction and contract
   tests pass.

---

# Ownership

Every service owns its own `outbox_events` table.

| Service              | Owns business data            | Owns Outbox events                          |
| -------------------- | ----------------------------- | ------------------------------------------- |
| Booking Service      | `bookings`, `booking_seats`   | Booking lifecycle events                    |
| Inventory Service    | `show_seats`, inventory state | Seat hold, booking and release events       |
| Payment Service      | `payments`, payment attempts  | Payment result events                       |
| Notification Service | Notification state            | Notification-owned events only when defined |
| User Service         | Users and identity state      | Approved minimal user lifecycle events only |

Rules:

- a service writes only to its own Outbox table;
- a service reads and publishes only its own Outbox rows;
- no service queries another service's Outbox table;
- no cross-service foreign key references an Outbox row;
- business event ownership remains in the producing business service;
- `common-outbox` provides infrastructure only;
- business event classes and business decisions do not belong in
  `common-outbox`.

Inventory Service remains the only owner of `show_seats`. Booking Service
requests reservation through `seat-reservation-requested`; it must not update
Inventory tables directly.

User Service owns OAuth2 clients, authorizations, consent, refresh tokens,
credentials, MFA state and signing-key access. Those records are not Outbox
events. User Service may create Outbox rows only for approved minimal lifecycle
events in the Event Catalog.

---

# End-to-End Flow

```text
Business command
        |
        v
Owning service transaction
        |
        +--> mutate business tables
        |
        +--> insert PENDING outbox_events row
        |
        v
Commit local MySQL transaction
        |
        v
Short claim transaction
        |
        +--> SELECT eligible rows
        +--> FOR UPDATE SKIP LOCKED
        +--> assign processing owner and lease
        +--> mark PROCESSING
        |
        v
Commit claim transaction
        |
        v
Publish to Kafka without a database transaction
        |
        v
Kafka acknowledgement future
        |
        +--> success: conditional PROCESSING -> SENT
        |
        +--> failure: conditional PROCESSING -> FAILED
```

Kafka I/O must never keep a database transaction open.

---

# Local Transaction Boundary

The aggregate mutation and Outbox insert use the same datasource and the same
local transaction.

Conceptual example:

```java
@Transactional
public Booking createBooking(CreateBookingCommand command) {
    Booking booking = bookingRepository.save(
            Booking.create(command));

    outboxService.save(
            createSeatReservationRequestedEvent(booking));

    return booking;
}
```

Required behavior:

| Business write             | Outbox insert               | Transaction result | Event may publish |
| -------------------------- | --------------------------- | ------------------ | ----------------- |
| Success                    | Success                     | Commit             | Yes               |
| Success                    | Failure                     | Rollback           | No                |
| Failure                    | Not executed or rolled back | Rollback           | No                |
| Rollback after both writes | Rolled back                 | Rollback           | No                |

`REQUIRES_NEW` must not be used for the Outbox insert. It could commit an event
for business data that later rolls back.

Direct Kafka publication must not replace the Outbox insert.

---

# Shared Components

R26.6 provides:

```text
OutboxEventEntity
OutboxStatus
AggregateType
OutboxRepository
OutboxService
DefaultOutboxService
OutboxClaimService
DefaultOutboxClaimService
OutboxAcknowledgementService
DefaultOutboxAcknowledgementService
OutboxRetryPolicy
OutboxPublisher
KafkaOutboxPublisher
OutboxScheduler
OutboxEventMessage
OutboxPublishException
OutboxProperties
OutboxConfiguration
```

| Component                      | Responsibility                                   |
| ------------------------------ | ------------------------------------------------ |
| `OutboxEventEntity`            | Persist event, routing and publication state     |
| `OutboxRepository`             | Atomic selection and conditional state updates   |
| `OutboxService`                | Insert an event in a business transaction        |
| `OutboxClaimService`           | Claim a bounded batch in a short transaction     |
| `OutboxAcknowledgementService` | Persist Kafka success or failure transactionally |
| `OutboxRetryPolicy`            | Calculate bounded backoff and jitter             |
| `OutboxPublisher`              | Publish one persisted event                      |
| `KafkaOutboxPublisher`         | Build the envelope and delegate to Kafka         |
| `OutboxScheduler`              | Coordinate claim, publish and acknowledgement    |
| `OutboxEventMessage`           | Canonical infrastructure event envelope          |
| `OutboxProperties`             | Typed scheduler, lease and retry configuration   |

`common-outbox` must not own:

```text
Booking status transitions
Seat allocation rules
Payment decisions
Notification decisions
Business event producer ownership
Business compensation logic
```

---

# Hardened Persistence Model

Each owning service defines its `outbox_events` table through Flyway.
`common-outbox` supplies the matching JPA entity and infrastructure.

| Field                   | Java type        | Meaning                               |
| ----------------------- | ---------------- | ------------------------------------- |
| `id`                    | `UUID`           | Stable event identifier               |
| `aggregate_type`        | `AggregateType`  | Owning aggregate category             |
| `aggregate_id`          | `UUID`           | Owning aggregate identifier           |
| `event_type`            | `String`         | Logical event type                    |
| `event_version`         | `String`         | Explicit event contract version       |
| `topic`                 | `String`         | Kafka destination                     |
| `partition_key`         | `String`         | Kafka ordering key                    |
| `occurred_at`           | `OffsetDateTime` | Business occurrence time              |
| `correlation_id`        | `UUID`           | Distributed flow identifier           |
| `causation_id`          | `UUID`           | Causing command or event              |
| `payload`               | `LONGTEXT`       | Immutable serialized payload          |
| `status`                | `OutboxStatus`   | Publication state                     |
| `retry_count`           | `Integer`        | Failed publication count              |
| `next_attempt_at`       | `OffsetDateTime` | Earliest next eligible attempt        |
| `last_error`            | `String`         | Bounded sanitized failure description |
| `processing_owner`      | `String`         | Unique claim token                    |
| `processing_started_at` | `OffsetDateTime` | Claim start time                      |
| `processing_expires_at` | `OffsetDateTime` | Claim lease expiration                |
| `created_at`            | `OffsetDateTime` | Outbox row creation time              |
| `published_at`          | `OffsetDateTime` | Successful Kafka acknowledgement time |

UUID columns use `BINARY(16)` consistently.

JPA schema generation does not replace Flyway. Production-like tests use
`ddl-auto=validate`.

Required indexes include:

```text
(status, next_attempt_at, processing_expires_at, created_at)
(processing_owner, status)
(aggregate_type, aggregate_id)
(correlation_id)
```

---

# Identifier Rules

All integration-event identifiers use UUID v7.

- Generate `eventId` once when the Outbox row is created.
- Preserve the same `eventId` across every retry.
- Never generate a new event ID for a publication retry.
- The Kafka message `eventId` equals `outbox_events.id`.
- `aggregateId` identifies the owning business aggregate.
- Retry, recovery, DLT replay and manual replay preserve the event ID.
- A new business event is not a retry and receives a new event ID.

Claim tokens are infrastructure identifiers. They are not integration-event IDs
and do not appear in the published envelope.

---

# Canonical Event Contract

Persisted and published envelope values are:

```text
eventId
aggregateId
aggregateType
eventType
eventVersion
occurredAt
correlationId
causationId
payload
```

Routing values persisted beside the envelope are:

```text
topic
partitionKey
```

Rules:

- `occurredAt` is the business occurrence time.
- `createdAt` is the Outbox row creation time and is not a replacement for
  `occurredAt`.
- `publishedAt` is the successful acknowledgement time.
- `eventVersion` is explicit.
- `correlationId` links events in the same distributed flow.
- `causationId` identifies the command or event that caused this event.
- payload contracts are immutable DTOs or records.
- timestamps use ISO-8601 through the shared Jackson configuration.
- money uses `BigDecimal` plus an explicit currency.
- JPA entities are never serialized as integration events.
- consumers do not trust producer-internal `__TypeId__` headers.

---

# Topic and Partition Routing

`topic` and logical `eventType` are separate persisted values.

The publisher delegates with:

```java
producer.send(
        event.getTopic(),
        event.getPartitionKey(),
        message);
```

Rules:

- topic names use kebab-case;
- topics exist in `docs/07_EVENT_CATALOG.md`;
- a service publishes only topics it owns;
- the partition key follows the event catalog and service design;
- Booking Saga events use the Booking aggregate ID as the Kafka key;
- retries preserve the original topic and key;
- consumers route by supported event type and version.

---

# Business Event Ownership

| Event/topic                   | Producer          | Primary consumers                       |
| ----------------------------- | ----------------- | --------------------------------------- |
| `seat-reservation-requested`  | Booking Service   | Inventory Service                       |
| `seat-reserved`               | Inventory Service | Booking Service                         |
| `seat-reservation-rejected`   | Inventory Service | Booking Service                         |
| `payment-requested`           | Booking Service   | Payment Service                         |
| `payment-succeeded`           | Payment Service   | Booking Service                         |
| `payment-failed`              | Payment Service   | Booking Service                         |
| `seat-release-requested`      | Booking Service   | Inventory Service                       |
| `seat-released`               | Inventory Service | Booking Service                         |
| `booking-confirmed`           | Booking Service   | Inventory Service, Notification Service |
| `booking-cancelled`           | Booking Service   | Inventory Service, Notification Service |
| `booking-expired`             | Booking Service   | Inventory Service, Notification Service |
| `user-registered`             | User Service      | Approved consumers only                 |
| `user-email-verified`         | User Service      | Approved consumers only                 |
| `user-account-status-changed` | User Service      | Approved consumers only                 |

The Event Catalog remains authoritative if ownership or the event set changes.
A consumer does not gain permission to publish a topic merely because it can
consume it.

---

# Status State Machine

Approved statuses are:

```text
PENDING
PROCESSING
SENT
FAILED
```

```text
PENDING ------> PROCESSING ------> SENT
                    |
                    +-----------> FAILED
                                      |
                                      +--> PROCESSING on retry

expired PROCESSING lease ------------> PROCESSING under a new claim
```

| Status       | Meaning                                            |
| ------------ | -------------------------------------------------- |
| `PENDING`    | Persisted and waiting for first publication        |
| `PROCESSING` | Owned by an active or recoverable processing lease |
| `SENT`       | Kafka acknowledgement completed successfully       |
| `FAILED`     | Last owned publication attempt failed              |

Rules:

- new rows start as `PENDING`;
- claim changes an eligible row to `PROCESSING`;
- `SENT` is terminal for normal scheduling;
- failure increments `retry_count`;
- retry changes an eligible `FAILED` row to `PROCESSING`;
- expired `PROCESSING` may be reclaimed safely;
- business code never creates a row directly as `SENT`;
- publication failure does not roll back committed business state;
- the obsolete status `NEW` must not be used.

---

# Atomic Claiming

Claiming occurs in a short local transaction.

The MySQL 8 query selects a bounded ordered batch using:

```sql
FOR UPDATE SKIP LOCKED
```

Eligible rows are:

- `PENDING` rows whose `next_attempt_at` is absent or due;
- `FAILED` rows below the maximum retry count whose next attempt is due;
- `PROCESSING` rows below the maximum retry count whose lease expired.

Ineligible rows are:

- `SENT` rows;
- active `PROCESSING` leases;
- delayed retries;
- retry-exhausted rows.

The claim transaction assigns a unique `processing_owner`, records the start
time, records the lease expiration and commits before Kafka publication.

The database row is authoritative. Correctness does not depend on an in-memory
lock, Redis lock or a single scheduler instance.

---

# Multi-Instance Safety

Multiple service instances may run the scheduler concurrently.

`SKIP LOCKED` ensures that a competing claimer does not wait for or select a row
currently locked by another claim transaction.

Each claimed event receives a unique claim token. A token is unique per claim,
not merely per application instance. This prevents a delayed callback from an
earlier attempt from acknowledging a newer claim.

Concurrency tests use MySQL Testcontainers and verify:

- two claimers cannot own the same active row;
- locked rows are skipped;
- active leases are not stolen;
- expired leases are recovered;
- stale acknowledgements update zero rows.

---

# Processing Lease Recovery

A row is recoverable when:

```text
status = PROCESSING
AND processing_expires_at <= current time
AND retry_count < maximum attempts
```

Recovery preserves all event identity, contract, payload and routing values.
It changes only processing ownership and lease metadata.

Do not reset all `PROCESSING` rows blindly. A live publisher may still own an
unexpired lease.

Because a process may stop after Kafka accepts an event but before `SENT`
commits, recovery may publish the same event again. Consumers remain
idempotent.

---

# Kafka Acknowledgement

Calling the producer is not proof of successful publication.

The row becomes `SENT` only after the future returned by
`KafkaProducerService.send(...)` completes successfully.

Acknowledgement runs in a separate short transaction after Kafka I/O.

The conditional success boundary is:

```text
event_id = expected event
AND status = PROCESSING
AND processing_owner = expected claim token
```

Success sets:

```text
status = SENT
published_at = acknowledgement time
next_attempt_at = NULL
processing lease fields = NULL
```

Failure uses the same ownership boundary and sets:

```text
status = FAILED
retry_count = retry_count + 1
next_attempt_at = calculated retry time
last_error = bounded sanitized reason
processing lease fields = NULL
```

An update count of zero means the callback is stale and is ignored.

---

# Retry Policy

Publication failures use bounded exponential backoff with bounded jitter.

```text
bounded delay =
min(
    configured maximum retry delay,
    base retry delay * 2 ^ current retry count
)

next attempt =
failure time
+ bounded delay
+ bounded random jitter
```

Rules:

- retry uses the same row and event ID;
- retry preserves topic and partition key;
- `retry_count` increments only through an owned failure acknowledgement;
- a row is not claimable before `next_attempt_at`;
- a row is not claimable after reaching `maximumAttempts`;
- `last_error` is limited to 2,000 characters;
- `last_error` contains no payload, credential or complete stack trace.

Retryable examples include broker unavailability, leader election, request
timeout and transient network failure.

Malformed JSON, unsupported event types, unsupported versions and invalid
routing require explicit terminal classification and operational handling. R26.6
does not introduce a separate terminal status or DLT.

---

# Failure Windows

| Failure window                                   | Expected result                                      |
| ------------------------------------------------ | ---------------------------------------------------- |
| Business transaction rolls back                  | Business state and Outbox row both roll back         |
| Process stops after business commit              | PENDING row remains durable                          |
| Claim transaction rolls back                     | Row remains eligible under its prior state           |
| Process stops after claim                        | Expired lease permits recovery                       |
| Kafka send fails                                 | Owned acknowledgement schedules retry                |
| Kafka accepts event, process stops before `SENT` | Recovery may publish the same event again            |
| Stale callback arrives                           | Conditional update affects zero rows                 |
| Consumer receives duplicate                      | Processed-event uniqueness prevents duplicate effect |
| Retry count reaches maximum                      | Automated claim stops and operations must alert      |

---

# Idempotent Consumption

At-least-once publication requires idempotent state-changing consumers.

The processing uniqueness boundary is:

```text
(event_id, consumer_name)
```

The following commit together:

```text
processed-event insertion
business state transition
resulting Outbox insertion
```

A duplicate-key race is duplicate delivery, not an uncontrolled internal error.
Consumers also validate current aggregate state and supported event version.

---

# Serialization

Payload JSON uses the approved shared `ObjectMapper`.

- use ISO-8601 timestamps;
- disable timestamp-array serialization;
- do not enable unsafe global polymorphic typing;
- do not deserialize arbitrary producer class names;
- use immutable DTOs or records;
- do not serialize JPA proxies or entities;
- do not serialize exceptions into the event;
- validate payload size;
- keep payloads compatible within an event version;
- introduce a new version for breaking changes.

Serialization occurs before the business transaction commits. A payload that
cannot be serialized must prevent committing business state without a
publishable event.

---

# Security and Privacy

- Only the owning service database user accesses its Outbox table.
- Publication workers use least-privilege Kafka credentials.
- Producer ACLs permit only topics owned by that service.
- Payloads contain only data required by approved consumers.
- Logs do not dump complete payloads.
- Administrative inspection and replay require privileged access.
- Backups containing Outbox rows follow data-protection requirements.
- Kafka and database traffic use protected transport outside isolated local
  development.

Prohibited payload and error data includes:

```text
Passwords and password hashes
Password-reset and email-verification tokens
Access and refresh tokens
Authorization codes
Database credentials
Private signing keys
OAuth2 client secrets
MFA secrets and recovery codes
Payment provider secrets
CVV values and full card numbers
Unnecessary personal data
Complete internal stack traces
```

## User Service and OAuth2 boundary

The Outbox is not OAuth2 token, session, revocation, consent or security-audit
storage.

Login activity, token issuance, authorization denial, refresh-token rotation,
client-secret changes, MFA operations and signing-key operations are not Kafka
business events by default.

An approved User lifecycle payload:

- uses a UUID user identifier;
- contains only fields required by an approved consumer;
- avoids a full profile when an identifier is sufficient;
- excludes every credential, token, secret and raw authentication artifact;
- follows retention and privacy requirements;
- has payload-contract and sensitive-data tests.

---

# Configuration

Active typed properties use the `cinema.outbox` prefix:

```yaml
cinema:
  outbox:
    batch-size: 100
    scheduler-delay: 5s
    lease-duration: 30s
    maximum-attempts: 5
    base-retry-delay: 1s
    maximum-retry-delay: 1m
    maximum-jitter: 500ms
```

Rules:

- values have safe local defaults;
- batch size and attempt count are positive;
- durations are non-negative and meaningful;
- maximum retry delay is not below the base delay;
- configuration contains no credentials;
- production must not silently disable publication.

---

# Dead-Letter Strategy

DLT publication is not implemented in R26.6.

Do not document a DLT topic as active until ownership, ACLs, retention, payload,
monitoring and replay behavior are implemented and tested.

A future DLT record preserves the original event identity and routing metadata.
It must not contain credentials, full payment data, complete stack traces or
unnecessary personal data.

DLT publication is not Saga compensation. Compensation remains business logic
owned by participating services.

---

# Manual Replay

Manual replay is not implemented in R26.6. When introduced, it requires:

- explicit authorization;
- an audit record with actor, reason, target and result;
- selection by stable event ID;
- preservation of event ID, aggregate key and version;
- supported payload and topic validation;
- protection against concurrent automated publication;
- bounded batches and dry-run inspection;
- metrics and alerting.

Manual replay must not silently edit payloads, generate a new ID to bypass
deduplication, change business state directly or reset every failed row blindly.

---

# Retention and Cleanup

Cleanup is not implemented in R26.6.

Before enabling cleanup, define retention for:

```text
SENT Outbox rows
retry-exhausted rows
processed-event rows
DLT records
audit records
Kafka topics
database backups
```

Cleanup rules:

- delete only terminal rows eligible by policy;
- never delete `PENDING` rows;
- never delete an active `PROCESSING` lease;
- do not discard unresolved failures merely because they are old;
- use bounded batches and short transactions;
- preserve audit requirements;
- coordinate Outbox, processed-event and Kafka retention.

Deleting processed-event records too early permits an old message to create a
duplicate business effect.

---

# Observability

Recommended metrics:

```text
outbox.pending.count
outbox.processing.count
outbox.failed.count
outbox.retry.exhausted.count
outbox.oldest.pending.age
outbox.publish.success.count
outbox.publish.failure.count
outbox.publish.duration
outbox.retry.count
outbox.stuck.processing.count
outbox.cleanup.deleted.count
```

Useful bounded dimensions include service, environment, event type, result and
failure category.

Do not use event IDs, aggregate IDs, user IDs or raw error messages as unbounded
metric labels.

Alerts should cover old pending rows, growing failure counts, expired leases,
publication failure rate, retry exhaustion, missing successful publications and
table growth.

Operational metrics and alerts remain follow-up work unless a service round
explicitly implements them.

---

# Testing Requirements

## Unit tests

Verify:

- new rows start as `PENDING` and are initially eligible;
- claim assigns status, owner and lease;
- matching owners may complete entity transitions;
- stale owners cannot complete transitions;
- error descriptions are bounded;
- retry delay grows exponentially and is capped;
- jitter remains within its configured bound;
- canonical envelopes preserve all required values;
- publisher uses persisted topic and partition key;
- malformed payload publication completes exceptionally;
- scheduler acknowledges only after the Kafka future completes.

## Migration and repository tests

Use MySQL Testcontainers to verify:

- UUID mappings match `BINARY(16)` migrations;
- payload mapping is `LONGTEXT`;
- status values match schema constraints;
- required columns and indexes exist;
- `FOR UPDATE SKIP LOCKED` works on MySQL 8;
- eligible states and retry times are selected correctly;
- batch size and retry exhaustion are enforced.

## Transaction tests

Verify:

- business mutation and Outbox insert commit together;
- failure of either write rolls back both;
- original business commit does not require Kafka availability;
- claim commits before Kafka I/O;
- acknowledgement executes in its own transaction;
- stale acknowledgements update zero rows.

## Concurrency tests

Verify:

- concurrent claimers do not own the same event;
- locked rows are skipped;
- active leases are not stolen;
- expired claims recover;
- Kafka I/O holds no database lock;
- duplicate delivery creates one consumer business effect.

## Security tests

Verify payloads and logs contain no prohibited data and Kafka credentials cannot
publish unauthorized topics.

Final verification is:

```bash
mvn clean verify
```

---

# R26.6 Completion State

Implemented in shared infrastructure:

- [x] Canonical persisted event metadata
- [x] Canonical published envelope
- [x] Explicit topic and partition key
- [x] Stable event ID across retry
- [x] Typed claim and retry configuration
- [x] Atomic MySQL claim using `FOR UPDATE SKIP LOCKED`
- [x] Unique processing-owner token
- [x] Processing lease and expired-claim recovery
- [x] Kafka acknowledgement-based `SENT` transition
- [x] Transactional conditional acknowledgements
- [x] Stale callback protection
- [x] Exponential backoff with bounded jitter
- [x] Bounded retry and error description
- [x] Unit tests for envelope, publisher, claim, retry and acknowledgement
- [x] MySQL concurrency and recovery integration tests

Still service-owned or deferred:

- [ ] Business mutation and Outbox insert integration per producing service
- [ ] Business event payload contracts per Event Catalog entry
- [ ] Consumer processed-event integration
- [ ] Kafka broker integration tests per service
- [ ] Non-retryable failure classification
- [ ] DLT design and implementation
- [ ] Authorized manual replay
- [ ] Retention and cleanup jobs
- [ ] Production metrics and alerts
- [ ] Kafka ACL verification

R26.6 completion does not by itself complete Booking publication. Booking creates
`seat-reservation-requested` in R26.7.

---

# Required Service Review Checklist

Before an Outbox-owning service is complete:

- [ ] Business state and Outbox row commit in one transaction
- [ ] Outbox event ID uses UUID v7
- [ ] Event ID remains stable across retries
- [ ] Topic exists in the Event Catalog
- [ ] Producer owns the event
- [ ] Kafka key is the approved aggregate identifier
- [ ] Envelope matches the canonical contract
- [ ] Payload uses the approved Jackson configuration
- [ ] Payload contains no secrets or prohibited payment data
- [ ] New rows start as `PENDING`
- [ ] Rows become `SENT` only after Kafka acknowledgement
- [ ] Failed publication increments retry state
- [ ] Retry exhaustion is observable
- [ ] Abandoned `PROCESSING` rows recover
- [ ] Multi-instance claiming is safe
- [ ] Consumers are transactional and idempotent
- [ ] Processed-event uniqueness is database-enforced
- [ ] Retry and replay preserve event ID and key
- [ ] Flyway and JPA mappings agree
- [ ] Automated tests cover failure windows and duplicate delivery
- [ ] `mvn clean verify` passes

---

# Prohibited Implementations

## Direct dual write

```java
bookingRepository.save(booking);
kafkaTemplate.send("seat-reservation-requested", event);
```

## Separate Outbox transaction

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveOutboxEvent(...) {
    outboxRepository.save(...);
}
```

when the business state uses another transaction.

## New event ID on retry

```java
event.setId(UUID.randomUUID());
publisher.publish(event);
```

## Marking sent before acknowledgement

```java
producer.send(topic, key, message);
repository.markSent(eventId);
```

without awaiting successful completion of the producer future.

## Shared Outbox database

```text
Booking Service
Payment Service
Inventory Service
        |
        v
one shared Outbox database
```

## Blind replay

```sql
UPDATE outbox_events
SET status = 'PENDING'
WHERE status = 'FAILED';
```

without authorization, bounded selection, validation, audit and concurrency
control.

---

# Useful Repository Checks

Find direct business-service Kafka publication:

```bash
git grep -n -E \
    "KafkaTemplate|kafkaTemplate\\.send|producer\\.send" \
    -- services
```

Find status inconsistencies:

```bash
git grep -n -E \
    "NEW|PENDING|PROCESSING|SENT|FAILED" \
    -- common services docs
```

Find regenerated event IDs:

```bash
git grep -n -E \
    "UUID\\.(randomUUID|fromString)|uuidV7" \
    -- common/common-outbox services
```

Find payload logging:

```bash
git grep -n -i -E \
    "log\\.(trace|debug|info|warn|error).*payload" \
    -- common services
```

Find prohibited sensitive fields:

```bash
git grep -n -i -E \
    "password|accessToken|refreshToken|apiKey|clientSecret|cvv|cardNumber" \
    -- common services
```

Verify formatting and build:

```bash
git diff --check
mvn clean verify
```

---

# Related Documentation

```text
docs/00_PROJECT_CONTEXT.md
docs/01_AI_CONTEXT.md
docs/02_ARCHITECTURE.md
docs/05_CODING_CONVENTIONS.md
docs/06_DATABASE_DESIGN.md
docs/07_EVENT_CATALOG.md
docs/08_SECURITY.md
docs/10_ROADMAP.md
docs/11_CHANGELOG.md
docs/12_DEPENDENCY_RULES.md
docs/13_SEQUENCE_DIAGRAMS.md
docs/14_DEPLOYMENT.md
docs/16_BOOKING_SERVICE_DESIGN.md
docs/decisions/
```

The Event Catalog owns event names, producers, consumers and versions.

Database Design owns service data boundaries.

Security Architecture owns authentication, authorization, secret and data
protection requirements.

This document owns the reliable publication contract between a local service
transaction and Kafka.
