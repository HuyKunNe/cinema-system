# P3.2 — Paged showtime management

Source increment from clean checkout `0ec5532`. No FE files were modified and no
runtime API export, test execution or database commands were performed.

## Existing contract and added operation

The Admin FE currently calls `getShowtimesByTimeRange` and filters/sorts the full
array in `features/showtimes/admin/api/admin-showtime-queries.ts`. Existing
`GET /api/v1/showtimes`, `/by-room/{roomId}`, `/by-movie/{movieId}`, `/bookable`
and detail APIs retain their paths, authorization and response shapes.

The new operation is:

```http
GET /api/v1/showtimes/management?from=2026-11-10T00:00:00%2B07:00&to=2026-11-11T00:00:00%2B07:00&page=0&size=20&status=SCHEDULED
```

Handler name: `getManagedShowtimes`. Both Gateway and Inventory require
`showtime:manage`; an ADMIN role or inventory:manage alone is insufficient. The
management matcher precedes the broad public showtime GET matcher at both layers.
No Swagger annotations, scope definitions or OAuth/OIDC token rules are added.

| Query parameter | Contract |
| --- | --- |
| `from`, `to` | Required ISO offset date-times; `to` must be after `from` |
| `cinemaId`, `movieId`, `roomId` | Optional UUID filters, combined with AND |
| `status` | Optional ShowtimeStatus; omission includes every status |
| `page` | Zero-based, default 0, nonnegative |
| `size` | Default 20; inclusive range 1–100 |

The range matches showtime **start times**, `from <= startsAt < to`. It is not an
interval-overlap search. Clients choose the cinema's local day and convert its
boundaries to offset date-times; the server does not infer a cinema timezone from
these optional IDs. Encode `+` as `%2B` when manually constructing a URL.

Results sort by `startsAt ASC, id ASC`. IDs are the tie-breaker for equal start
times. Unknown IDs or contradictory room/cinema filters return an empty page,
not a parent-resource existence error. This management query includes historical,
cancelled and inactive-room/cinema showtimes unless filtered otherwise; it does
not apply public bookability or future-only rules.

## Response and errors

Success is `200`, `Cache-Control: no-store`, with a raw existing
`PageResponse<ShowtimeResponse>`. There is no ApiResponse success wrapper.
Each content item retains the existing full ShowtimeResponse shape, including its
stored screening/scheduling snapshots and selected pricing references.

An empty first page is:

```json
{
  "content": [],
  "page": {
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

This is an illustrative shape, not a captured response. Existing shared failure
envelopes apply: 401 for anonymous access, 403 for missing permission, and 400 for
invalid/missing parameters. A negative page or size outside 1–100 produces
`INVENTORY_INVALID_SHOWTIME_PAGINATION`. Period validation uses existing
`SHOWTIME_PERIOD_REQUIRED` / `INVALID_SHOWTIME_PERIOD` Inventory errors. Malformed
UUID, status or timestamp values fail transport conversion before the query.

## Persistence and rollout

- Reuses ShowtimeRepository, ShowtimeService/Impl, ShowtimeMapper,
  PageResponseMapper and existing response DTOs; no duplicate persistence model.
- Content and explicit count queries have identical filter predicates. Only
  to-one associations needed by the response are fetched; no collection fetch
  join is used with pagination. The service reads content/count/mapping in one
  read-only REPEATABLE_READ transaction.
- Fixed ordering does not freeze the list across separate page requests when
  showtimes are created or rescheduled. Refresh/reset pagination after mutations.
- No schema/index/migration, datasource, MySQL driver, event or service-owned data
  changes. Runtime query validation, MySQL execution plans and HTTP behavior have
  not been checked by compile-only verification.
- Deploy the Gateway matcher together with the Inventory endpoint. Each layer
  enforces the permission independently. Existing Gateway routing already covers
  showtime paths; no route change is required.
- FE integration is separate: export the updated Inventory spec, generate the
  client and add a PageResponse parser. Send all filters and pagination to the
  new operation; include them in the query key and reset page when filters change.
  Consume content/page directly. Existing array callers continue to work.

## Verification limits and optional manual review

Added unexecuted service/MVC/Gateway regression sources for filter forwarding,
page metadata, invalid pagination/time ranges, raw success shape and management
authorization before public catalog rules. None starts during compilation.

Optional development-environment HTTP review: compare totals/content across page
boundaries, equal start times and combined room/cinema/movie/status filters;
confirm an item exactly at `to` is excluded; check 401/403/400 and existing public
GET behavior. Use existing data only. These runtime checks remain pending.

Compile-only verification succeeded for Inventory, Gateway and their reactor
dependencies:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/inventory-service,infrastructure/gateway-service -am -DskipTests test-compile
```

This compiles production and test sources without running tests or applications.
Static checks confirmed identical content/count filter predicates and management
authorization ahead of public rules at both layers; `git diff --check` passed.
BUILD SUCCESS does not certify runtime JPQL, HTTP or MySQL behavior.
