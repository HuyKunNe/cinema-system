# Runtime contract review — 2026-11-11

## 2026-11-20 — User credential request metadata

Reviewed the staff provisioning and password/email-verification request schemas.
The running User spec describes the four password fields as plain strings
without password format or writeOnly. Email verification token is also missing
writeOnly. All five fields are already required by existing validation.
The staff list query correctly reports optional cinemaId, page >= 0 and size
1–100; no pagination contract change was needed. The currently observed Booking
spec still does not expose the conditional print-simulations controller; its
header/schema behavior was not verified by enabling it or writing data.

Added UserCredentialsOpenApiConfiguration using the established service-local
OpenApiCustomizer pattern. Source now sets format=password and writeOnly=true
on CreateStaffAccountRequest.password, ConfirmEmailVerificationRequest.password,
and ChangeCurrentUserPasswordRequest.currentPassword/newPassword. Verification
token receives writeOnly=true without changing its format/length constraints.
No Swagger annotations, Jackson serialization-access changes, example secrets,
new dependency or authentication/validation rules were introduced.

ConfirmEmailVerificationRequest now redacts its entire toString, including
token/password, matching the existing staff creation/password-change DTOs.
ChangeCurrentUserPasswordRequest already had a redacted toString and was left
unchanged. This is preventive string rendering hardening, not evidence that a
credential was observed leaking from a running endpoint.

Offline `-pl services/user-service -am -DskipTests compile` completed BUILD
SUCCESS for 15 modules. A GET after compilation still showed the old User
schema: the metadata correction remains DONE IN SOURCE / pending service
reload and schema verification. No service restart, API write, migration, FE
edit, client generation or test execution was performed. Existing working-tree
edits, including UserServiceApplication, were preserved. Verify all five fields
after the normal User Service restart before generating clients or closing this
part of P0.3. Broader authentication/concurrency verification remains open.

## 2026-11-20 — P0.3 pickup nullability continuation

Read-only GETs against the current local service OpenAPI endpoints observed:

| Service / port | Operations | Missing / duplicate operationId within this spec |
| --- | ---: | --- |
| Movie / 8081 | 19 | 0 / 0 |
| User / 8082 | 14 | 0 / 0 |
| Inventory / 8083 | 69 | 0 / 0 |
| Booking / 8084 | 13 | 0 / 0 |
| Payment / 8085 | 6 | 0 / 0 |

Inventory initially timed out with a four-second limit; a subsequent request
with a longer timeout succeeded. CreateShowtimeBatchRequest.showtimes now has
minItems=1, maxItems=50. This updates the historical minItems=0 observation
below; it does not prove batch transaction/concurrency behavior.

User exposes the four staff management operations and me/staff-context.
Booking exposes getBookingPickupPass and lookupBookingPickup. The observed
Booking spec does not expose print-simulations, consistent with its conditional
controller; no runtime flag value was read or changed. Existing process info
still has no build/git metadata, so exact runtime revision is not certified.

Before correction, Booking OpenAPI 3.1.0 described the following fields as
non-null and omitted model required lists even though their response records
use JsonInclude.ALWAYS and legitimately return null:

| Schema | Field |
| --- | --- |
| BookingPickupPassResponse | expiresAt, qrPayload |
| BookingPickupLookupResponse | totalGuests |
| Ticket (referenced by pickup lookup) | checkedInAt |
| Printing (referenced by pickup lookup) | simulationBlockReason |

Added BookingPickupOpenApiConfiguration using the existing Inventory
SeatMapOpenApiConfiguration/RoomLayoutMatrixOpenApiConfiguration pattern.
The customizer marks properties of these ALWAYS-included response records
required and makes only the five nullable fields accept null. Nested schemas
are resolved from pickup lookup references rather than guessed global names.
OpenAPI 3.1 uses anyOf with an explicit null branch; 3.0 uses nullable=true.
No Swagger annotations were added to controllers/DTOs, and no runtime JSON,
business logic, security rule, dependency, database or FE code changed.

After compilation, a fresh GET of the running Booking OpenAPI observed all
five fields required with a null branch. Pickup-pass has eight required
properties. No process restart was explicitly performed. This is runtime
schema evidence, not authenticated workflow or cross-service race evidence.

Verification: offline Maven at root with
`-pl services/booking-service -am -DskipTests compile` completed BUILD SUCCESS
for 17 modules. git diff --check reported no whitespace errors. No tests,
service/API writes, migration commands or FE client generation were executed.
Pre-existing working-tree edits were preserved. P0.2 and the broader P0.3
integration/authentication/client verification gates remain open.

The original 2026-11-11 evidence below remains historical.

## Scope and provenance

Reviewed checkout `15fd453` with a clean starting working tree. The owner now
permits database writes through service/API; direct SQL/database writes and test
execution remain outside this review. This increment made read-only HTTP calls
and changed the existing batch request validation metadata; no data was written.

Local listeners were present on Gateway 8080, Movie 8081, User 8082, Inventory
8083, Booking 8084, Payment 8085 and FE 5173. The OpenAPI documents below were
read directly from each service's `/v3/api-docs`, without authentication.
Inventory/Booking/Payment `/actuator/info` returned 200 but no build/git metadata.
Consequently their exact running source revision cannot be certified from these
responses. Processes were not restarted and no launch credentials were read.

## Observed OpenAPI

| Service / port   | OpenAPI | Operations | Missing / duplicate operationId within this spec |
| ---------------- | ------- | ---------- | ------------------------------------------------ |
| Movie / 8081     | 3.0.1   | 19         | 0 / 0                                            |
| User / 8082      | 3.1.0   | 7          | 0 / 0                                            |
| Inventory / 8083 | 3.1.0   | 69         | 0 / 0                                            |
| Booking / 8084   | 3.1.0   | 6          | 0 / 0                                            |
| Payment / 8085   | 3.1.0   | 5          | 0 / 0                                            |

Counted GET/POST/PUT/PATCH/DELETE operations. These are separate service specs;
this is not a claim about a merged multi-service document or FE client parity.
Each reported info.version was the generic `1.0.0`, not a source commit.

Inventory exposes `getManagedShowtimes` and `createShowtimeBatch`, and the inline
status enums of RoomLayoutResponse and CinemaPricingPolicyVersionResponse both
contain DRAFT/PUBLISHED/RETIRED. This proves enum presence in the observed schema,
not that lifecycle publication/retirement works against MySQL.

## Batch minimum mismatch and source correction

Runtime CreateShowtimeBatchRequest.showtimes had minItems=0, maxItems=50. Source
used @NotEmpty plus @Size(max=50), while ShowtimeCreationService explicitly rejects
an empty batch. This advertised an empty list despite the existing business rule.

Changed the existing @Size to `min=1, max=MAX_SHOWTIMES`. Retained @NotEmpty,
nested @Valid/@NotNull, UUID requirement, service guard, endpoint and DTO shape.
No Swagger annotations, duplicate models, FE changes or migrations were added.
The expected published schema is minItems=1, maxItems=50 after service reload.

Inventory and reactor production sources compiled successfully with:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/inventory-service -am -DskipTests compile
```

The follow-up HTTP observation at `2026-11-11T07:45:17Z` still returned minItems=0.
Compilation did not reload the running process. Restart/reload Inventory through
the existing launch configuration, then re-read the same schema before claiming
runtime correction. No automatic restart or environment/secret changes made.

## Observed Gateway HTTP reads

| Request without Authorization                                                      | Observed status           |
| ---------------------------------------------------------------------------------- | ------------------------- |
| GET /api/v1/cinemas                                                                | 200; three cinema records |
| GET /api/v1/showtimes/management?from=2026-11-11T00:00:00Z&to=2026-11-12T00:00:00Z | 401                       |
| GET /api/v1/bookings                                                               | 401                       |

This does not verify authorized admin/customer behavior or 403 permission checks.
No create/publish/hold/booking/payment mutation was sent. Existing historical
Bearer tokens were not reused. The Computer Use native helper was unavailable
after its documented retry/reset recovery, so an authenticated FE session could
not be inspected or operated. No password, connection string or secret requested.

## Remaining verification

- Reload Inventory and confirm the emitted batch bound is 1..50, then review FE
  generation with source/build provenance; do not hand-edit generated clients.
- Once an authenticated API/session is available, verify batch rejection and
  lifecycle writes through services, with recorded resource IDs and outcomes.
- Keep P0.2 concurrency/rollback and other runtime gates open. Reading OpenAPI,
  compilation and anonymous GET responses do not close them.

`git diff --check` passed. No tests were executed, applications started, provider
calls made, database/migration commands run or FE files modified.
