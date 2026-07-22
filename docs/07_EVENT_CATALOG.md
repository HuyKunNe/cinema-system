# Event Catalog

Version: R14

---

# Principles

All business communication uses Kafka.

No synchronous REST communication for business workflow.

Events are immutable.

Events must be versionable.

Every event has UUID v7.

---

# Naming Convention

Topic

kebab-case

Examples

booking-created

seat-reserved

payment-success

payment-failed

booking-confirmed

inventory-restored

---

# Event Envelope

Every published event contains

eventId

aggregateId

aggregateType

eventType

occurredAt

payload

---

# Transactional Outbox

Business Transaction

↓

Outbox Event

↓

Commit

↓

Scheduler

↓

Kafka

---

# Current Event Flow

Booking Service

publishes

seat-reserved

↓

Payment Service

consumes

↓

payment-success

↓

Booking Service

consumes

↓

booking-confirmed

↓

Inventory Service

updates seat status

---

# Planned Events

## seat-reserved

Producer

Booking Service

Consumers

Payment Service

Inventory Service

Purpose

Start Saga.

---

## payment-success

Producer

Payment Service

Consumers

Booking Service

Notification Service

Purpose

Confirm booking.

---

## payment-failed

Producer

Payment Service

Consumers

Booking Service

Inventory Service

Purpose

Cancel reservation.

---

## booking-confirmed

Producer

Booking Service

Consumers

Notification Service

Purpose

Send ticket.

---

## booking-cancelled

Producer

Booking Service

Consumers

Inventory Service

Notification Service

Purpose

Release seats.

---

## inventory-restored

Producer

Inventory Service

Consumers

Booking Service

Purpose

Finish compensation.

---

# Retry Policy

Producer

Outbox Scheduler

↓

Kafka

↓

ACK

Success

↓

SENT

Failure

↓

FAILED

↓

Retry

Retry Count

++

Next Retry Time

updated.

---

# Dead Letter Strategy

Future

DLQ

booking-dlt

payment-dlt

inventory-dlt

---

# Event Versioning

Future policy

Event payload must support versioning.

Example

BookingCreatedEventV1

BookingCreatedEventV2

Old consumers continue working.

---

# Event Ordering

Ordering guaranteed

inside one partition.

Partition key

Aggregate ID

Booking ID

---

# Idempotent Consumer

Every consumer

stores

event_id

consumer_name

Duplicate event

↓

Ignored

---

# Business Event Rule

Business events

must NEVER be placed inside

common-outbox.

Correct

booking-service

BookingCreatedEvent

payment-service

PaymentSuccessEvent

inventory-service

SeatReleasedEvent

common-outbox

contains only

Outbox infrastructure.

---

# Future Events

Notification Sent

Refund Completed

Movie Updated

Showtime Updated

User Registered

Coupon Applied

Reward Granted

Review Created

Recommendation Generated

These events belong to corresponding services.
