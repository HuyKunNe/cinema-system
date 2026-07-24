# Sequence Diagrams

Version: R24

---

# Booking and Seat Reservation Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Booking as Booking Service
    participant Kafka
    participant Inventory as Inventory Service
    participant Redis
    participant InventoryDB as Inventory Database

    Client->>Gateway: Create booking
    Gateway->>Booking: POST /bookings

    Booking->>Booking: Create PENDING booking
    Booking->>Booking: Store seat snapshot
    Booking->>Booking: Insert SEAT_RESERVATION_REQUESTED outbox event
    Booking-->>Client: Booking accepted

    Booking->>Kafka: seat-reservation-requested
    Kafka->>Inventory: Consume request

    Inventory->>Inventory: Check event idempotency
    Inventory->>Redis: Acquire ordered seat locks
    Redis-->>Inventory: Locks acquired

    Inventory->>InventoryDB: Validate requested show_seats

    alt All seats are AVAILABLE
        Inventory->>InventoryDB: AVAILABLE to HELD
        Inventory->>InventoryDB: Insert SEAT_RESERVED outbox event
        Inventory->>InventoryDB: Commit transaction
        Inventory->>Kafka: seat-reserved
        Kafka->>Booking: Consume reservation success
        Booking->>Booking: PENDING to RESERVED
        Booking->>Booking: Insert PAYMENT_REQUESTED outbox event
    else One or more seats are unavailable
        Inventory->>InventoryDB: Insert SEAT_RESERVATION_REJECTED outbox event
        Inventory->>InventoryDB: Commit transaction
        Inventory->>Kafka: seat-reservation-rejected
        Kafka->>Booking: Consume rejection
        Booking->>Booking: PENDING to REJECTED
    end

    Inventory->>Redis: Release seat locks
```

Booking Service owns booking state. Inventory Service exclusively owns
`show_seats`, Redis seat locks and seat-state transitions.

Booking status `RESERVED` and Inventory status `HELD` are separate concepts.

---

# Payment Success Flow

```mermaid
sequenceDiagram
    participant Booking as Booking Service
    participant Kafka
    participant Payment as Payment Service
    participant Inventory as Inventory Service
    participant Notification as Notification Service

    Booking->>Kafka: payment-requested
    Kafka->>Payment: Consume request

    Payment->>Payment: Check event idempotency
    Payment->>Payment: Process payment
    Payment->>Payment: Save payment result and outbox event
    Payment->>Kafka: payment-succeeded

    Kafka->>Booking: Consume payment success
    Booking->>Booking: RESERVED to CONFIRMED
    Booking->>Booking: Insert BOOKING_CONFIRMED outbox event
    Booking->>Kafka: booking-confirmed

    Kafka->>Inventory: Consume booking confirmation
    Inventory->>Inventory: Check event idempotency
    Inventory->>Inventory: HELD to BOOKED

    Kafka->>Notification: Consume booking confirmation
    Notification->>Notification: Send ticket notification
```

---

# Payment Failure and Seat Release Flow

```mermaid
sequenceDiagram
    participant Payment as Payment Service
    participant Kafka
    participant Booking as Booking Service
    participant Inventory as Inventory Service
    participant Redis
    participant InventoryDB as Inventory Database

    Payment->>Kafka: payment-failed
    Kafka->>Booking: Consume payment failure

    Booking->>Booking: RESERVED to CANCELLED
    Booking->>Booking: Insert SEAT_RELEASE_REQUESTED outbox event
    Booking->>Kafka: seat-release-requested

    Kafka->>Inventory: Consume release request
    Inventory->>Inventory: Check event idempotency
    Inventory->>Redis: Acquire ordered seat locks
    Redis-->>Inventory: Locks acquired

    Inventory->>InventoryDB: HELD to AVAILABLE
    Inventory->>InventoryDB: Insert SEAT_RELEASED outbox event
    Inventory->>InventoryDB: Commit transaction
    Inventory->>Redis: Release seat locks
    Inventory->>Kafka: seat-released
```

Booking expiration or cancellation follows the same
`seat-release-requested` flow.

Only Inventory Service may perform `HELD → AVAILABLE`.

---

# Transactional Outbox Flow

```mermaid
sequenceDiagram
    participant Service as Business Service
    participant Database
    participant Scheduler as Outbox Scheduler
    participant Kafka

    Service->>Database: Save aggregate changes
    Service->>Database: Save outbox event
    Database-->>Service: Commit local transaction

    loop Poll publishable events
        Scheduler->>Database: Claim NEW events
        Scheduler->>Kafka: Publish event
        Kafka-->>Scheduler: Acknowledge
        Scheduler->>Database: Mark event SENT
    end
```

Aggregate changes and their outbox event must be committed in the same local
database transaction.

---

# Outbox Retry Flow

```mermaid
sequenceDiagram
    participant Scheduler as Outbox Scheduler
    participant Database
    participant Kafka

    Scheduler->>Database: Claim publishable event
    Scheduler->>Kafka: Publish event
    Kafka--xScheduler: Publish failure

    Scheduler->>Database: Increment retry count
    Scheduler->>Database: Set next retry time
    Scheduler->>Database: Mark retryable failure

    loop Until published or exhausted
        Scheduler->>Database: Claim event when retry is due
        Scheduler->>Kafka: Retry publish
    end
```

Retries must be bounded and use the retry policy defined by `common-outbox`.

---

# Idempotent Consumer Flow

```mermaid
sequenceDiagram
    participant Kafka
    participant Consumer
    participant Database

    Kafka->>Consumer: Deliver event
    Consumer->>Database: Check processed event ID

    alt Event was already processed
        Consumer-->>Kafka: Acknowledge without side effects
    else Event is new
        Consumer->>Database: Apply domain changes
        Consumer->>Database: Record processed event ID
        Database-->>Consumer: Commit transaction
        Consumer-->>Kafka: Acknowledge
    end
```

The domain changes and processed-event record must belong to the same local
transaction.

---

# Inventory Distributed Lock Flow

```mermaid
sequenceDiagram
    participant Consumer as Inventory Consumer
    participant Redis
    participant InventoryDB as Inventory Database

    Consumer->>Consumer: Normalize and sort seat identifiers
    Consumer->>Redis: Acquire ordered seat locks
    Redis-->>Consumer: Locks acquired

    Consumer->>InventoryDB: Read current persisted states
    Consumer->>InventoryDB: Execute conditional state update

    alt Conditional update succeeds
        InventoryDB-->>Consumer: Commit
    else State changed or update failed
        InventoryDB-->>Consumer: Roll back
    end

    Consumer->>Redis: Release seat locks
```

Redis provides coordination only. Database conditional updates or database
locking provide the final consistency guarantee against double booking.

Booking Service must never acquire Inventory seat locks.

---

# ShowSeat State Transitions

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE
    AVAILABLE --> HELD: Reservation accepted
    HELD --> AVAILABLE: Released or expired
    HELD --> BOOKED: Booking confirmed
    AVAILABLE --> UNAVAILABLE: Operational block
    HELD --> UNAVAILABLE: Administrative intervention
    UNAVAILABLE --> AVAILABLE: Re-enabled
```

The normal booking path is:

```text
AVAILABLE → HELD → BOOKED
```

The normal release path is:

```text
HELD → AVAILABLE
```

Unsupported transitions, including `BOOKED → HELD`, must be rejected.
