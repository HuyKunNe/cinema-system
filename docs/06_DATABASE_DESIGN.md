# Database Design

Version: R14

---

# Design Principles

The project follows:

- Database per Service
- No Shared Database
- Event Driven Synchronization
- ACID inside one service
- Eventually Consistent between services

Every service owns its own schema.

Other services must never query its database directly.

Communication happens through Kafka events.

---

# Booking Service Database

Tables

bookings

booking_seats

outbox_events

processed_events

## bookings

Purpose

Store booking information.

Primary Key

UUID v7

Main Fields

id

user_id

showtime_id

status

total_amount

created_at

updated_at

---

## booking_seats

Purpose

Store reserved seats.

Fields

id

booking_id

show_seat_id

seat_number

price

---

## outbox_events

Purpose

Transactional Outbox.

Fields

id

aggregate_type

aggregate_id

event_type

payload

status

retry_count

next_retry_at

created_at

updated_at

---

## processed_events

Purpose

Idempotent Consumer.

Fields

event_id

consumer_name

processed_at

Unique Key

(event_id, consumer_name)

---

# Movie Service Database

Tables

movies

genres

movie_genres

showtimes

theaters

screens

screen_seats

---

# Inventory Service Database

Tables

show_seats

inventory_logs

processed_events

outbox_events

---

# Payment Service Database

Tables

payments

payment_transactions

processed_events

outbox_events

---

# Notification Service Database

Tables

notifications

notification_logs

processed_events

---

# User Service Database

Tables

users

roles

permissions

refresh_tokens

---

# Database Ownership

Booking Service

owns

bookings

booking_seats

Movie Service

owns

movies

showtimes

Inventory Service

owns

show_seats

Payment Service

owns

payments

No service may access another service database.

---

# Transaction Rule

One transaction

↓

One database

Distributed transaction is NOT allowed.

Cross-service consistency is achieved using Saga.

---

# Flyway Policy

Every schema change

↓

New migration

Never modify an executed migration.

---

# UUID Strategy

All primary keys

UUID Version 7

Applied to

Entity IDs

Event IDs

Outbox IDs

---

# Audit Fields

Every aggregate should contain

created_at

updated_at

Optionally

created_by

updated_by

when auditing is enabled.

---

# Index Strategy

Create indexes on

Foreign Keys

Business Keys

Status Columns

Search Columns

Kafka Retry Columns

Outbox Status

Processed Event IDs

---

# Soft Delete

Current policy

NOT enabled.

Business services should use status fields when necessary.

---

# Naming Convention

Table

snake_case

Column

snake_case

Constraint

fk_xxx

uk_xxx

idx_xxx

---

# Future Scaling

MySQL Read Replica

Partitioning

Archive Tables

Read Models

CQRS Projection
