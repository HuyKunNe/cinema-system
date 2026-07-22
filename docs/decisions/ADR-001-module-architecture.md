# ADR-001 Module Architecture

Status

Accepted

---

## Context

The project requires reusable infrastructure while keeping business services independent.

---

## Decision

The project is divided into three groups:

common/

infrastructure/

services/

The module list is locked.

No additional common modules should be introduced without architectural review.

---

## Consequences

Shared code lives in common.

Infrastructure lives in infrastructure.

Business logic lives only in services.

Dependencies flow only from services → common.

Business services never depend on another business service.
