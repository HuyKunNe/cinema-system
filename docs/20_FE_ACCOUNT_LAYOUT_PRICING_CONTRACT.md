# Account, layout and pricing contracts — P0.3 source review

**2026-11-10 update:** Controller Swagger/OpenAPI annotations have been removed
at the owner's request. Explicit operation IDs in this reference describe the
earlier reviewed source, not guaranteed names in a new runtime export. Reconcile
generated FE names before adopting a new specification. Runtime session/CSRF,
permissions, payloads and database behavior are unchanged.

The continuation from `f2d950e` renames layout/pricing controller methods to the
previously documented operation names, matching existing generated FE functions
without annotations. This is source compatibility evidence only; runtime exports
and schema compatibility still need verification. Account handler names are
unchanged, and the session-versus-Bearer integration gap below remains open.

Reviewed against BE `main` at `4e58548` on 2026-11-06. This supplements
[the FE contract reference](19_FE_API_CONTRACT.md). Examples use synthetic IDs
and values; response examples are excerpts, not captured HTTP responses. Replace
IDs and concurrency versions with values actually returned by the owning service.
No runtime OpenAPI export, automated checks or FE modifications were performed.

## Account: authentication is an open integration gap

`AuthorizationServerSecurityConfiguration` defines an authorization-server
filter chain and an application chain using form login, sessions and the default
CSRF protection. No `oauth2ResourceServer` configuration was found for the User
application routes. `CurrentUserController` expects `CinemaUserDetails`, not a
JWT principal. The shared security module supplies JWT mechanics, but does not
install a Resource Server filter chain for these routes.

The generated FE user client uses `apiRequest`. Its shared Axios instance adds
an access token, but does not explicitly configure cross-origin session cookies
or supply a User Service CSRF token. The existence of generated functions does
not demonstrate that Bearer-only calls can reach the User controllers. A Gateway
JWT check does not establish the User Service session or its expected principal.
Cookie behavior also depends on the deployment origin; it was not tested here.

Before claiming account integration complete, settle whether the SPA account
APIs will accept Bearer tokens independently at User Service or use an explicitly
supported session/CSRF path. The choice must preserve the authorization-server
login flow, trusted actor identity and revocation behavior. This review does
not choose or implement that security change, disable CSRF, or assume a session
cookie can substitute for the Gateway's authentication requirement.

For this service, do not promise the Resource Servers' JSON 401/403 contract:
authentication entry points and default CSRF handling must be reviewed for the
actual request context. Shared domain error handling applies once a request
reaches the corresponding application handler.

| Existing operation | Response | Source boundary |
| --- | --- | --- |
| `GET /api/v1/users/me` | `200`, raw `CurrentUserProfileResponse` | Authenticated User session; ID from `CinemaUserDetails` |
| `PUT /api/v1/users/me` | `200`, raw profile | Same owner; session/CSRF requirements apply |
| `PUT /api/v1/users/me/password` | `204`, no response body | Same owner; requires current and new passwords; session/CSRF requirements apply |
| `PATCH /api/v1/users/{userId}/lock` | `204`, no body | `user:manage` in the application chain and service method security |
| `PATCH /api/v1/users/{userId}/unlock` | `204`, no body | Same privileged boundary |
| `PATCH /api/v1/users/{userId}/disable` | `204`, no body | Same privileged boundary |
| `PATCH /api/v1/users/{userId}/enable` | `204`, no body | Same privileged boundary |

`AdministrativeUserAccountServiceImpl` records successful privileged actions via
`SecurityAuditRecorder`; callers do not supply an actor in the request. These
controllers do not provide an Admin user list/search/detail API. Do not invent
one, or assume an ADMIN role alone implies `user:manage`.

The controllers declare no explicit operation IDs. Existing generated function
names such as `getCurrentProfile` and `lockUserAccount` are prior-export evidence;
runtime names and whole-spec uniqueness remain unverified.

### Profile example, after establishing supported authentication

`PUT /api/v1/users/me`:

```json
{
  "firstName": "An",
  "lastName": "Nguyen",
  "phoneNumber": null
}
```

`200` response excerpt; the GET profile operation has the same response shape:

```json
{
  "id": "019a5a00-0000-7000-8000-000000000001",
  "firstName": "An",
  "lastName": "Nguyen"
}
```

The names have a maximum length of 100; phone number has a maximum length of 32.
`UserProfileServiceImpl` trims these fields to null and replaces all three
values. Omitted fields therefore clear their previous values; this is not a
partial PATCH. The shared Jackson configuration omits null response properties
unless a DTO overrides inclusion; do not require a JSON null for an empty phone
number in this profile response. The DTO has no email, role, status or owner-ID
write fields.

An Admin account lock request is
`PATCH /api/v1/users/019a5a00-0000-7000-8000-000000000001/lock`, without a JSON
body; success is `204`. Its target ID is not the audit actor.
Password changes accept `currentPassword` and `newPassword`; values are not
included in this reference. `UserCredentialServiceImpl` enforces its password
policy, checks the current credential and revokes authorizations after a change.
Do not log request bodies or infer password policy solely from DTO annotations.

## Room layouts: matrix document and revision are already in source

All `/api/v1/room-layouts` operations require `inventory:manage` at Inventory.
Gateway requires authentication for these routes. Successful responses are raw
DTOs, not `ApiResponse` envelopes.

| Route | Explicit operation ID | Response |
| --- | --- | --- |
| `POST /api/v1/room-layouts` | `createRoomLayoutDraft` | `201`, `RoomLayoutResponse`, with Location |
| `GET /api/v1/room-layouts?roomId={roomId}` | `getRoomLayoutsByRoom` | `200`, array of layout metadata |
| `GET /api/v1/room-layouts/{layoutId}` | `getRoomLayoutById` | `200`, metadata |
| `GET /api/v1/room-layouts/{layoutId}/content` | `getRoomLayoutContent` | `200`, complete content document |
| `PUT /api/v1/room-layouts/{layoutId}/content` | `replaceRoomLayoutContent` | `200`, refreshed content |
| `POST /api/v1/room-layouts/{layoutId}/publish` | `publishRoomLayout` | `200`, metadata |
| `POST /api/v1/room-layouts/{layoutId}/drafts` | `createRoomLayoutDraftFromPublished` | `201`, copied matrix draft content |

Create an empty default 6x6 matrix with
`POST /api/v1/room-layouts` and `{"roomId":"019a5a00-0000-7000-8000-000000000004"}`.
Omitting both canvas dimensions and matrix selects this default in the DTO.
Explicit matrix creation also requires an empty seat list. Populate seats using
content replacement, after reading the created layout ID and `version`.

`PUT /api/v1/room-layouts/{layoutId}/content` example:

```json
{
  "expectedVersion": 0,
  "matrix": {
    "rowCount": 2,
    "columnCount": 5,
    "seats": [{
      "rowIndex": 0,
      "columnIndex": 2,
      "number": 1,
      "seatType": "STANDARD"
    }]
  }
}
```

`200` content response excerpt:

```json
{
  "layout": {
    "id": "019a5a00-0000-7000-8000-000000000005",
    "layoutVersion": 1,
    "version": 1,
    "status": "DRAFT"
  },
  "seats": [],
  "elements": [],
  "matrix": {
    "rowCount": 2,
    "columnCount": 5,
    "seats": [{
      "seatId": null,
      "rowIndex": 0,
      "columnIndex": 2,
      "number": 1,
      "seatType": "STANDARD"
    }]
  }
}
```

The dimensions preserve the empty second row, leading empty cells and trailing
columns. Reconstruct empties from those dimensions and the sparse seat entries;
do not shrink the matrix to occupied positions or convert it to geometry.
With `matrix`, omit legacy `seats` and `elements` from the request, including
empty lists. Keep returned non-null `seatId` values for retained bound seats.
Draft entries may have null seat IDs. Each dimension is an integer from 1 to
100; matrix input permits at most 2500 seat entries.

`RoomLayoutMatrixValidator` requires consecutive numbers 1..n in each row's
column order. It rejects invalid numbering rather than renumbering on save.
Row labels derive from row positions, so an empty row does not shift later
labels. `layoutVersion` identifies the room's layout revision; `version` is the
optimistic lock token. Content writes and publication use the latter as
`expectedVersion`; copying from published uses the source's `version`.

Publish with `{"expectedVersion":1}` using the actual latest value. Publication
has additional readiness guards; a syntactically valid draft is not proof it
can publish. A stale write returns `409 INVENTORY_ROOM_LAYOUT_VERSION_CONFLICT`.
For content replacement, publication and copying from published, this error now
includes `error.currentVersion`, the optimistic version observed during the
precondition check. It is not `layoutVersion` and is not a token to automatically
overwrite with: reload content, reconcile the user's edits, then explicitly retry.
The resource can change again after the error response. The existing FE error
normalizer currently drops this additional field; adopting it requires a separate
FE update. Existing code/message-based conflict handling remains usable.

Illustrative conflict body (timestamp and null properties omitted):

```json
{
  "success": false,
  "error": {
    "code": "INVENTORY_ROOM_LAYOUT_VERSION_CONFLICT",
    "message": "Room layout has changed; reload before saving",
    "category": "BUSINESS",
    "currentVersion": 7
  }
}
```

`currentVersion` is optional in the shared error model. Other errors keep their
existing code/category/details and have no known current version; null omission
depends on the service's Jackson configuration. This change adds no endpoint or
Swagger annotation and does not change pricing-conflict responses.

Current `SeatType`/matrix validation reserves two adjacent columns for COUPLE.
The requested P2 target is one cell per couple seat. This is an identified
source/target gap; the current FE also reserves two columns. A coordinated
product/FE decision is needed before changing this rule. The example deliberately
uses STANDARD and does not change that rule. Metadata canvas dimensions remain
compatibility values in the current entity; matrix editing does not submit them.

## Pricing configuration and fare-matrix writes

Let `V` denote `/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/versions/{versionId}`
in the table below. These routes require `inventory:manage` at both Gateway and
Inventory. Successful bodies are raw DTOs.

| Route | Explicit operation ID | Response |
| --- | --- | --- |
| `PUT V/configuration` | `replacePricingPolicyConfiguration` | `200`, refreshed `FareMatrixResponse` |
| `GET V/fare-matrix` | `getPricingFareMatrix` | `200`, matrix with version-owned definitions |
| `PUT V/fare-matrix` | `replacePricingFareMatrix` | `200`, refreshed matrix |
| `GET V/fare-matrix/validation` | `validatePricingPolicyFareMatrix` | `200`, validation result |
| `POST V/publish` | `publishPricingPolicyVersion` | `200`, published version metadata |

`expectedVersion` is `FareMatrixResponse.version` or the corresponding pricing
version's optimistic version, not its `revision`. Writes require DRAFT and
matching ownership/applicability. Wrong versions produce
`409 INVENTORY_PRICING_POLICY_VERSION_CONFLICT`; non-draft edits produce
`409 INVENTORY_PRICING_POLICY_NOT_DRAFT`. These errors do not return a replacement
version field. Reload, reconcile and explicitly retry with the returned version.

For a draft whose complete intended definitions are one full-day band and one
default group, `PUT V/configuration`:

```json
{
  "expectedVersion": 0,
  "timeBands": [{
    "id": "019a5a00-0000-7000-8000-000000000010",
    "name": "All day",
    "startMinute": 0,
    "endMinute": 1440,
    "sortOrder": 0
  }],
  "ticketGroups": [{
    "id": "019a5a00-0000-7000-8000-000000000008",
    "name": "Regular",
    "eligibilityDescription": null,
    "defaultGroup": true,
    "sortOrder": 0
  }]
}
```

IDs above must be retained definitions from this version. Omit an ID only when
creating a new definition; the server generates it. Configuration replacement
submits the complete intended lists. Removing a definition removes its draft
fare cells in the same transaction; keeping IDs preserves attached fares while
renaming/reordering. Newly introduced combinations remain UNCONFIGURED.
Exactly one unconditional default group is required, and overlapping bands are
rejected. Names and eligibility descriptions are display data, not eligibility
checks. Unknown legacy properties are rejected.

Use the response's actual version for the next `PUT V/fare-matrix`:

```json
{
  "expectedVersion": 1,
  "scopes": [{"experienceType":"STANDARD","contentFormat":"2D"}],
  "seatTypes": ["COUPLE"],
  "cells": [{
    "experienceType": "STANDARD",
    "contentFormat": "2D",
    "timeBandId": "019a5a00-0000-7000-8000-000000000010",
    "ticketGroupId": "019a5a00-0000-7000-8000-000000000008",
    "seatType": "COUPLE",
    "state": "PRICED",
    "amount": 180000
  }]
}
```

`200` response excerpt:

```json
{
  "status": "DRAFT",
  "version": 2,
  "coverageComplete": true,
  "cells": [{
    "experienceType": "STANDARD",
    "contentFormat": "2D",
    "timeBandId": "019a5a00-0000-7000-8000-000000000010",
    "ticketGroupId": "019a5a00-0000-7000-8000-000000000008",
    "seatType": "COUPLE",
    "state": "PRICED",
    "amount": 180000,
    "unit": "PAIR"
  }]
}
```

This assumes exactly the one band/group/scope/seat type shown. A full matrix
replacement also replaces its scopes and seat-type selection; it is not a cell
PATCH. Missing combinations are rendered UNCONFIGURED. Zero is a configured
PRICED amount, distinct from UNCONFIGURED or NOT_APPLICABLE. Publication checks
more than this excerpt's coverage flag and uses the actual returned version.
No-op writes can retain their version, so never calculate the next token locally.

`FareMatrixRules` excludes COUPLE from its premium uniform-fare rule. Each pair's
whole price can vary by group, band, format and experience. The existing rule for
PRICED non-COUPLE cells in PREMIUM_CINEMA remains source behavior; this reference
does not extend it to COUPLE or introduce any additional price equality rule.

## Persistence, evidence and remaining work

Current User V1 persists profiles/credentials; V2 seeds authorities. Inventory
V1 already contains matrix cell storage, row/column counts, pricing time bands,
ticket groups and fare-cell references. Current Inventory V2 concerns projection
snapshots. Historical design notes about later numbered migrations are not an
instruction to recreate those migrations or modify an applied baseline.

Source evidence: User controllers, `AuthorizationServerSecurityConfiguration`,
`UserProfileServiceImpl`, `UserCredentialServiceImpl`, and
`AdministrativeUserAccountServiceImpl`; Inventory controllers/DTOs listed above,
`RoomLayoutMatrixValidator`, `RoomLayoutMatrixService`,
`PricingPolicyConfigurationService`, `FareMatrixService`, `FareMatrixRules`, and
their service-owned migrations. FE generated User functions and Admin pricing
callers were inspected read only; their existence is not runtime verification.

P0.3 still needs runtime spec provenance, whole-spec operation-ID review and
actual HTTP/security verification when authorized. User account authentication
integration is an explicit gap to resolve under P5.1/P5.2; COUPLE matrix semantics
remain P2 work. P0.2 execution gates stay open and R28 remains deferred.
No API, schema, authority or migration was changed in this documentation increment.
