# ADR-013 Spring Authorization Server

Status

Accepted

Date

2026-08-05

---

## Context

Cinema Booking System requires one authoritative authentication and token-issuance boundary.

All protected services must validate authentication independently. API Gateway is not the sole security boundary.

The completed `common-security` module provides shared Resource Server mechanics:

- JWT principal extraction and UUID subject handling;
- role and permission authority mapping;
- issuer and audience validation;
- JWK-based signature validation;
- standard Servlet `401` and `403` responses.

The system still requires an authoritative component that owns user authentication, OAuth2 client authentication, token issuance, signing keys, refresh-token lifecycle, revocation, authorization records, and security events.

---

## Decision

User Service will integrate Spring Authorization Server.

User Service is the authoritative OAuth2 and OpenID Connect issuer for Cinema Booking System.

The authorization-server implementation belongs only to:

```text
services/user-service
```

It must not be implemented inside `common-security`, API Gateway, or another business service.

`common-security` remains responsible for shared Resource Server mechanics and must not issue user or service access tokens.

---

## Authentication Topology

```text
Client
    ↓
API Gateway
    ↓
User Service
    ↓
Spring Authorization Server
    ↓
User-owned database
```

Protected API flow:

```text
Client
    ↓ access token
API Gateway
    ↓ validated access token
Business Service
    ↓ validates token again
Service-owned authorization
```

Service-to-service flow:

```text
Service client
    ↓ client credentials
User Service Authorization Server
    ↓ short-lived service access token
Target business service
```

Both API Gateway and each protected business service must validate the token independently. Passing through API Gateway does not automatically make a request trusted.

---

## Authoritative Issuer

Each environment must expose exactly one canonical issuer.

Example development issuer:

```text
http://localhost:8082
```

Example production issuer:

```text
https://identity.cinema.example
```

The exact issuer must be supplied through environment configuration:

```text
CINEMA_AUTH_ISSUER
```

The issuer must not be hard-coded in Java source and must exactly match the `iss` claim placed in access tokens.

Resource Servers must reject tokens with a missing issuer, incorrect issuer, or issuer belonging to another environment.

---

## Authorization Server Endpoints

Spring Authorization Server will expose standard OAuth2 and OpenID Connect endpoints, including authorization, token, revocation, introspection where required, JWK Set, provider configuration, and user-info where required.

Endpoint paths must use Spring Authorization Server conventions unless an explicit requirement justifies a change. OAuth2 protocol endpoints must not be replaced with custom login-token protocols.

---

## OAuth2 Grant Types

Approved grant types:

```text
Authorization Code with PKCE
Refresh Token
Client Credentials
```

Rules:

- Authorization Code with PKCE is required for public browser, mobile, and native clients.
- Client Credentials is used only for approved service identities.
- Refresh Token is allowed only for approved client types.
- Public clients must not depend on a client secret.
- Confidential clients must authenticate using approved credentials.
- Client credentials must not represent an end user.

The Resource Owner Password Credentials grant is prohibited. Raw user credentials must not be exchanged directly by arbitrary clients for access tokens.

---

## OpenID Connect

OpenID Connect will be enabled for user identity use cases.

OIDC identity tokens must be used only by approved OIDC clients. An ID token is not an API access token. Business services must accept only access tokens intended for their Resource Server audience.

---

## Access Token Model

Access tokens will be signed JWTs.

The initial access-token lifetime is:

```text
15 minutes
```

The exact lifetime remains environment configuration but must stay short-lived.

Required claims:

```text
iss
sub
aud
iat
nbf
exp
jti
```

Approved application claims:

```text
username
roles
permissions
```

Example:

```json
{
  "iss": "https://identity.cinema.example",
  "sub": "019c1234-1111-7abc-8def-0123456789ab",
  "aud": ["cinema-api"],
  "iat": 1785891600,
  "nbf": 1785891600,
  "exp": 1785892500,
  "jti": "019c1234-2222-7abc-8def-0123456789ab",
  "username": "user@example.com",
  "roles": ["USER"],
  "permissions": ["booking:create", "booking:read"]
}
```

The `sub` claim contains the UUID v7 identifier of the authenticated user or approved service principal.

Tokens must not contain passwords, password hashes, refresh tokens, MFA secrets, unnecessary private data, provider credentials, or database credentials.

---

## Access Token Audience

The initial protected API audience is:

```text
cinema-api
```

The audience must be supplied through environment configuration:

```text
CINEMA_AUTH_AUDIENCE
```

API Gateway and business services must reject tokens that do not contain the required audience. A token intended only for a different API must not be accepted.

Future audience separation requires an accepted architecture update and corresponding Resource Server configuration.

---

## Roles and Permissions

Application roles remain coarse-grained:

```text
USER
STAFF
ADMIN
SERVICE
```

JWT role claims use values without the Spring prefix. `common-security` converts them to Spring authorities with the `ROLE_` prefix.

Fine-grained permissions remain unchanged, for example:

```text
booking:create
booking:read
booking:cancel
movie:manage
inventory:manage
user:manage
```

User Service owns assignments between users, roles, and permissions.

Each business service remains responsible for endpoint authorization, resource ownership checks, business-state authorization, and service-owned permissions. A role claim alone does not override resource ownership.

---

## Service Identity

Approved internal services use the Client Credentials grant.

Service access tokens must be short-lived, audience-restricted, scope-restricted, issued only to registered confidential clients, and independently validated by the target service.

Approved service principals may receive `ROLE_SERVICE`. Service accounts must not reuse human administrator accounts.

Service client secrets or private keys must be delivered through environment or secret management and must never be committed to Git.

---

## Signing Keys

User Service owns access-token signing keys.

The initial signing algorithm is:

```text
RS256
```

Minimum RSA key size:

```text
3072 bits
```

Private signing keys must remain accessible only to the Authorization Server, never be committed or logged, be supplied through approved secret management, and support controlled rotation.

Public keys are exposed through the Authorization Server JWK Set endpoint.

Resource Servers validate access tokens through the configured issuer URI, JWK Set URI, and required audience. Resource Servers must not share the Authorization Server private key.

---

## Signing-Key Rotation

Signing keys must use a stable `kid`.

Key rotation must:

1. Create a new signing key.
2. Publish its public key through the JWK Set.
3. Begin signing new tokens with the new key.
4. Retain the previous public key until all tokens signed by it can no longer be valid.
5. Remove the old public key only after the overlap period.
6. Record the privileged rotation operation.

Emergency rotation must support revoking trust in a compromised key. The exact automated rotation schedule is deployment configuration.

---

## Refresh Token Model

Refresh tokens will be opaque security-sensitive credentials.

The initial maximum refresh-token lifetime is:

```text
30 days
```

Refresh tokens must be transmitted only over TLS, never appear in URLs, logs, traces, or Kafka events, never be accepted as API access tokens, use approved at-rest protection, belong to a user, client, and authorization session, and have an explicit expiration time.

Refresh tokens must be revoked on logout, password reset, or account compromise where applicable.

Refresh tokens rotate after every successful refresh:

```java
TokenSettings.builder()
        .reuseRefreshTokens(false)
```

Use of an invalidated refresh token must be treated as a possible reuse event. Detected reuse must revoke the affected authorization session and produce an auditable security event.

---

## Browser Token Storage

Browser clients must not store refresh tokens in local storage.

Approved browser storage should use `Secure`, `HttpOnly`, appropriately scoped cookies when supported by the selected client architecture. Access-token storage must minimize exposure to script injection. CSRF protection must be evaluated according to the final browser and cookie flow.

---

## Client Registration

OAuth2 clients must be registered explicitly. Initial implementation uses controlled server-side registration. Dynamic client registration is not enabled.

Registered clients must define client identifier, authentication method, approved grant types, redirect URIs, post-logout redirect URIs where applicable, scopes, token lifetime, refresh-token eligibility, and PKCE requirements.

Client secrets must be encoded using the approved password encoder. Plain-text client secrets must not be stored.

---

## Authorization Persistence

User Service owns Authorization Server persistence.

Persistent data includes registered clients, OAuth2 authorizations, authorization consents, refresh authorization state, revocation state, and security audit records where applicable.

Authorization Server tables belong only to the User Service database. No other service may query or update them, reuse User Service repositories, or create cross-database foreign keys to them.

Flyway owns the User Service schema.

---

## Account Status Enforcement

Account states include:

```text
ACTIVE
LOCKED
DISABLED
PENDING_VERIFICATION
```

Only active and otherwise eligible accounts may receive new valid user sessions.

- Locked accounts cannot authenticate.
- Disabled accounts cannot receive new tokens.
- Pending-verification accounts follow the approved verification policy.
- Password reset may revoke active authorization sessions.
- Account compromise must support session revocation.
- Account-status changes require audit coverage.

---

## Privileged Accounts and MFA

Privileged accounts include administrators, operational accounts, and security-management accounts.

Production privileged access requires MFA.

If MFA is not completed in R25, production privileged login must remain disabled or be protected by an approved external identity control that enforces MFA. The absence of MFA must not be silently treated as production-ready administrative security.

---

## Revocation

User Service owns token and authorization revocation.

Revocation must support logout, refresh-token revocation, authorization-session revocation, password-reset revocation, account-disable revocation, account-compromise revocation, and client-disable revocation.

Because access tokens are short-lived JWTs, immediate access-token revocation may require additional infrastructure.

The initial design relies on short access-token lifetime, refresh-session revocation, signing-key emergency rotation for key compromise, and account-state enforcement on new token issuance.

Any future access-token denylist requires an explicit scalability and availability decision.

---

## Audit Requirements

Security-sensitive operations must be auditable.

Audit events include successful and failed authentication, account lock or disable, password reset, refresh-token reuse detection, authorization-session revocation, role and permission assignment, OAuth2 client changes, signing-key rotation, and privileged account actions.

Audit records must not contain passwords, raw refresh tokens, private signing keys, client secrets, or MFA secrets.

---

## Common Security Boundary

`common-security` owns reusable mechanics:

```text
AuthenticationUser
CurrentUser
SecurityContextUtils
role and permission conversion
issuer validation integration
audience validation
JWK-based JwtDecoder configuration
standard Resource Server security responses
```

`common-security` must not own:

```text
User entity
User repository
Role assignment persistence
Password authentication
Password reset
Spring Authorization Server filter chain
RegisteredClientRepository
OAuth2AuthorizationService
JWK private keys
Refresh-token persistence
Login or registration controllers
```

Those responsibilities belong to User Service.

---

## User Service Boundary

User Service owns users, profiles, credentials, roles, permissions, account states, verification state, password-recovery state, OAuth2 clients, OAuth2 authorizations, OAuth2 consents, refresh authorization state, signing-key access, and security audit records.

User Service must not expose credential entities directly through APIs. Other services receive identity and authorization information only through validated tokens or approved User Service APIs.

---

## Testing Requirements

R25 must verify at minimum:

```text
valid issuer accepted
invalid issuer rejected
valid audience accepted
invalid audience rejected
valid signature accepted
invalid signature rejected
expired token rejected
not-before token rejected
missing subject rejected
UUID subject mapped correctly
roles mapped correctly
permissions mapped correctly
authorization code with PKCE succeeds
public client without PKCE is rejected
client credentials succeeds for approved service client
unapproved client is rejected
refresh token rotates
old refresh token reuse is rejected
logout revokes the refresh authorization
locked user cannot authenticate
disabled user cannot authenticate
401 response follows ApiResponse
403 response follows ApiResponse
```

Tests requiring database semantics must use MySQL Testcontainers. Protocol integration tests must not depend on production keys or production credentials.

---

## Consequences

### Positive

- One authoritative issuer exists.
- OAuth2 and OpenID Connect behavior follows standards.
- Business services do not sign unrelated tokens.
- Signing-key ownership is explicit.
- Resource Servers validate tokens consistently.
- User and service authentication are separated.
- Refresh-token lifecycle has explicit ownership.
- Migration to an external identity provider remains possible because Resource Servers depend on issuer, JWK, and audience contracts.

### Negative

- User Service becomes security-critical.
- Authorization Server persistence and key management require additional operational controls.
- Refresh-token rotation and reuse detection increase implementation complexity.
- Key rotation requires an overlap period.
- Privileged production access remains blocked until MFA or an approved external control exists.
- Authorization Server availability affects new login and token-refresh operations.

---

## Rejected Alternatives

### Shared HMAC Secret

Rejected because every Resource Server receiving the shared secret could sign tokens.

### Token Issuance in common-security

Rejected because a common library must not become an authoritative identity service.

### Token Issuance in API Gateway

Rejected because Gateway is a routing and edge-policy boundary, not the owner of users, credentials, or authorization sessions.

### Independent Token Issuers per Service

Rejected because issuer, key, and claim trust would become inconsistent.

### Custom Username/Password Token Endpoint

Rejected because it replaces standard OAuth2 behavior with a project-specific protocol.

### Resource Owner Password Credentials Grant

Rejected because it requires clients to handle raw user passwords and is not part of the approved OAuth2 model.

### Long-Lived Access Tokens

Rejected because compromise impact would be excessive and revocation would be difficult.

---

## Implementation Boundary

This ADR accepts the architecture decision only. It does not mark User Service schema, registration, password authentication, Authorization Server configuration, clients, RSA keys, JWT customization, refresh-token rotation, revocation, MFA, or auditing as implemented.

Those capabilities remain part of subsequent R25 implementation checkpoints.

---

## References

- `docs/08_SECURITY.md`
- `docs/10_ROADMAP.md`
- `docs/12_DEPENDENCY_RULES.md`
- `common/common-security`
- `services/user-service`
