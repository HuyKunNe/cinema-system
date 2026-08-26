# ADR-006 Saga Pattern

Status

Accepted

Last reviewed

2026-08-26

---

## Decision

Booking workflows use Saga choreography.

No centralized orchestrator.

Each participating service consumes an owned event, commits one local
transaction, and persists any resulting event through its local Outbox.

R26 implements reservation coordination through `payment-requested` publication.
Payment-result continuation is R27 scope. Cancellation and expiration publish
`booking-cancelled` and `booking-expired`; the explicit
`seat-release-requested` command is reserved for payment-failure compensation.

---

## Reason

Lower coupling.

Each service owns its own business process.

Better horizontal scalability.

Duplicate and out-of-order delivery must be handled idempotently.
