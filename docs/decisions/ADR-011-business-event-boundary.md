# ADR-011 Business Event Boundary

Status

Accepted

Last reviewed

2026-08-26

---

## Decision

Business events belong to business services.

common-outbox contains only infrastructure.

Examples

`seat-reservation-requested`, `payment-requested`, `booking-cancelled`, and
`booking-expired`

belong to Booking Service.

`payment-succeeded` and `payment-failed`

belong to Payment Service when R27 implements them.

`seat-reserved`, `seat-reservation-rejected`, and `seat-released`

belong to Inventory Service. `seat-released` remains future integration work.

`common-kafka` and `common-outbox` may own technical envelopes, serializers,
retry, claim, and publication abstractions. They must not own business payloads,
state transitions, compensation decisions, or producer ownership.

---

## Reason

Clear ownership.

No business coupling inside common modules.

No service imports another service's internal event implementation merely to
consume its Kafka contract.
