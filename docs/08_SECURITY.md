# Security Architecture

Version: R14

---

# Overview

The system uses Spring Security with JWT-based authentication.

Authentication and authorization are centralized in the common-security module.

---

# Authentication Flow

Client

↓

Login

↓

User Service

↓

Generate JWT

↓

Return Access Token

↓

Client stores token

↓

Subsequent requests

↓

Gateway

↓

Business Service

↓

JWT Validation

↓

SecurityContext

---

# Authorization

Authorization uses Role-Based Access Control (RBAC).

Future support:

- Permission-based authorization
- Resource-level authorization

---

# JWT Claims

Standard claims

- sub
- iat
- exp
- jti

Custom claims

- userId
- username
- roles

---

# Token Lifetime

Access Token

15~30 minutes

Refresh Token

7~30 days

---

# Password

BCryptPasswordEncoder

Never store plain text passwords.

---

# Security Context

Current authenticated user can be obtained through SecurityContextUtils.

Business services should never parse JWT directly.

---

# API Security

Public endpoints

- Login
- Register
- Swagger (configurable)

Protected endpoints

Everything else.

---

# Gateway

Gateway validates JWT before forwarding requests.

Services may optionally validate again for defense in depth.

---

# Method Security

Supported annotations

@PreAuthorize

@PostAuthorize

RolesAllowed

---

# Future Enhancements

OAuth2

OpenID Connect

Key Rotation

JWKS

Rate Limiting

API Key

MFA
