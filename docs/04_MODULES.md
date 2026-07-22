# Modules

Version R14

---

# Root Structure

common/

infrastructure/

services/

docs/

---

# Common Modules

## common-core

Utilities

Constants

Time

UUID

Base classes

---

## common-jpa

BaseEntity

Auditing

JPA Support

---

## common-exception

BusinessException

ErrorCode

Exceptions

---

## common-response

ApiResponse

Pagination

Response Model

---

## common-api

GlobalExceptionHandler

Response Factory

Validation Mapping

---

## common-validation

Reusable Bean Validation

Validators

---

## common-jackson

Jackson Configuration

Json Utilities

---

## common-logging

Logging Aspect

Correlation ID

Logging Utilities

---

## common-mapper

MapStruct Configuration

Base Mapper

---

## common-security

JWT

Authentication

Authorization

Security Utilities

---

## common-lock

Distributed Lock

Redisson

Lock Service

---

## common-kafka

Kafka Producer

Kafka Consumer

Kafka Configuration

Event Envelope

---

## common-outbox

Outbox Entity

Outbox Service

Publisher

Scheduler

Retry

---

## common-search

Elasticsearch

Search Utilities

(R15)

---

## common-storage

Storage Abstraction

MinIO

S3

(R16)

---

## common-tracing

Tracing

Correlation

OpenTelemetry

(R17)

---

## common-openapi

Swagger

OpenAPI Configuration

(R18)

---

## common-test

Testing Utilities

Containers

Base Test

(R19)

---

# Infrastructure

Config Server

Discovery Server

Gateway Service

---

# Business Services

Booking Service

Inventory Service

Movie Service

Payment Service

Notification Service

User Service

---

# Dependency Rule

Business Services

↓

Common Modules

Business Services

must NEVER depend on another business service.

Communication must happen through:

Kafka

or

Gateway

Never through shared database.
