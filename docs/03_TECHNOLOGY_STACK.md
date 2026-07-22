# Technology Stack

Version: R14

---

# Programming Language

Java 21

Reason

- Latest LTS
- Virtual Thread Ready
- Pattern Matching
- Record
- Performance Improvement

---

# Build Tool

Apache Maven

Architecture

Multi Module

Reason

- Dependency Management
- Modular Development
- Easy CI/CD

---

# Framework

Spring Boot

Version

3.5.4

Modules

- Spring Web
- Spring Validation
- Spring Security
- Spring Data JPA
- Spring AOP
- Spring Kafka
- Spring Cache

---

# Spring Cloud

Components

Gateway

Config Server

Discovery Server

OpenFeign (future if required)

---

# Database

MySQL 8

Reason

- Stable
- ACID
- High Performance

---

# Database Migration

Flyway

Policy

Migration only.

Never modify existing migration.

---

# Cache

Redis

Usage

Distributed Lock

Caching

Temporary Data

Session (future)

---

# Distributed Lock

Redisson

Usage

Seat Reservation

Prevent Double Booking

---

# Messaging

Apache Kafka

Pattern

Event Driven

Saga

Transactional Outbox

---

# Search

Elasticsearch

Implemented in

common-search

---

# Object Mapping

MapStruct

No BeanUtils

No Reflection Mapping

---

# JSON

Jackson

Configuration

ISO-8601

JavaTimeModule

---

# API Documentation

OpenAPI 3

Swagger UI

---

# Authentication

Spring Security

JWT

Role Based Authorization

---

# Logging

SLF4J

Logback

AOP Logging

Correlation ID

---

# Testing

JUnit 5

Mockito

Testcontainers

Spring Boot Test

---

# Build

Docker

Docker Compose

---

# Monitoring (Future)

Micrometer

Prometheus

Grafana

---

# Tracing (Future)

Micrometer Tracing

OpenTelemetry

Zipkin

---

# File Storage (Future)

MinIO

S3 Compatible

---

# UUID Strategy

UUID Version 7

Applied to

All Entities

All Events

---

# Architecture Patterns

Microservices

DDD Friendly

Event Driven

Saga Choreography

Transactional Outbox

Idempotent Consumer

Distributed Lock

API Gateway

Config Server

Service Discovery
