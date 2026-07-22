# Transactional Outbox

Version: R14

---

# Purpose

Guarantee reliable event publishing without distributed transactions.

---

# Flow

Business Transaction

↓

Update Database

↓

Insert Outbox Event

↓

Commit

↓

Scheduler

↓

Kafka

↓

ACK

↓

Mark SENT

---

# Components

Outbox Entity

Outbox Repository

Outbox Service

Kafka Publisher

Scheduler

Retry Policy

---

# Event Status

NEW

↓

PUBLISHING

↓

SENT

or

FAILED

---

# Retry Strategy

Scheduler periodically scans FAILED events.

Retry until configured maximum.

Backoff supported through next_retry_at.

---

# Failure Handling

Publish failure never rolls back business transaction.

Business data remains committed.

Only event publication is retried.

---

# Design Principles

Persistence Layer

↓

Outbox

↓

Kafka

Never

Persistence

↓

Kafka

---

# AggregateType

Represents business aggregate.

Examples

BOOKING

PAYMENT

INVENTORY

---

# Event Payload

Payload is serialized JSON.

Jackson configuration comes from common-jackson.

---

# Scheduler

Runs periodically.

Finds

NEW

FAILED

Publishes events.

Updates status.

---

# Idempotency

Consumers must store processed event ids.

Duplicate events are ignored.

---

# Future

Exponential Backoff

DLQ

Monitoring

Metrics

Dashboard
