# ADR-005 Event Driven Architecture

Status

Accepted

Last reviewed

2026-08-26

---

## Decision

Cross-service business workflows that coordinate state changes use asynchronous,
versioned Kafka events.

Synchronous REST remains allowed for approved queries, reference-data lookup,
OAuth2/OIDC protocol endpoints, and commands explicitly accepted by the
architecture. It must not create a distributed database transaction or bypass
service ownership.

R26 Booking-to-Inventory reservation coordination uses Kafka. R27 Payment and
later Saga continuations must follow the Event Catalog.

---

## Reason

Loose coupling.

Scalability.

Independent deployment.

Better fault isolation.

Local transactions remain independent.

At-least-once delivery is handled with Transactional Outbox and idempotent
consumers.
