# ADR-011 Business Event Boundary

Status

Accepted

---

## Decision

Business events belong to business services.

common-outbox contains only infrastructure.

Examples

BookingCreatedEvent

belongs to booking-service.

PaymentSucceededEvent

belongs to payment-service.

SeatReservedEvent

belongs to inventory-service.

---

## Reason

Clear ownership.

No business coupling inside common modules.
