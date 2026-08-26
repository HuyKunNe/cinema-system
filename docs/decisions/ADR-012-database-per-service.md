# ADR-012 Database per Service

Status

Accepted

Last reviewed

2026-08-26

---

## Decision

Each service owns its own database schema.

No service accesses another service database.

Cross-service references use stable identifiers without physical foreign keys.
Local foreign keys remain allowed between tables owned by the same service.

Through R26, Movie, User, Inventory, and Booking own separate implemented
databases. Payment and Notification database designs remain future R27/R28
scope.

---

## Reason

Independent deployment.

Independent scaling.

Loose coupling.

True microservice architecture.

Consistency through Event Driven communication.

Booking Service stores Booking-owned seat snapshots but never queries or updates
Inventory-owned `show_seats`.
