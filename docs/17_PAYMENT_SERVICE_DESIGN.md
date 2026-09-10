# Payment Service Design

**Version:** R27.6
**Status:** Authenticated webhook ingress and idempotent provider-result application implemented
**Last updated:** 2026-09-10

---

## 1. Purpose

Payment Service owns payment attempts, provider interaction, payment state,
refund state, webhook evidence, Payment Outbox records, and Payment
consumer-processing records.

This document defines the implementation baseline for R27. It must be applied
together with:

- `docs/02_ARCHITECTURE.md`
- `docs/05_CODING_CONVENTIONS.md`
- `docs/06_DATABASE_DESIGN.md`
- `docs/07_EVENT_CATALOG.md`
- `docs/08_SECURITY.md`
- `docs/09_OUTBOX.md`
- `docs/12_DEPENDENCY_RULES.md`
- `docs/16_BOOKING_SERVICE_DESIGN.md`
- ADR-005, ADR-006, ADR-007, ADR-011, ADR-012, and ADR-013

R27 completes the principal Booking–Payment Saga success and failure paths.
Notification delivery remains R28 scope.

---

## 2. Starting Point

At R27.1 start:

- `services/payment-service` contains only a registered Maven POM;
- Booking Service publishes canonical `payment-requested` version `1`;
- Payment Service runtime code, migrations, consumers, provider adapters,
  webhooks, Outbox events, and APIs are not implemented;
- Booking does not yet consume `payment-succeeded` or `payment-failed`;
- Inventory does not yet consume `booking-confirmed` or
  `seat-release-requested`;
- production payment-provider selection remains deferred.

Documentation must not claim those runtime paths are implemented until their
tests and the root Maven reactor pass.

---

## 3. Ownership

Payment Service owns:

```text
payments
payment_transactions
payment_provider_webhook_events
processed_events
outbox_events
```

Payment Service owns:

- payment-attempt lifecycle;
- amount and currency verification at its boundary;
- provider selection through an internal port;
- provider idempotency keys;
- safe provider references;
- authenticated provider-webhook verification;
- duplicate provider-callback protection;
- payment success and terminal failure decisions;
- refund and reconciliation state;
- Payment Outbox records;
- Payment consumer-processing records;
- financial administrative audit evidence.

Payment Service does not own:

```text
bookings
booking_seats
show_seats
users
notifications
```

`bookingId` and `userId` are external UUID references. They must not have
physical foreign keys or JPA relationships to another service database.

---

## 4. Service Boundaries

Payment Service must not:

- depend on Booking Service or Inventory Service as a Maven module;
- import another service's entity, repository, service, or internal event class;
- connect to `cinema_booking_db` or `cinema_inventory_db`;
- update Booking or ShowSeat state directly;
- trust amount, currency, status, or provider outcome without validation;
- call an external provider while holding a database transaction or lock;
- generate a new provider idempotency key when retrying the same operation;
- publish directly to Kafka after committing payment state;
- store CVV, full card numbers, access credentials, raw webhook secrets, or
  unrestricted provider payloads;
- expose JPA entities or provider-native error objects from APIs;
- convert an unknown provider outcome into a terminal failure silently.

Payment and provider operations use local transactions only. Cross-service
continuation occurs through versioned Kafka events.

---

## 5. Canonical Event Contracts

### 5.1 Consumed event

Payment Service consumes:

```text
payment-requested
```

Version `1` payload:

```text
bookingId
userId
amount
currency
paymentAttempt
holdExpiresAt
requestedAt
```

The Kafka partition key is `bookingId`.

### 5.2 Produced events

Payment Service produces one terminal result for an accepted payment attempt:

```text
payment-succeeded
payment-failed
```

Both use:

```text
aggregateType = PAYMENT
aggregateId = paymentId
partitionKey = bookingId
eventVersion = 1
correlationId = source payment-requested correlationId
causationId = source payment-requested eventId
```

Each result receives a new UUID v7 event ID and trusted server timestamp.

`payment-failed` version `1` is terminal for the Booking Saga. Therefore an
emitted version `1` failure must use:

```text
retryable = false
```

Retryable technical provider failures remain internal Payment state and do not
produce a terminal event until the provider result is known or the approved
retry/expiration policy reaches a terminal decision.

---

## 6. Payment Aggregate

Approved Payment fields:

```text
id
bookingId
userId
paymentAttempt
amount
currency
provider
status
refundStatus
providerReference
failureCode
failureMessage
holdExpiresAt
requestedAt
completedAt
sourceEventId
correlationId
version
createdAt
updatedAt
```

Approved Payment statuses:

```text
RECEIVED
PROCESSING
PENDING_PROVIDER
SUCCEEDED
FAILED
EXPIRED
RECONCILIATION_REQUIRED
```

Approved refund statuses:

```text
NOT_REQUESTED
PENDING
SUCCEEDED
FAILED
```

Normal transitions:

```text
RECEIVED -> PROCESSING
PROCESSING -> PENDING_PROVIDER
PROCESSING -> SUCCEEDED
PROCESSING -> FAILED
RECEIVED -> EXPIRED
PENDING_PROVIDER -> SUCCEEDED
PENDING_PROVIDER -> FAILED
PENDING_PROVIDER -> RECONCILIATION_REQUIRED
SUCCEEDED -> RECONCILIATION_REQUIRED
```

`RECONCILIATION_REQUIRED` is used when provider outcome and Booking hold timing
cannot be safely reconciled automatically, including confirmed provider success
observed only after `holdExpiresAt`.

An unknown provider outcome is not equivalent to `FAILED`.

---

## 7. Payment Transaction Model

`payment_transactions` records provider-facing operations and webhook evidence.

Approved transaction types:

```text
CHARGE
REFUND
RECONCILIATION
```

Approved transaction statuses:

```text
READY
PROCESSING
PENDING_PROVIDER
SUCCEEDED
FAILED
```

Approved fields:

```text
id
paymentId
provider
transactionType
attemptNumber
status
amount
currency
idempotencyKey
providerReference
providerEventId
failureCode
failureMessage
requestedAt
completedAt
processingOwner
processingExpiresAt
createdAt
updatedAt
```

Only `payment_transactions.payment_id -> payments.id` is a physical foreign
key because both tables belong to Payment Service.

Provider request and response bodies are not stored by default. Any approved
evidence must be allowlisted, bounded, sanitized, and free of credentials or
card data.

---

## 8. Database Constraints

The R27 Flyway schema must enforce at least:

```text
payments(booking_id, payment_attempt) UNIQUE
payments(source_event_id) UNIQUE
payment_transactions(provider, idempotency_key) UNIQUE
payment_transactions(provider, provider_event_id) UNIQUE when event ID exists
payment_provider_webhook_events(provider, provider_event_id) UNIQUE
processed_events(event_id, consumer_name) UNIQUE
```

Additional requirements:

- IDs use UUID v7 stored as `BINARY(16)`;
- `amount` uses `DECIMAL(19, 2)` and is non-negative;
- `currency` is a normalized three-letter code;
- `payment_attempt` and transaction `attempt_number` are positive;
- state enums are stored as strings and constrained to approved values;
- timestamps use microsecond precision and UTC-compatible mappings;

```markdown
- provider reference uniqueness is scoped by provider;
- Outbox schema matches `common-outbox` exactly;
- Hibernate uses `ddl-auto: validate` and Flyway owns DDL.

`payment_provider_webhook_events` is the authoritative provider-callback
idempotency history. `payment_transactions.provider_event_id` records the latest
callback evidence applied to the transaction; it does not replace the immutable
callback marker history.
```

---

### R27.3 implementation state

R27.3 implements:

- `Payment` and `PaymentTransaction` persistence mappings;
- Payment and transaction status enums;
- Flyway migrations for `payments`, `payment_transactions`,
  `processed_events`, and `outbox_events`;
- Payment-attempt, source-event, provider-operation, provider-reference,
  provider-event, and consumer-event uniqueness boundaries;
- UUID `BINARY(16)`, `DECIMAL(19, 2)`, normalized currency, positive-attempt,
  state, completion, and processing-lease constraints;
- Payment and transaction repositories;
- pessimistic Payment and transaction lookup boundaries;
- MySQL Testcontainers migration and repository verification.

R27.3 does not implement:

- `payment-requested` validation or Kafka consumption;
- processed-event registration behavior;
- provider calls or provider-operation claiming;
- webhook handling;
- terminal payment-result Outbox creation;
- refund or reconciliation workflows.

### R27.4 implementation state

R27.4 implements:

- immutable `PaymentRequestedPayload`;
- strict JSON-to-envelope and JSON-to-payload readers;
- canonical envelope and payload validation;
- Payment-owned processed-event persistence;
- concurrent duplicate-event registration;
- transactional Payment-attempt consistency enforcement;
- atomic `RECEIVED` Payment and `READY` CHARGE creation;
- stable provider idempotency keys;
- expired Payment creation without a CHARGE;
- Kafka listener wiring;
- bounded retry and sanitized dead-letter publication;
- unit, MySQL integration, concurrency, and Kafka integration verification.

R27.4 does not implement:

- provider API calls;
- provider-operation claiming or leases;
- authenticated webhook processing;
- terminal payment-result Outbox publication;
- Booking payment-result consumption;
- refunds or reconciliation.

### R27.5 implementation state

R27.5 implements:

- immutable provider-neutral charge command and result contracts;
- explicit `SUCCEEDED`, `FAILED`, `PENDING`, and `UNKNOWN` provider outcomes;
- Payment-owned provider port and normalized provider registry;
- deterministic local/test `MOCK` provider adapter;
- stable provider reference behavior for the same idempotency key;
- bounded provider-operation claiming;
- processing-owner and processing-expiration leases;
- expired-lease recovery using the same transaction and idempotency key;
- separate indexed queries for ready and expired operations;
- MySQL `FOR UPDATE SKIP LOCKED` claiming under `READ_COMMITTED`;
- immutable claimed-operation snapshots;
- fixed `Payment -> PaymentTransaction` lock ordering;
- explicit prevention of provider execution inside database transactions;
- lease-owner verification before applying provider results;
- success, failure, pending, unknown, and late-success state handling;
- `RECONCILIATION_REQUIRED` for provider success observed after hold expiration;
- per-operation worker failure isolation;
- crash-window verification after provider acceptance and before local result commit;
- unit, transaction-boundary, MySQL concurrency, lease-recovery, and provider-idempotency verification.

The R27.5 worker has no scheduled or public trigger. Scheduled execution remains
disabled until R27.7 can persist terminal Payment state and its canonical result
Outbox event atomically.

R27.5 does not implement:

- production MoMo or VNPay credentials and network calls;
- authenticated provider webhooks;
- terminal payment-result Outbox publication;
- Booking payment-result consumption;
- automatic refund or reconciliation processing.

### R27.6 implementation state

R27.6 implements:

- immutable raw webhook request, verified callback, application-result, and
  provider-acknowledgement contracts;
- configurable webhook body-size validation;
- normalized provider allowlist and verifier registry;
- provider-specific signature, timestamp, replay, event-type, and payload
  verification boundary;
- strict separation between untrusted raw input and trusted provider results;
- `POST /api/v1/payments/webhooks/{provider}`;
- JWT-security allowlisting for the provider callback endpoint;
- provider-specific acknowledgement generation;
- Flyway-owned `payment_provider_webhook_events`;
- immutable provider callback evidence;
- `(provider, providerEventId)` callback idempotency;
- MySQL atomic duplicate registration;
- fixed `Payment -> PaymentTransaction` lock ordering;
- forward-only success, failure, pending, unknown, duplicate, stale, and
  existing-result handling;
- rollback of the callback marker when result application fails;
- unit, MVC security, MySQL race, rollback, and full HTTP boundary verification.

The webhook endpoint is `permitAll` only at the customer JWT Resource Server
boundary. It is not an unauthenticated business operation. Every supported
production provider must authenticate its callback inside its
`PaymentProviderWebhookVerifier` before the payload becomes trusted.

R27.6 does not implement:

- production MoMo or VNPay credentials, network clients, or signature adapters;
- scheduled provider-operation execution;
- atomic `payment-succeeded` or `payment-failed` Outbox publication;
- Booking payment-result consumption;
- automatic refund or reconciliation processing.

The repository currently uses a deterministic test-only verifier for HTTP
integration verification. Test provider headers and signature values must not
appear in production source or configuration.

R27.7 must persist the terminal Payment result and its canonical Outbox event in
the same local transaction before scheduled provider execution is enabled.

## 9. `payment-requested` Consumer Transaction

The listener validates the canonical envelope before domain processing:

```text
eventId is UUID v7
eventType = payment-requested
eventVersion = 1
producer = booking-service
aggregateType = BOOKING
aggregateId = payload.bookingId
partitionKey = payload.bookingId
positive paymentAttempt
non-negative amount
three-letter currency
requestedAt < holdExpiresAt
```

````

The consumer then performs one short local transaction:

```text
Insert processed-event marker
Resolve existing payment by (bookingId, paymentAttempt)
Validate duplicate payload consistency
If already expired, create terminal EXPIRED Payment without a CHARGE
Otherwise create RECEIVED Payment
Create READY CHARGE transaction with stable provider idempotency key
Persist source event ID and correlation ID
Commit
```

Through R27.4, expired request handling persists the terminal Payment state but
does not yet publish `payment-failed`. R27.7 must add terminal result Outbox
publication atomically with the approved terminal-result transaction before R27
closure.

The transaction must not call the provider.

A duplicate event with the same event ID is a no-op. A different event ID for
the same `(bookingId, paymentAttempt)` is accepted only when its normalized
payload exactly matches the existing Payment. A mismatch is a contract conflict
and must not create another payment or charge.

---

## 10. Provider Execution Boundary

A Payment-owned worker claims bounded `READY` or retryable provider operations
using a short database transaction and a processing lease.

The flow is:

```text
Claim transaction row and commit
Call provider outside database transaction
Open result transaction
Lock Payment and transaction
Verify processing owner and current state
Persist sanitized provider outcome
Create terminal Outbox event only for a terminal outcome
Commit
```

Provider calls must never hold database locks.

If the process stops after the provider accepted a charge but before Payment
state commits, the retry uses the same provider idempotency key. A provider
adapter without idempotent request support is not approved for automatic retry.

The initial deterministic local/test adapter is:

```text
MOCK
```

It exists for contract and Saga verification only. It does not make the system
production payment-provider ready.

---

## 11. Provider Port

Provider-specific code remains behind a Payment-owned port. The domain and
application services must not depend on a provider SDK.

Conceptual operations:

```text
initiateCharge(command, idempotencyKey)
queryCharge(providerReference, idempotencyKey)
requestRefund(command, idempotencyKey)
verifyWebhook(headers, rawBody)
parseWebhook(rawBody)
```

Approved provider result categories:

```text
SUCCEEDED
FAILED
PENDING
UNKNOWN
```

`UNKNOWN` requires safe retry or reconciliation. It must not produce
`payment-failed` automatically.

---

## 12. Provider and Consumer Idempotency

Four idempotency boundaries are distinct:

| Boundary           | Key                           | Purpose                                    |
| ------------------ | ----------------------------- | ------------------------------------------ |
| Kafka delivery     | `(eventId, consumerName)`     | Prevent duplicate event effects            |
| Payment attempt    | `(bookingId, paymentAttempt)` | Prevent duplicate Payment aggregates       |
| Provider operation | `(provider, idempotencyKey)`  | Prevent duplicate provider charges/refunds |
| Provider webhook   | `(provider, providerEventId)` | Prevent duplicate callback effects         |

A Kafka processed-event row alone cannot prevent a second provider charge after
a crash. Provider idempotency is mandatory.

Client idempotency keys for future refund APIs remain separate from Kafka event
IDs and provider idempotency keys.

---

## 13. Payment Success

A terminal successful provider result performs one local transaction:

```text
Lock Payment aggregate
Verify current Payment state
Verify provider amount and currency
Verify result belongs to the claimed provider operation
Persist safe provider reference
Change Payment -> SUCCEEDED
Complete CHARGE transaction
Create payment-succeeded Outbox event
Commit
```

Canonical payload:

```text
paymentId
bookingId
amount
currency
provider
providerReference
paidAt
```

If success is first observed at or after `holdExpiresAt`, Payment must not
silently publish normal success. It enters `RECONCILIATION_REQUIRED` and follows
the approved reconciliation/refund policy.

---

## 14. Payment Failure

A confirmed terminal failure performs one local transaction:

```text
Lock Payment aggregate
Verify current Payment state
Complete CHARGE transaction as FAILED
Store approved stable failure code and sanitized message
Change Payment -> FAILED or EXPIRED
Create payment-failed Outbox event with retryable=false
Commit
```

Approved version `1` failure codes are:

```text
PAYMENT_DECLINED
PAYMENT_TIMEOUT
PROVIDER_UNAVAILABLE
INVALID_PAYMENT_REQUEST
RESERVATION_EXPIRED
DUPLICATE_PAYMENT
```

`PROVIDER_UNAVAILABLE` becomes terminal only after the approved retry policy is
exhausted and the provider outcome is known not to have succeeded. Ambiguous
outcomes require reconciliation.

Internal stack traces, credentials, provider-native objects, and unrestricted
provider messages are prohibited from the event.

---

## 15. Webhook Security and Processing

Provider webhook endpoints do not use customer bearer authentication. They use
provider-specific request authentication.

| Request                                      | R27.6 rule                            |
| -------------------------------------------- | ------------------------------------- |
| `OPTIONS /**`                                | permit for CORS preflight             |
| `/actuator/health`, `/actuator/info`         | permit                                |
| `GET /api/v1/payments/{paymentId}`           | require `payment:read`                |
| `POST /api/v1/payments/webhooks/{provider}`  | permit JWT; require provider verifier |
| refund, reconciliation, and all other routes | deny                                  |

The endpoint contract is provider-adapter owned under:

```text
POST /api/v1/payments/webhooks/{provider}
```

Processing performs:

1. capture the raw request body and headers;
2. reject an empty or oversized body;
3. normalize and resolve the provider through the verifier registry;
4. authenticate the provider request before trusting any callback field;
5. parse the provider payload into an immutable verified result;
6. validate provider event ID, provider reference, outcome, amount, currency,
   timestamps, and bounded failure evidence;
7. locate the CHARGE transaction from its provider reference;
8. lock `Payment` before `PaymentTransaction`;
9. verify trusted callback data against locally persisted financial data;
10. atomically register `(provider, providerEventId)`;
11. apply only an allowed forward transition;
12. retain duplicate and stale callback evidence without repeating a transition;
13. return the provider-specific acknowledgement.

R27.6 does not create `payment-succeeded` or `payment-failed`. R27.7 adds the
terminal Outbox event to the same transaction that commits the terminal Payment
state. Until then, scheduled provider execution remains disabled.

Webhook logs must not include signatures, secrets, authorization headers, card
data, or unrestricted raw request bodies.

---

## 16. Booking Result Integration

R27 adds Booking consumers for:

```text
payment-succeeded
payment-failed
```

`payment-succeeded` handling commits atomically:

```text
processed-event marker
RESERVED -> CONFIRMED
confirmedAt
booking-confirmed Outbox event
```

It validates Payment ID, Booking ID, amount, currency, expected Booking state,
event type/version, correlation, and causation metadata.

`payment-failed` handling commits atomically:

```text
processed-event marker
RESERVED -> PAYMENT_FAILED
seat-release-requested Outbox event
```

Only terminal version `1` payment failures are accepted for this transition.

A delayed payment success for `CANCELLED`, `EXPIRED`, or `PAYMENT_FAILED`
requires reconciliation and must not restore or confirm the Booking silently.
Duplicate and competing results must create at most one Booking transition and
one resulting Outbox event.

---

## 17. Inventory Saga Integration

R27 adds Inventory consumers for:

```text
booking-confirmed
seat-release-requested
booking-cancelled
booking-expired
```

`booking-confirmed` changes only seats that are:

```text
status = HELD
held_by_booking_id = event.bookingId
```

The transition is:

```text
HELD -> BOOKED
```

Release commands and lifecycle events change only matching held seats:

```text
HELD -> AVAILABLE
```

They must never release `BOOKED` seats, another Booking's hold, or a newer hold.

R26 cancellation and expiration do not also publish
`seat-release-requested`; Inventory consumes their lifecycle events directly.

---

## 18. Refund and Reconciliation

Refunds and reconciliation remain Payment-owned operations.

Before enabling an administrative refund endpoint, R27 must add explicit
permissions to the User Service authority catalog:

```text
payment:refund
payment:reconcile
```

Payment Service must then independently enforce the permission and record a
durable financial audit event.

Refund rules:

- refund only an eligible successful Payment;
- use a distinct stable provider idempotency key;
- never infer refund success from an HTTP timeout;
- persist the provider reference and safe outcome;
- serialize competing refund requests;
- prevent refunded amount from exceeding paid amount;
- keep provider credentials and raw responses out of audit records;
- require reconciliation for ambiguous outcomes.

The exact production provider and commercial refund policy remain deferred.

---

## 19. API and Security Boundary

Payment Service is an independent OAuth2 Resource Server for customer and
administrative APIs.

Initial protected query endpoint planned for R27:

```text
GET /api/v1/payments/{paymentId}
```

It requires `payment:read` and ownership derived from the validated JWT subject.
A caller must not supply another `userId` to bypass ownership.

Refund and reconciliation endpoints remain disabled until their permissions,
audit model, idempotency request contract, and provider behavior are implemented
and tested.

Publicly reachable endpoints are deny-by-default except:

- approved health/info endpoints;
- explicitly configured provider webhook paths protected by provider signature
  verification.

Payment Service uses the common exception and response contracts for business
APIs. Provider protocol acknowledgements may use the exact provider-required
format and must not be wrapped when that would violate the provider contract.

---

## 20. Transactional Outbox and Processed Events

Payment Service uses `common-outbox` without a service-specific fork.

Required guarantees:

- Payment state and result Outbox event commit atomically;
- event IDs remain stable across publication retry;
- Kafka acknowledgement controls `SENT` state;
- multiple Payment instances claim Outbox rows safely;
- provider calls never execute within Outbox claim or acknowledgement
  transactions;
- state-changing Kafka consumers use `(event_id, consumer_name)` uniqueness;
- processed markers, domain changes, and resulting Outbox events commit or roll
  back together.

At-least-once publication remains the delivery model.

---

## 21. Initial Configuration Contract

Expected configuration names:

```text
PAYMENT_SERVICE_PORT
PAYMENT_DB_URL
PAYMENT_DB_USERNAME
PAYMENT_DB_PASSWORD
CONFIG_SERVER_URL
EUREKA_SERVER_URL
KAFKA_BOOTSTRAP_SERVERS
CINEMA_AUTH_ISSUER
CINEMA_AUTH_JWK_SET_URI
CINEMA_AUTH_AUDIENCE
PAYMENT_KAFKA_ENABLED
PAYMENT_REQUESTED_TOPIC
PAYMENT_REQUESTED_CONSUMER_GROUP
PAYMENT_PROVIDER
PAYMENT_PROVIDER_OPERATION_BATCH_SIZE
PAYMENT_PROVIDER_OPERATION_LEASE_DURATION
PAYMENT_PROVIDER_RETRY_INTERVAL
PAYMENT_PROVIDER_MAXIMUM_RETRIES
```

Provider credentials use provider-specific environment or secret-manager
configuration and must not have development defaults containing real secrets.

Initial defaults:

```text
port = 8085
database = cinema_payment_db
payment-requested topic = payment-requested
provider = MOCK for local/test only
```

---

## 22. Testing Requirements

R27 verification must cover:

- Payment application context and independent Resource Server wiring;
- Flyway migration, UUID types, indexes, checks, and unique constraints;
- canonical `payment-requested` validation;
- request expiration before provider initiation;
- duplicate same-event delivery;
- distinct events for the same Booking attempt;
- conflicting duplicate payload rejection;
- provider call outside database transactions;
- stable provider idempotency key across retries and restarts;
- crash window after provider acceptance and before local result commit;
- provider success, terminal failure, pending, timeout, and unknown outcomes;
- no terminal event for retryable or ambiguous provider outcomes;
- exactly one payment result Outbox event;
- webhook signature, timestamp, replay, payload-size, and duplicate-event checks;
- webhook and worker race ordering;
- success observed before and after `holdExpiresAt`;
- sanitized payload, logs, failure messages, and persisted evidence;
- Booking success and failure consumer atomicity;
- competing payment success/failure results;
- delayed results after cancellation or expiration;
- Inventory confirmation and conditional release ownership checks;
- duplicate `booking-confirmed`, lifecycle, and release events;
- refund idempotency and reconciliation controls when enabled;
- Payment dependency-boundary verification;
- Payment never accessing Booking or Inventory tables;
- focused and root `mvn clean verify`.

---

## 23. Implementation Order

| Checkpoint | Scope                                                       | Status  |
| ---------- | ----------------------------------------------------------- | ------- |
| R27.1      | Payment architecture and contract closure                   | DONE    |
| R27.2      | Payment Service bootstrap and Resource Server security      | DONE    |
| R27.3      | Payment aggregate and Flyway schema                         | DONE    |
| R27.4      | `payment-requested` validation and idempotent consumption   | DONE    |
| R27.5      | Provider port, operation worker, and provider idempotency   | DONE    |
| R27.6      | Authenticated webhook and provider-result processing        | DONE    |
| R27.7      | `payment-succeeded` and `payment-failed` Outbox publication | NEXT    |
| R27.8      | Booking payment-result consumers                            | PLANNED |
| R27.9      | Inventory confirmation and compensation consumers           | PLANNED |
| R27.10     | Refund, reconciliation, permissions, and audit controls     | PLANNED |
| R27.11     | Kafka retry, DLT, and publication verification              | PLANNED |
| R27.12     | Saga integration, race, and concurrency verification        | PLANNED |
| R27.13     | Stabilization, documentation, and closure                   | PLANNED |

No checkpoint may introduce real provider credentials, direct cross-service
database access, non-idempotent charges, or direct Kafka publication after a
domain commit.

---

## 24. R27.1 Exit Criteria

R27.1 may be marked complete when:

- Payment ownership and non-ownership are explicit;
- consumed and produced event contracts match the Event Catalog;
- terminal versus retryable failure semantics are explicit;
- provider calls are separated from database transactions;
- provider, Kafka, Payment-attempt, and webhook idempotency are distinct;
- Payment and transaction states are explicit;
- delayed success and unknown outcomes route to reconciliation;
- schema constraints and external-reference rules are explicit;
- webhook trust and raw-body verification rules are explicit;
- Booking and Inventory R27 responsibilities are explicit;
- refund permissions and audit prerequisites are explicit;
- implementation checkpoints and test expectations are explicit;
- affected Roadmap and Event Catalog sections are synchronized;
- no Payment runtime implementation is claimed complete.

---

## 25. Deferred Decisions

The following choices remain deferred without weakening this design:

- production payment-provider vendor;
- production provider credential-delivery mechanism;
- provider-hosted checkout UX and return URLs;
- commercial cancellation and refund policy;
- automated reconciliation schedule;
- PCI compliance scope beyond the current prohibition on card-data storage;
- multi-currency conversion policy;
- partial capture and partial refund support;
- chargeback and dispute workflows.

Every later choice must preserve database ownership, provider idempotency,
webhook authentication, sensitive-data minimization, Transactional Outbox, and
idempotent consumer rules.
````
