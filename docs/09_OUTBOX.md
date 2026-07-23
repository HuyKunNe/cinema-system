# Transactional Outbox

Version: R23

---

# Purpose

Cinema Booking System uses the Transactional Outbox Pattern to publish Kafka
integration events reliably without using a distributed transaction.

The pattern solves the dual-write problem:

```text
Write business data
+
Publish Kafka event
```

These two operations cannot be committed atomically across MySQL and Kafka.
Therefore, the owning service writes both its business state and an outbox row
inside one local database transaction. A scheduler publishes the persisted event
after the transaction commits.

The outbox guarantees:

- A committed business change has a durable event record.
- A rolled-back business change does not publish an event.
- Kafka publication can be retried independently.
- A service does not require a distributed transaction.

The outbox does not guarantee end-to-end exactly-once processing.

The delivery model is:

```text
At-least-once publication
+
Idempotent consumption
```

Duplicate delivery is expected and must not create duplicate business effects.

---

# Source of Truth

This document describes:

1. The `common-outbox` implementation completed in R14.
2. The rules every business service must follow when integrating it.
3. The hardening still required before production use.

When documentation and implementation differ:

1. Preserve database-per-service ownership.
2. Preserve the local business transaction plus outbox insert boundary.
3. Preserve at-least-once delivery and idempotent consumption.
4. Do not claim exactly-once delivery.
5. Treat the implementation gaps listed in this document as unfinished work.

Related event names, producers and consumers are defined by:

```text
docs/07_EVENT_CATALOG.md
```

---

# Ownership

Every service owns its own `outbox_events` table.

Examples:

| Service              | Owns business data            | Owns outbox events                          |
| -------------------- | ----------------------------- | ------------------------------------------- |
| Booking Service      | `bookings`, `booking_seats`   | Booking lifecycle events                    |
| Inventory Service    | `show_seats`, inventory state | Seat reservation and release events         |
| Payment Service      | `payments`, payment attempts  | Payment result events                       |
| Notification Service | Notification state            | Notification-owned events only when defined |

An outbox table is not shared infrastructure data.

Rules:

- A service writes only to its own outbox table.
- A service reads and publishes only its own outbox rows.
- No service queries another service's outbox table.
- No cross-service foreign key may reference an outbox row.
- Business event ownership remains in the producing business service.
- `common-outbox` provides infrastructure only.
- Business event classes and business decisions must not be placed in
  `common-outbox`.

Inventory Service owns `show_seats`. Booking Service must request seat
reservation through `seat-reservation-requested`; it must not update inventory
tables directly.

---

# End-to-End Flow

```text
Business command
        |
        v
Owning service transaction
        |
        +--> update business tables
        |
        +--> insert outbox_events row as PENDING
        |
        v
Commit local MySQL transaction
        |
        v
Outbox Scheduler
        |
        +--> select publishable rows
        |
        +--> mark PROCESSING
        |
        +--> publish to Kafka
        |
        v
Kafka acknowledgement future
        |
        +--> success: mark SENT
        |
        +--> failure: mark FAILED and increment retry_count
```

The event must never be published directly from the business transaction as a
replacement for the outbox insert.

Correct:

```text
Business transaction
    update aggregate
    save outbox row
commit

Scheduler
    publish later
```

Incorrect:

```text
Business transaction
    update aggregate
    kafkaTemplate.send(...)
commit
```

The incorrect flow can produce either:

- Committed business data with no Kafka event.
- A Kafka event for business data that later rolls back.

---

# Local Transaction Boundary

The aggregate update and outbox insert must use the same datasource and the same
local transaction.

Conceptual example:

```java
@Transactional
public UUID createBooking(CreateBookingCommand command) {
    Booking booking = bookingRepository.save(
        Booking.create(command)
    );

    outboxService.save(
        new OutboxEventEntity(
            uuidV7Generator.generate(),
            AggregateType.BOOKING,
            booking.getId(),
            "seat-reservation-requested",
            serializeSeatReservationRequested(booking)
        )
    );

    return booking.getId();
}
```

Required behavior:

| Business write             | Outbox insert               | Transaction result | Event may publish |
| -------------------------- | --------------------------- | ------------------ | ----------------- |
| Success                    | Success                     | Commit             | Yes               |
| Success                    | Failure                     | Rollback           | No                |
| Failure                    | Not executed or rolled back | Rollback           | No                |
| Rollback after both writes | Rolled back                 | Rollback           | No                |

`REQUIRES_NEW` must not be used for the outbox insert because it could commit an
event for business data that later rolls back.

Publishing occurs outside the original business transaction.

---

# Current `common-outbox` Components

R14 introduced:

```text
OutboxEventEntity
OutboxStatus
AggregateType
OutboxRepository
OutboxService
DefaultOutboxService
OutboxPublisher
KafkaOutboxPublisher
OutboxScheduler
OutboxEventMessage
OutboxEventPayload
OutboxPublishException
OutboxConfiguration
```

Responsibilities:

| Component              | Responsibility                                     |
| ---------------------- | -------------------------------------------------- |
| `OutboxEventEntity`    | Persist publication state and serialized payload   |
| `OutboxRepository`     | Load and save outbox rows                          |
| `OutboxService`        | Save an outbox row from a business transaction     |
| `OutboxPublisher`      | Publish one persisted outbox event                 |
| `KafkaOutboxPublisher` | Deserialize payload and delegate to `common-kafka` |
| `OutboxScheduler`      | Poll, publish and update status                    |
| `OutboxEventMessage`   | Kafka message currently created by the publisher   |
| `OutboxEventPayload`   | Current generic payload wrapper                    |

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

# Current Persistence Model

The current R14 entity maps to:

| Field            | Current Java type | Meaning                                                     |
| ---------------- | ----------------- | ----------------------------------------------------------- |
| `id`             | `UUID`            | Event identifier                                            |
| `aggregate_type` | `AggregateType`   | Aggregate category                                          |
| `aggregate_id`   | `UUID`            | Aggregate identifier                                        |
| `event_type`     | `String`          | Topic and event routing value in the current implementation |
| `payload`        | `LONGTEXT`        | Serialized JSON payload                                     |
| `status`         | `OutboxStatus`    | Publication state                                           |
| `retry_count`    | `Integer`         | Failed publication count                                    |
| `created_at`     | `OffsetDateTime`  | Creation time                                               |
| `published_at`   | `OffsetDateTime`  | Successful publication time                                 |

The actual Flyway migration in each service must define the schema. JPA must
validate it; JPA schema generation must not replace Flyway.

Conceptual baseline:

```sql
CREATE TABLE outbox_events (
    id             BINARY(16)    NOT NULL,
    aggregate_type VARCHAR(50)   NOT NULL,
    aggregate_id   BINARY(16)    NOT NULL,
    event_type     VARCHAR(100)  NOT NULL,
    payload        LONGTEXT      NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    retry_count    INT           NOT NULL DEFAULT 0,
    created_at     DATETIME(6)   NOT NULL,
    published_at   DATETIME(6)   NULL,
    PRIMARY KEY (id),
    INDEX idx_outbox_publish (
        status,
        created_at
    ),
    INDEX idx_outbox_aggregate (
        aggregate_type,
        aggregate_id
    )
);
```

The exact UUID column representation must match the shared UUID/JPA strategy.
Do not mix `VARCHAR(36)` and `BINARY(16)` accidentally.

The conceptual schema above reflects the current entity. Fields such as
`next_retry_at`, `last_error`, claim ownership and lease expiration require a
future migration and corresponding entity changes before they may be used.

---

# Identifier Rules

All outbox event identifiers use UUID v7.

Rules:

- Generate `eventId` once when the outbox row is created.
- Preserve the same `eventId` across every retry.
- Never generate a new ID for a publication retry.
- The Kafka message `eventId` must equal `outbox_events.id`.
- `aggregateId` must identify the owning business aggregate.
- Kafka retry, DLT replay and manual replay must preserve the original
  `eventId`.
- A newly created business event is not a retry and receives a new `eventId`.

Generating a new `eventId` during retry breaks consumer deduplication.

---

# Event Contract

The canonical integration-event contract is defined in
`docs/07_EVENT_CATALOG.md`.

Required envelope information includes:

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

The current R14 `OutboxEventMessage` contains only:

```text
eventId
eventType
createdAt
payload
```

This is an implementation gap. Before business-service integration is considered
complete, the persisted data and published envelope must be aligned with the
canonical event contract without introducing producer-internal class coupling.

Rules:

- `occurredAt` represents when the business event occurred.
- `createdAt` represents when the outbox row was created.
- Publication time must not replace business occurrence time.
- `eventVersion` must be explicit.
- `correlationId` links events belonging to the same business flow.
- `causationId` identifies the command or event that caused the new event.
- Event payloads must be immutable contracts.
- Timestamps use ISO-8601 and the approved shared Jackson configuration.
- Money uses `BigDecimal` plus an explicit currency.
- JPA entities must never be serialized as integration events.
- Kafka consumers must not trust producer-internal `__TypeId__` headers.

---

# Event Type and Topic Routing

The current `KafkaOutboxPublisher` calls:

```java
producer.send(
    event.getEventType(),
    message
);
```

Therefore, the current implementation uses `event_type` as the Kafka topic.

Until topic and logical event type are modeled separately:

- `event_type` must contain the exact approved topic name.
- Topic names must be kebab-case.
- Topic names must exist in `docs/07_EVENT_CATALOG.md`.
- A service may publish only the topics it owns.
- Consumers must route by an explicit supported event type and version.

If a future design separates `topic` from `eventType`, it requires a migration,
contract update and tests. Do not silently change the meaning of the existing
column.

---

# Business Event Ownership

The current project event flow is:

| Event/topic                  | Producer          | Primary consumers                       |
| ---------------------------- | ----------------- | --------------------------------------- |
| `seat-reservation-requested` | Booking Service   | Inventory Service                       |
| `seat-reserved`              | Inventory Service | Booking Service, Payment Service        |
| `seat-reservation-rejected`  | Inventory Service | Booking Service                         |
| `payment-success`            | Payment Service   | Booking Service, Notification Service   |
| `payment-failed`             | Payment Service   | Booking Service, Inventory Service      |
| `booking-confirmed`          | Booking Service   | Notification Service                    |
| `booking-cancelled`          | Booking Service   | Inventory Service, Notification Service |
| `inventory-restored`         | Inventory Service | Booking Service                         |

This table summarizes the current `docs/07_EVENT_CATALOG.md`; that catalog remains
authoritative if the event set changes.

A consumer does not gain permission to publish a topic merely because it can
consume it.

---

# Status State Machine

Current statuses:

```text
PENDING
PROCESSING
SENT
FAILED
```

Current transitions:

```text
PENDING -----> PROCESSING -----> SENT
                   |
                   +-----------> FAILED
                                      |
                                      +--> PROCESSING on retry
```

Meaning:

| Status       | Meaning                                                |
| ------------ | ------------------------------------------------------ |
| `PENDING`    | Persisted and waiting for first publication            |
| `PROCESSING` | Selected by a scheduler and publication is in progress |
| `SENT`       | Kafka send future completed successfully               |
| `FAILED`     | Last publication attempt failed                        |

Rules:

- New outbox rows start as `PENDING`.
- `PROCESSING` is set before calling the publisher.
- `SENT` is terminal for normal scheduler processing.
- `FAILED` increments `retry_count`.
- Retrying changes `FAILED` to `PROCESSING`.
- Business code must not create rows directly as `SENT`.
- Publication failure must not change committed business state.

The older term `NEW` is not the current enum value. New documentation and
migrations must use `PENDING` unless an explicit migration renames the state.

---

# Current Scheduler Behavior

`OutboxScheduler` currently:

1. Runs with fixed delay `${cinema.outbox.delay:5000}`.
2. Selects up to 100 rows whose status is `PENDING` or `FAILED`.
3. Orders rows by `createdAt`.
4. Filters rows through `canRetry()`.
5. Marks each selected row `PROCESSING`.
6. Saves the row.
7. Publishes asynchronously.
8. Marks it `SENT` after successful completion.
9. Marks it `FAILED` and increments `retry_count` after failure.

Current defaults:

| Setting                 |    Value |
| ----------------------- | -------: |
| Scheduler delay         | 5,000 ms |
| Query batch size        |      100 |
| Maximum failed attempts |        5 |

`OutboxConstants` defines `BATCH_SIZE = 100` and `DEFAULT_DELAY = 5000`, but the
current repository method and scheduler annotation also contain their effective
values directly. These values should be unified through validated configuration
in a later hardening change.

---

# Kafka Acknowledgement Rule

Calling the Kafka producer is not proof of successful publication.

The outbox row may become `SENT` only after the future returned by
`KafkaProducerService.send(...)` completes successfully.

Current behavior:

```text
producer.send(...)
    |
    +--> future succeeds --> markSent()
    |
    +--> future fails ----> markFailed()
```

The exact Kafka producer acknowledgement level is infrastructure configuration,
but the application must never mark `SENT` merely because the send method
returned a future.

Important failure window:

```text
Kafka accepted message
        |
process crashes before database marks SENT
        |
scheduler publishes the same event again
```

This is why duplicate Kafka delivery is valid and consumers must be idempotent.

---

# Retry Policy

The current R14 retry implementation is immediate polling of `FAILED` rows until
`retry_count` reaches 5.

Current rule:

```java
retryCount < 5
```

The same outbox row and `eventId` are reused.

Current limitations:

- No `next_retry_at`.
- No exponential backoff.
- No jitter.
- No error classification.
- No persisted bounded error reason.
- No automatic terminal status distinct from `FAILED`.
- No DLT publication.

Production hardening should add bounded exponential backoff with jitter:

```text
next delay =
min(
    configured maximum,
    base delay * 2 ^ retry_count
)
+
bounded jitter
```

Retryable examples:

```text
Kafka broker temporarily unavailable
Leader election
Request timeout
Transient network failure
```

Non-retryable examples:

```text
Malformed persisted JSON
Unsupported event type
Unsupported event version
Missing required identifier
Invalid topic mapping
```

Non-retryable failures must not be retried forever. Their operational handling
requires an explicit terminal policy, protected diagnostics and an authorized
replay process.

---

# `PROCESSING` Recovery

The current scheduler selects only `PENDING` and `FAILED`.

If a process stops after saving `PROCESSING` but before saving `SENT` or `FAILED`,
the row remains stuck because the current query will not select it again.

This is a known production-blocking gap.

A hardened implementation requires a claim lease, for example:

```text
processing_owner
processing_started_at
processing_expires_at
```

Recovery rule:

```text
PROCESSING
+
lease expired
        |
        v
eligible for safe retry
```

Recovery preserves the same `eventId`.

Do not reset every `PROCESSING` row blindly. A live publisher may still own an
unexpired claim.

---

# Multi-Instance Concurrency

The current repository query:

```text
findTop100ByStatusInOrderByCreatedAt(...)
```

does not atomically claim rows and does not prevent multiple application
instances from selecting the same row.

Duplicate publication is semantically tolerated by idempotent consumers, but
uncontrolled concurrent claims create avoidable load and unsafe state races.

Before horizontally scaling an outbox publisher, implement and test one approved
database claim strategy, such as:

```text
SELECT ... FOR UPDATE SKIP LOCKED
```

inside a short claim transaction, followed by leased processing.

Requirements:

- Claim rows atomically.
- Keep database locks short.
- Do not hold a database transaction open while waiting for Kafka.
- Record ownership and lease expiration.
- Recover abandoned claims.
- Preserve at-least-once delivery.
- Test with multiple scheduler instances.

Distributed Redis locks must not replace the database claim protocol for outbox
rows.

---

# Ordering

Kafka guarantees order only within a partition.

The partition key must be the aggregate identifier, normally `aggregateId`, so
events for one aggregate remain in order.

Rules:

- Events for one booking use the same booking ID as the Kafka key.
- Events for one payment use the same payment aggregate key according to the
  event contract.
- Retries preserve the original key.
- A consumer must still validate current aggregate state.
- An old or delayed event must not reverse a newer terminal state.
- Global ordering across all aggregates is neither required nor guaranteed.

The current `KafkaOutboxPublisher` delegates without visibly passing an explicit
key. `common-kafka` and producer integration must be verified before claiming
aggregate ordering is implemented.

---

# Idempotent Consumer

Every state-changing Kafka consumer must implement the Idempotent Consumer
Pattern.

Conceptual table:

```sql
CREATE TABLE processed_events (
    event_id       BINARY(16)   NOT NULL,
    consumer_name  VARCHAR(100) NOT NULL,
    processed_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id, consumer_name)
);
```

The identifier uses the same UUID storage strategy as event IDs.

Consumer transaction:

```text
Begin local database transaction
        |
        +--> insert (event_id, consumer_name)
        |
        +--> duplicate key?
        |       |
        |       +--> yes: no-op and acknowledge
        |
        +--> validate event and business preconditions
        |
        +--> update owned business data
        |
        +--> insert new outbox event when required
        |
Commit
        |
        v
Kafka offset may be acknowledged
```

The processed-event insert, business update and any resulting outbox insert must
commit in the same local transaction.

Incorrect:

```text
save processed_events
commit

update business data
commit
```

If the second transaction fails, a retry is incorrectly ignored.

`consumer_name` must be stable across deployments and unique for a logical
consumer. It must not contain a random instance identifier.

---

# Consumer Validation

Idempotency is necessary but does not make an event trusted.

Before applying a business effect, consumers validate:

- Envelope structure.
- Supported `eventType`.
- Supported `eventVersion`.
- Required identifiers.
- `aggregateId` consistency.
- Kafka key consistency.
- Timestamp format.
- Correlation and causation identifiers.
- Payload structure.
- Numeric ranges.
- Monetary value and currency.
- Current business-state preconditions.
- Producer identity where infrastructure provides it.

A delayed `payment-success` must not revive a cancelled or expired booking.

An event with a duplicate `eventId` but different content is invalid and must not
be treated as a normal duplicate.

---

# Failure Scenarios

| Failure                                       | Expected result                                      |
| --------------------------------------------- | ---------------------------------------------------- |
| Business transaction rolls back               | Business data and outbox row both roll back          |
| Service crashes after commit, before polling  | `PENDING` row remains durable                        |
| Payload cannot be serialized before save      | Business transaction rolls back                      |
| Payload cannot be deserialized by publisher   | Future fails; row becomes `FAILED`                   |
| Kafka is unavailable                          | Row becomes `FAILED`; retry count increments         |
| Kafka succeeds, service crashes before `SENT` | Event may be published again                         |
| Database save of `SENT` fails                 | Event may be published again                         |
| Consumer receives duplicate                   | Processed-event uniqueness prevents duplicate effect |
| Scheduler crashes while row is `PROCESSING`   | Current implementation can leave row stuck           |
| Two schedulers select the same row            | Current implementation may publish concurrently      |
| Retry count reaches 5                         | Current scheduler stops selecting the row            |

The last three cases require operational visibility and production hardening.

---

# Dead-Letter Strategy

DLT support is not implemented in the current R14 `common-outbox`.

Do not document `booking-dlt`, `payment-dlt` or `inventory-dlt` as active until
topics, ownership, ACLs, retention, payload rules, monitoring and replay behavior
are implemented and tested.

A future DLT record should preserve:

```text
original eventId
original topic
original partition and offset where available
event type and version
aggregate identifier
bounded failure category
failure timestamp
retry count
approved correlation metadata
```

It must not contain:

```text
credentials
tokens
private keys
full card data
complete stack traces
unnecessary personal data
unbounded exception messages
```

DLT publication is not a business compensation action. Saga compensation must be
defined by the owning services and their business events.

---

# Manual Replay

Manual replay is a privileged administrative operation.

Requirements:

- Explicit authorization.
- Audit record containing actor, reason, target and result.
- Selection by stable event ID.
- Preservation of original `eventId`.
- Preservation of aggregate key and event version.
- Validation that payload and topic remain supported.
- Protection against concurrent automated publication.
- Idempotent consumers.
- Bounded batch size.
- Dry-run or inspection capability before large replay.
- Metrics and alerting.

Manual replay must not:

- Edit a failed payload silently.
- Generate a new ID merely to bypass deduplication.
- Change business state directly.
- Reset all failed events without a bounded target.
- expose protected payload data in an administrative UI.

If correcting an invalid historical event requires a new business fact, create a
new explicitly owned event rather than disguising it as a retry.

---

# Security

Outbox payloads receive the same protection as integration events.

Rules:

- Only the owning service database user may access its outbox table.
- The publication worker uses least-privilege Kafka credentials.
- Producer ACLs permit only topics owned by that service.
- Payloads contain only data required by approved consumers.
- Payloads must not contain secrets.
- Logs must not dump complete protected payloads.
- Backups containing outbox rows must be protected.
- Administrative inspection and replay require privileged access.
- Manual replay is audited.
- Retention and deletion follow data-protection requirements.
- Kafka and database traffic use protected transport outside isolated local
  development.

Prohibited payload data:

```text
Passwords
Password hashes
Access tokens
Refresh tokens
Database credentials
Private signing keys
OAuth2 client secrets
Payment provider secrets
CVV values
Full card numbers
Unnecessary personal data
Internal stack traces
```

If `last_error` is introduced later, it must store a bounded sanitized reason,
not credentials, raw payloads or complete exception traces.

---

# Serialization

Payload JSON uses the approved `common-jackson` `ObjectMapper`.

Rules:

- Use ISO-8601 timestamps.
- Disable timestamp-array serialization.
- Do not enable unsafe global polymorphic default typing.
- Do not deserialize arbitrary producer class names.
- Use immutable DTOs or records.
- Do not serialize JPA lazy proxies.
- Do not serialize exceptions into the event.
- Validate maximum payload size.
- Keep payloads backward compatible within an event version.
- Introduce a new version for breaking contract changes.

Serialization should occur before the transaction commits. A payload that cannot
be serialized must prevent creation of a business state that has no publishable
event.

---

# Retention and Cleanup

Cleanup is not implemented in the current module.

Before cleanup is enabled, define per-environment retention for:

```text
SENT outbox rows
Terminal failed rows
Processed-event rows
DLT records
Audit records
Kafka topics
Database backups
```

Cleanup rules:

- Delete only terminal rows eligible by policy.
- Never delete `PENDING` rows.
- Never delete an actively leased `PROCESSING` row.
- Do not delete unresolved failures merely because they are old.
- Use bounded batches.
- Avoid long-running transactions.
- Preserve audit requirements.
- Coordinate outbox retention with Kafka retention and replay requirements.
- Monitor cleanup failures and table growth.

Deleting `processed_events` too early permits an old Kafka message to cause a
duplicate business effect.

---

# Observability

The outbox requires metrics, structured logs and alerts.

Recommended metrics:

```text
outbox.pending.count
outbox.processing.count
outbox.failed.count
outbox.oldest.pending.age
outbox.publish.success.count
outbox.publish.failure.count
outbox.publish.duration
outbox.retry.count
outbox.stuck.processing.count
outbox.cleanup.deleted.count
```

Useful dimensions:

```text
service
environment
event_type
result
bounded_failure_category
```

Do not use `eventId`, `aggregateId`, user identifiers or raw error messages as
unbounded metric labels.

Structured logs may include:

```text
service
eventId
eventType
aggregateId where approved
retryCount
correlationId
bounded result
bounded failure category
```

Logs must not include complete payloads or secrets.

Alert examples:

- Oldest `PENDING` age exceeds the service objective.
- `FAILED` count grows continuously.
- Events remain `PROCESSING` beyond their lease.
- Publish failure rate exceeds a threshold.
- No successful publication occurs while pending rows exist.
- Retry exhaustion occurs.
- Cleanup stops and table size grows unexpectedly.

---

# Configuration

Current property:

```yaml
cinema:
    outbox:
        delay: 5000
```

The current scheduler defaults to 5 seconds when the property is missing.

Future validated configuration should include:

```yaml
cinema:
    outbox:
        enabled: true
        delay: 5s
        batch-size: 100
        max-retries: 5
        retry:
            initial-delay: 1s
            max-delay: 5m
            jitter: true
        processing-lease: 1m
        retention: 7d
```

This example defines the target configuration shape only. Properties not bound
and implemented in code must not be treated as active.

Configuration must:

- Have safe defaults for local development.
- Be validated at startup.
- Reject negative delays and invalid batch sizes.
- Be environment-specific where necessary.
- Not contain credentials.
- Not silently disable publication in production.

---

# Testing Requirements

## Unit tests

Verify:

- A new entity starts as `PENDING`.
- `markProcessing()` changes status to `PROCESSING`.
- `markSent()` changes status to `SENT` and sets `publishedAt`.
- `markFailed()` changes status to `FAILED` and increments `retryCount`.
- `canRetry()` is true below the maximum.
- `canRetry()` is false at the maximum.
- Publisher creates the expected event envelope.
- Publisher uses the approved topic.
- Serialization failure completes exceptionally.
- Successful Kafka future marks the row `SENT`.
- Failed Kafka future marks the row `FAILED`.

## Repository tests

Verify with MySQL Testcontainers:

- UUID column mapping matches Flyway.
- `LONGTEXT` payload mapping is valid.
- Status enum values match the schema.
- Rows are selected in the intended order.
- Batch size is enforced.
- Eligible and ineligible statuses are separated.
- Required indexes exist.

## Transaction integration tests

Verify:

- Business update and outbox insert commit together.
- Business failure rolls back the outbox insert.
- Outbox failure rolls back the business update.
- No Kafka publication is required for the original transaction to commit.
- A resulting consumer outbox event commits with the consumer business update.

## Kafka integration tests

Verify with Kafka Testcontainers:

- A persisted event is published.
- The message contains the original `eventId`.
- Topic and aggregate key match the catalog.
- The row becomes `SENT` only after successful acknowledgement.
- Broker failure produces `FAILED`.
- Recovery republishes with the same `eventId`.
- Duplicate delivery creates one business effect.
- Unsupported event versions are rejected safely.

## Concurrency tests

Before multi-instance production use, verify:

- Two schedulers do not own the same active claim.
- Database locks are not held while waiting for Kafka.
- Expired claims are recovered.
- Live claims are not stolen.
- Multiple aggregate events preserve required ordering.

## Security tests

Verify:

- Event payloads contain no prohibited fields.
- Logs do not contain complete payloads or credentials.
- Producer credentials cannot publish unauthorized topics.
- Consumer credentials cannot publish merely because they can read.
- Manual replay requires authorization and produces an audit record.

The current `common-outbox` module has no test source directory. R14 should not
be considered production-hardened until the required automated tests are added.

---

# Implementation Gaps at R23

R14 provides the reusable outbox foundation, but the following items remain:

- Align `OutboxEventMessage` with the canonical event envelope.
- Persist or otherwise supply `aggregateId`, `aggregateType`, `eventVersion`,
  `occurredAt`, `correlationId` and `causationId` to the published event.
- Send an explicit aggregate Kafka key.
- Replace duplicated constants with validated properties.
- Implement real delayed retry using `next_retry_at`.
- Add exponential backoff and jitter.
- Persist a bounded sanitized failure category when required.
- Define terminal retry exhaustion behavior.
- Recover abandoned `PROCESSING` rows.
- Implement atomic multi-instance claims and leases.
- Add DLT behavior only after an explicit design is accepted.
- Add authorized and audited manual replay.
- Add retention and cleanup jobs.
- Add metrics, structured logs and alerts.
- Add unit, repository, transaction, Kafka, concurrency and security tests.
- Add Flyway migrations to each outbox-owning business service.
- Verify topic ACLs and database least privilege.

These are implementation tasks, not features that may be marked complete merely
by documenting them.

---

# Required Review Checklist

Before an outbox-owning service is complete:

- [ ] Business state and outbox row commit in one local transaction
- [ ] Outbox row uses UUID v7
- [ ] Event ID remains stable across retries
- [ ] Topic exists in `docs/07_EVENT_CATALOG.md`
- [ ] Producer owns the event
- [ ] Kafka key is the approved aggregate ID
- [ ] Published envelope matches the canonical event contract
- [ ] Payload uses the approved Jackson configuration
- [ ] Payload contains no secrets or prohibited payment data
- [ ] New rows start as `PENDING`
- [ ] Rows become `SENT` only after Kafka acknowledgement
- [ ] Failed publication increments retry state
- [ ] Retry exhaustion is observable
- [ ] Abandoned `PROCESSING` rows can recover
- [ ] Multi-instance row claiming is safe
- [ ] Consumers are transactional and idempotent
- [ ] Processed-event uniqueness is enforced by the database
- [ ] Consumer state validation rejects stale transitions
- [ ] Retry and replay preserve the original event ID and key
- [ ] Manual replay is authorized and audited
- [ ] Cleanup retention is explicit
- [ ] Logs and metrics do not expose protected payload data
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

---

## Separate outbox transaction

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveOutboxEvent(...) {
    outboxRepository.save(...);
}
```

when the business state uses another transaction.

---

## New ID on retry

```java
event.setId(UUID.randomUUID());
publisher.publish(event);
```

---

## Marking sent before acknowledgement

```java
producer.send(topic, message);
event.markSent();
outboxRepository.save(event);
```

---

## Shared outbox database

```text
Booking Service
Payment Service
Inventory Service
        |
        v
one shared outbox database
```

---

## Non-idempotent consumer

```java
@KafkaListener(...)
public void consume(Event event) {
    paymentService.charge(event);
}
```

without processed-event protection and provider idempotency.

---

## Sensitive payload

```json
{
    "bookingId": "...",
    "accessToken": "...",
    "paymentApiKey": "...",
    "cardNumber": "..."
}
```

---

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

Find direct Kafka publication in business services:

```bash
git grep -n -E \
    "KafkaTemplate|kafkaTemplate\\.send|producer\\.send" \
    -- services
```

Every business publication must be reviewed to confirm whether it belongs behind
the outbox.

Find outbox status inconsistencies:

```bash
git grep -n -E \
    "NEW|PENDING|PROCESSING|SENT|FAILED" \
    -- common services docs
```

Find event IDs regenerated during retry:

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

Find prohibited sensitive fields in events:

```bash
git grep -n -i -E \
    "password|accessToken|refreshToken|apiKey|clientSecret|cvv|cardNumber" \
    -- common services
```

Review matches in authentication DTOs separately from integration events.

Find service migrations:

```bash
find services \
    -path "*/src/main/resources/db/migration/*.sql" \
    -type f \
    -print
```

Verify formatting:

```bash
git diff --check
```

Run the complete build:

```bash
mvn clean verify
```

---

# Related Documentation

See:

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
docs/decisions/
```

The Event Catalog owns event names, producers and consumers.

Database Design owns service data boundaries.

Security Architecture owns authentication, authorization, secret and data
protection requirements.

This document owns the reliable publication contract between a local service
transaction and Kafka.
