# Dependency Rules

Version: R14

---

# Purpose

This document defines all allowed dependencies inside the project.

Violation of these rules is considered an architectural violation.

---

# Dependency Direction

Client

↓

Gateway

↓

Business Service

↓

Common Module

---

# Module Dependency

Allowed

services
    ↓
common

Forbidden

common
    ↓
services

---

# Service Dependency

Booking Service

must NEVER depend on

Payment Service

Inventory Service

Movie Service

User Service

Notification Service

Communication must happen through:

Kafka

REST (Gateway only)

Never through direct service dependency.

---

# Database Dependency

Allowed

Service

↓

Own Database

Forbidden

Booking Service

↓

Payment Database

Inventory Database

Movie Database

Each service owns its own schema.

---

# Event Dependency

Business Event

↓

Kafka

↓

Business Service

Business events never belong inside common modules.

---

# Common Module Dependency

Allowed

common-api
    ↓
common-response

common-validation

common-exception

Forbidden

common-core
    ↓
common-api

common-jpa
    ↓
booking-service

---

# Layer Dependency

Controller

↓

Service

↓

Repository

↓

Database

Never

Controller

↓

Repository

---

# DTO Dependency

Entity

↓

Mapper

↓

DTO

Never expose Entity directly to API.

---

# Transaction Rule

@Transactional

Service Layer only.

Never on Controller.

Never on Repository.

---

# Logging

Every layer uses

SLF4J

No System.out.println()

---

# Architecture Review

Any new module requires:

Architecture review

ADR

Documentation update

Roadmap update
