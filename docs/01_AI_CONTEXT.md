# AI Context

This file exists for AI assistants.

Purpose:

Allow any AI to continue the project without previous chat history.

---

# Current Progress

Completed:

R1-R14

Current Target:

R15 common-search

---

# Locked Architecture

Do NOT modify:

Module structure

Technology stack

Package naming

Architecture

unless explicitly requested.

---

# Technology

Java 21

Spring Boot 3.5.4

MySQL 8

Kafka

Redis

Flyway

MapStruct

Jackson

OpenAPI

JUnit 5

Testcontainers

Docker Compose

---

# Architecture Decisions

UUID v7

Saga Choreography

Transactional Outbox

Event Driven

Distributed Lock

ApiResponse Standard

BusinessException Base

No Lombok in Common Layer

MapStruct Only

Jackson ISO-8601

---

# Current Event Flow

Booking Service

↓

Transaction

↓

Booking Table

+

Outbox Table

↓

Outbox Scheduler

↓

Kafka

↓

Payment

↓

Inventory

---

# Common Layer Status

common-core

DONE

common-jpa

DONE

common-exception

DONE

common-response

DONE

common-api

DONE

common-validation

DONE

common-jackson

DONE

common-logging

DONE

common-mapper

DONE

common-security

DONE

common-lock

DONE

common-kafka

DONE

common-outbox

DONE

---

# Coding Convention

No Lombok in common modules.

Logger:

private static final Logger LOGGER

MapStruct only.

BusinessException base.

ApiResponse only.

No direct ObjectMapper creation.

---

# Next Step

Implement common-search.
