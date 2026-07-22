# ADR-012 Database per Service

Status

Accepted

---

## Decision

Each service owns its own database schema.

No service accesses another service database.

---

## Reason

Independent deployment.

Independent scaling.

Loose coupling.

True microservice architecture.

Consistency through Event Driven communication.
