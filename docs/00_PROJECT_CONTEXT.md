# Cinema Booking System

Version: 0.1 (R14)

---

# Project Overview

Cinema Booking System là hệ thống đặt vé xem phim theo kiến trúc Microservices hướng Enterprise.

Mục tiêu:

- High Availability
- High Scalability
- Event-Driven
- Cloud Ready
- Production Ready
- DDD Friendly
- Easy Horizontal Scaling

Hệ thống được thiết kế để phục vụ các chuỗi rạp như:

- CGV
- Galaxy Cinema
- Cinestar
- Lotte Cinema

---

# Current Progress

Completed

✅ R1 Parent Project

✅ R2 common-core

✅ R3 common-jpa

✅ R4 common-exception

✅ R5 common-response

✅ R6 common-api

✅ R7 common-validation

✅ R8 common-jackson

✅ R9 common-logging

✅ R10 common-mapper

✅ R11 common-security

✅ R12 common-lock

✅ R13 common-kafka

✅ R14 common-outbox

---

Current Target

R15 common-search

---

# Project Goals

The project focuses on:

- Clean Architecture
- Domain Driven Design
- Event Driven Architecture
- Saga Pattern (Choreography)
- Transactional Outbox
- Idempotent Consumer
- Distributed Lock
- Observability
- Production Ready Deployment

---

# Architecture Style

Microservices

↓

Spring Cloud

↓

Kafka Event Driven

↓

Saga Pattern

↓

MySQL

↓

Redis

↓

Docker

---

# Principles

The project follows:

- SOLID
- DRY
- KISS
- Clean Code
- Hexagonal Friendly
- CQRS Ready
- Event Driven

---

# Project Structure

common/

infrastructure/

services/

docs/

---

# Development Strategy

The project is developed incrementally.

Progress is divided into:

R1

↓

R2

↓

...

↓

R21

Each round must pass:

- Unit Test

- Maven Build

- Integration

before moving to the next round.

---

# Documentation

The docs directory is the single source of truth.

Chat history should never be treated as project documentation.
