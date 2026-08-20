# Booking Service Design

**Version:** R26.1
**Status:** Accepted design baseline
**Last updated:** 2026-08-20

---

## 1. Purpose

Booking Service owns the customer booking aggregate and coordinates the booking
Saga through integration events.

This document defines the approved implementation baseline for R26.

It must be implemented together with:

- `docs/02_ARCHITECTURE.md`
- `docs/05_CODING_CONVENTIONS.md`
- `docs/06_DATABASE_DESIGN.md`
- `docs/07_EVENT_CATALOG.md`
- `docs/08_SECURITY.md`
- `docs/09_OUTBOX.md`
- `docs/12_DEPENDENCY_RULES.md`

---

## 2. Ownership

Booking Service owns:

```text
bookings
booking_seats
processed_events
outbox_events
```

Booking Service owns:

- booking creation;
- authenticated booking ownership;
- booking lifecycle state;
- client request idempotency;
- booking expiration;
- cancellation;
- booking-owned seat snapshots;
- Booking Outbox records;
- Booking consumer-processing records;
- Booking Saga state transitions.

Booking Service does not own:

```text
show_seats
payments
users
movies
showtimes
notifications
```

Cross-service identifiers are stored as UUID values without cross-database
foreign keys or JPA associations.

---

## 3. Service Boundaries

Booking Service must not:

- accept `userId` from a create-booking request;
- read or update `cinema_inventory_db`;
- import `ShowSeat`, `ShowSeatRepository`, or another service's entity;
- call another service's repository;
- publish directly to Kafka after a domain mutation;
- confirm a booking without validating its current state;
- trust an event without validating its identifier, type and version;
- expose JPA entities from controllers.

Inventory Service remains the only owner of:

- ShowSeat availability;
- seat holds;
- hold ownership;
- hold expiration metadata;
- `HELD -> BOOKED`;
- conditional seat release.

---

## 4. Authentication and Ownership

All customer Booking APIs require an authenticated JWT.

The authoritative booking owner is:

```text
JWT sub
```

The subject must be a valid UUID v7.

Create-booking requests must not contain:

```text
userId
ownerId
customerId
```

Booking Service resolves the current user through the approved shared security
abstraction.

A customer may read or mutate only a booking whose `user_id` equals the
authenticated subject.

Administrative access requires an explicitly approved Booking permission and
must not bypass service-level ownership rules accidentally.

---

## 5. Booking Aggregate

The Booking aggregate contains:

```text
Booking
└── BookingSeat snapshots
```

Approved Booking fields:

```text
id
userId
showtimeId
clientRequestId
status
totalAmount
currency
expiresAt
confirmedAt
cancelledAt
rejectionReason
version
createdAt
updatedAt
```

Approved BookingSeat fields:

```text
id
bookingId
inventorySeatId
showtimeId
seatNumber
seatType
price
createdAt
```

`bookingId` is the only physical foreign-key relationship in the initial
Booking schema because both tables belong to Booking Service.

---

## 6. Booking Status

Approved states:

```text
PENDING
RESERVED
REJECTED
CONFIRMED
PAYMENT_FAILED
CANCELLED
EXPIRED
```

Approved normal transitions:

```text
PENDING -> RESERVED
PENDING -> REJECTED

RESERVED -> CONFIRMED
RESERVED -> PAYMENT_FAILED
RESERVED -> CANCELLED
RESERVED -> EXPIRED
```

A transition must validate the expected current state.

An event must not restore a terminal booking to an earlier state.

Terminal states are:

```text
REJECTED
CONFIRMED
PAYMENT_FAILED
CANCELLED
EXPIRED
```

The exact cancellation policy after `CONFIRMED` is deferred until an explicit
refund requirement is approved.

---

## 7. Pending Seat Request

A create-booking request contains:

```text
showtimeId
clientRequestId
seatNumbers
```

While the Booking is `PENDING`, Booking Service has not received authoritative
seat pricing from Inventory Service.

Therefore the following fields may be null while `PENDING`:

```text
bookings.total_amount
bookings.currency
booking_seats.inventory_seat_id
booking_seats.seat_type
booking_seats.price
```

The initial BookingSeat rows preserve:

```text
bookingId
showtimeId
seatNumber
```

After `seat-reserved`, Booking Service completes the authoritative seat
snapshots using the event payload.

Once the Booking reaches `RESERVED`, its seat price snapshot must not be changed
from current Inventory or Movie data.

---

## 8. Client Request Idempotency

The create-booking API requires `clientRequestId`.

The authoritative uniqueness boundary is:

```text
(user_id, client_request_id)
```

Rules:

- the same user and request ID must not create multiple bookings;
- different users may use the same request ID;
- retries return the previously created Booking;
- a reused request ID with a different normalized request payload is rejected;
- the database unique constraint is the final concurrency guarantee.

The normalized request identity includes:

```text
showtimeId
sorted distinct seatNumbers
```

Seat order supplied by the client is not semantically significant.

---

## 9. Create Booking Transaction

Create Booking uses one local transaction:

```text
Resolve authenticated user ID
Normalize and validate seat numbers
Check client request idempotency
Create PENDING Booking
Create pending BookingSeat rows
Create seat-reservation-requested Outbox row
Commit
```

Booking Service must not keep this transaction open while calling another
service.

The API returns an accepted Booking representation after the local transaction
commits.

---

## 10. Seat Reservation Result

When `seat-reserved` is consumed, Booking Service performs one transaction:

```text
Check processed event
Load Booking
Validate PENDING state
Validate showtime and requested seat set
Complete authoritative BookingSeat snapshots
Set total amount and currency
Change PENDING -> RESERVED
Insert processed-event marker
Create payment-requested Outbox row
Commit
```

When `seat-reservation-rejected` is consumed:

```text
Check processed event
Load Booking
Validate PENDING state
Store rejection reason
Change PENDING -> REJECTED
Insert processed-event marker
Commit
```

Duplicate delivery must not create duplicate state transitions or outgoing
events.

---

## 11. Expiration and Cancellation

Booking expiration is based on persisted `expires_at`.

Expiration processing must:

- select only eligible non-terminal bookings;
- validate the current state again inside the transaction;
- use safe concurrent claiming or locking;
- create the required compensation event through Transactional Outbox;
- never release Inventory rows directly.

Cancellation must validate:

- authenticated ownership;
- current Booking state;
- whether cancellation is allowed;
- whether a seat-release event is required.

---

## 12. Transactional Outbox

Booking Service uses `common-outbox`, but R26 integration is not complete until
the shared implementation supports the canonical event contract.

Required persisted and published values:

```text
eventId
aggregateId
aggregateType
eventType
eventVersion
topic
partitionKey
occurredAt
correlationId
causationId
payload
```

The Booking aggregate ID is used as the Kafka partition key for Booking Saga
events.

The hardened Outbox implementation must support:

- atomic database claiming;
- multiple application instances;
- processing leases;
- abandoned-claim recovery;
- bounded retry with backoff;
- preserved event identifiers;
- acknowledgement-based SENT transitions;
- safe persistence updates outside Kafka callback threads.

---

## 13. Idempotent Consumers

Every state-changing Booking consumer owns a logical consumer name.

The unique processing boundary is:

```text
(event_id, consumer_name)
```

The following operations commit together:

```text
processed-event insertion
Booking state change
resulting Outbox insertion
```

A duplicate-key race must be treated as duplicate delivery, not as an
uncontrolled internal error.

---

## 14. Initial API Contract

Initial endpoints:

```text
POST /api/v1/bookings
GET  /api/v1/bookings/{bookingId}
GET  /api/v1/bookings
POST /api/v1/bookings/{bookingId}/cancel
```

Create request:

```json
{
  "clientRequestId": "checkout-019c1234",
  "showtimeId": "019c1234-3333-7abc-8def-0123456789ab",
  "seatNumbers": ["H7", "H8"]
}
```

The request must not contain `userId`.

All responses use the approved shared API response contract.

---

## 15. Testing Requirements

R26 tests must cover:

- unauthenticated create request;
- authenticated ownership extraction;
- invalid JWT subject;
- empty seat list;
- duplicate seat numbers after normalization;
- seat-count limit;
- concurrent identical client requests;
- request-ID reuse with different payload;
- duplicate Kafka delivery;
- stale event handling;
- invalid Booking state transitions;
- transaction rollback;
- Outbox creation in the domain transaction;
- processed-event atomicity;
- concurrent expiration;
- cancellation ownership;
- Booking Service dependency-boundary checks;
- Booking Service never accessing `show_seats`.

---

## 16. Implementation Order

```text
R26.1  Booking architecture and contract closure
R26.2  Booking Service bootstrap and security
R26.3  Booking aggregate and Flyway schema
R26.4  Authenticated create and query APIs
R26.5  Client request idempotency
R26.6  Outbox contract hardening
R26.7  seat-reservation-requested publication
R26.8  Inventory event integration
R26.9  Reservation result handling
R26.10 Expiration and cancellation
R26.11 Payment event preparation
R26.12 Integration and concurrency verification
R26.13 Stabilization and closure
```

Payment processing remains R27.

Notification processing remains R28.
