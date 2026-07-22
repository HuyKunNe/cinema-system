# Coding Conventions

Version R14

---

# General Principles

SOLID

DRY

KISS

Clean Code

Prefer Composition

---

# Java

Java 21

Use Record where appropriate.

Avoid unnecessary inheritance.

---

# Package Naming

com.cinema.common.xxx

com.cinema.booking.xxx

com.cinema.payment.xxx

---

# Class Naming

Controller

Service

Repository

Mapper

Entity

Configuration

Validator

---

# DTO Naming

Request

Response

Event

Message

Projection

---

# Exception

Every business exception extends

BusinessException

Never throw RuntimeException directly.

---

# Response

Always return

ApiResponse

Never expose Entity directly.

---

# Validation

Bean Validation

Reusable validators only inside common-validation.

Business validation stays inside service.

---

# Mapping

MapStruct only.

Do NOT use

BeanUtils.copyProperties()

Manual reflection mapping

---

# Logging

Common modules

No Lombok

Logger

private static final Logger LOGGER =
        LoggerFactory.getLogger(CurrentClass.class);

Use parameterized logging.

Never use System.out.println().

---

# Lombok Policy

Common Layer

Not Allowed

Business Services

Allowed if project decides later.

---

# JSON

Use common-jackson configuration.

Never instantiate ObjectMapper directly.

---

# Kafka

Never publish directly from business transaction.

Use Transactional Outbox.

---

# Persistence

Entity

must not know Kafka.

Repositories only handle persistence.

---

# Transactions

Only Service Layer owns @Transactional.

Never put @Transactional on Repository.

---

# Testing

Every common module

must contain unit tests.

Integration tests

use Testcontainers.

---

# Naming

Method

verb + noun

Variable

camelCase

Constant

UPPER_SNAKE_CASE

Package

lowercase

---

# Git

One feature

One commit

Meaningful commit message.

---

# Documentation

Every architecture decision

must be recorded in ADR.

ROADMAP.md

must be updated after every completed round.

CHANGELOG.md

must be updated after every release.

AI_CONTEXT.md

must always reflect the current project status.
