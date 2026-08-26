# ADR-007 Transactional Outbox

Status

Accepted

Last reviewed

2026-08-26

---

## Decision

Every integration event caused by a local business mutation must first be stored
in the producing service's `outbox_events` table in the same local transaction.

Publishing to Kafka is handled asynchronously.

Publication is at least once. Retries preserve the original event ID, topic,
partition key, correlation ID, and causation ID. Consumers remain responsible
for idempotency.

`common-outbox` owns claim, lease, retry, envelope, publication, and
acknowledgement infrastructure. Producing services own event meaning and payload.

---

## Reason

No distributed transaction.

Reliable delivery.

Retry capability.

Failure isolation.

Persistence never depends on Kafka.

Direct Kafka publication after committing domain state is prohibited.
