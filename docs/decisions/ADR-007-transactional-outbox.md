# ADR-007 Transactional Outbox

Status

Accepted

---

## Decision

Every business event must first be stored in outbox_events.

Publishing to Kafka is handled asynchronously.

---

## Reason

No distributed transaction.

Reliable delivery.

Retry capability.

Failure isolation.

Persistence never depends on Kafka.
