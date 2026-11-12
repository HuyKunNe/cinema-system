# Runtime contract review — 2026-11-11

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
