# FE API contract — P0.3 source review

**2026-11-10 update:** Swagger/OpenAPI annotations have been removed from BE
controllers at the owner's request. Explicit operation IDs, response descriptions,
tags and security metadata listed below are historical source-review evidence.
Springdoc now infers controller operations without those overrides. Before
regenerating FE clients, compare the exported specification and generated names;
HTTP mappings, payloads, runtime authorization and response handling are unchanged.

The same cleanup removes model/DTO `@Schema` annotations, including nested records.
Explicit schema names, required/nullable overrides, descriptions and examples may
therefore differ in the next export. Jackson and validation annotations remain;
runtime JSON and validation rules have not been changed by this cleanup.

**Handler-name continuation from `f2d950e`:** Renamed 28 Movie/Inventory handler
methods to the operation names previously supplied by annotations. All 28 names
match existing generated FE functions. Generic methods such as `get`, `replace`,
`createDraft` and `publish` now carry business names such as `getPricingFareMatrix`,
`replacePricingPolicyConfiguration`, `createRoomLayoutDraft` and `publishRoomLayout`.
No Swagger annotations were restored. Static inspection found no duplicate public
method names across current service REST controllers; handler bodies and all other
Java bytes in the changed files are preserved. This supports Springdoc's default
method-name inference but does not certify runtime operation IDs, schema naming,
or FE regeneration. Existing response/schema metadata warnings above still apply.

Reviewed against BE `main` at `34fafae` on 2026-11-06, plus the P0.3 OpenAPI
metadata change described below. This is a source-based integration reference,
not an exported OpenAPI specification or a runtime acceptance result.

The continuation against `4e58548` covers account authentication/profile, Admin
account actions, matrix layouts and configurable pricing in the
[account/layout/pricing supplement](20_FE_ACCOUNT_LAYOUT_PRICING_CONTRACT.md).
It records an unresolved User Service session-versus-Bearer integration gap;
generated User functions alone are not proof of usable SPA authentication.

## Evidence and limits

- BE controllers, DTOs, services, enums, security configurations, response/error
  factories and current Flyway migrations define the behavior described here.
- The local `cinematic-web` checkout was read only. Reviewed its OpenAPI
  metadata, movie/booking generated clients, Axios mutator and Admin showtime
  create caller. No FE files were changed or regenerated.
- FE `openapi/movie-service.metadata.md` still contains placeholders for the
  source commit, working-tree state and export time. Its JSON is useful for
  comparison but cannot certify the current runtime revision.
- FE Inventory metadata declares `22dfdc130bec4939ed6613ab01ed1b37f0e616da` and
  an export on 2026-10-21. It explicitly does not verify build provenance.
- FE `orval.config.ts` reads the five services' local `/v3/api-docs` endpoints,
  rather than these stored snapshots. The Axios mutator returns the HTTP body
  unchanged; it does not unwrap an application-level `data` property.
- No services were started, runtime specs fetched, client generation performed,
  or automated checks run. Whole-spec `operationId` uniqueness is unverified.

## Response and authorization boundaries

Do not infer a uniform success wrapper from ADR-010 alone. Current Movie and
the Inventory endpoints below return raw response DTOs. Booking and Payment
management explicitly return `ApiResponse<T>`. No response-wrapping advice was
found in the shared response/API modules. Changing these bodies would affect
existing clients and is outside this P0.3 change.

`ApiResponse<T>` contains `success`, `timestamp`, `data`, and `error`.
Null properties may be omitted by the shared Jackson configuration unless a DTO
explicitly overrides inclusion. Examples below are response excerpts.
`PageResponse<T>` contains `content` and `page`; `PageInfo` contains `page`,
`size`, `totalElements`, `totalPages`, `first`, and `last`.
Domain errors handled by `GlobalExceptionHandler` use `ApiResponse` with an
`error` containing `code`, `message`, `category`, and `details`. A `204` response
has no JSON body. Do not access `.data` uniformly on every successful endpoint.

| Flow / route | Body and status from source | Authorization boundary |
| --- | --- | --- |
| `POST /api/v1/movies` | `201`, raw `MovieResponse`; movie and versions in one transaction | Service: `movie:manage`; Gateway requires authentication for writes |
| `PUT /api/v1/movies/{id}` | `200`, raw `MovieResponse`; full replacement including every persisted screening version | Same as create |
| `GET /api/v1/movies/catalog` | `200`, raw `PageResponse<MovieResponse>` | Public GET at service and Gateway |
| `GET /api/v1/movies/{movieId}/screening-versions` | `200`, raw array; omit `active` to include inactive versions | Public GET |
| `POST /api/v1/movies/{movieId}/screening-versions` | `201`, raw version; duplicate full identity is `409`, not an idempotent success | Service: `movie:manage`; retained existing API, not an extra step after movie creation |
| `PUT /api/v1/movies/{movieId}/screening-versions/{versionId}/active` | `200`, raw version; requires `active` and `expectedVersion` | Service: `movie:manage` |
| `POST /api/v1/showtimes` | `201`, raw `ShowtimeResponse` | Service: `showtime:manage`; authenticated write at Gateway |
| `GET /api/v1/showtimes/bookable` | `200`, raw array; `cinemaId`, `from`, `to`, optional `movieId` | Public GET |
| `GET /api/v1/showtimes/{showtimeId}/seat-map` | `200`, raw `ShowtimeSeatMapResponse`; read does not reserve seats | Public GET |
| `GET /api/v1/showtimes/{showtimeId}/seat-quote` | `200`, raw `ShowtimeSeatQuoteResponse`; repeated `seatNumbers` query parameters | Public GET; default-group quote |
| `POST /api/v1/showtimes/{showtimeId}/seat-quote` | `200`, raw `ShowtimeGroupedSeatQuoteResponse`; per-seat ticket groups | Direct Inventory permits anonymous POST; current Gateway requires authentication for this POST |
| `POST /api/v1/bookings` | `202`, `ApiResponse<BookingResponse>`, `Location` points to the booking | Authenticated; owner derived from `CurrentUser.id()` |
| `GET /api/v1/bookings`, `GET /api/v1/bookings/{bookingId}` | `200`, wrapped page or booking | Authenticated; service enforces ownership |
| `POST /api/v1/bookings/{bookingId}/cancel` | `200`, wrapped booking | Authenticated owner; valid lifecycle state required |
| `GET /api/v1/bookings/management/**` | Admin read operations from `BookingManagementController` | ADMIN role at service and Gateway |
| `GET /api/v1/payments/management/by-booking/{bookingId}` | `200`, wrapped page of payment attempts | Service: ADMIN **and** `payment:read`; Gateway: ADMIN |
| `GET /api/v1/payments/management/{paymentId}` | `200`, wrapped payment | Same as payment list |

The Gateway and each service independently enforce their configured boundary.
This review records the POST quote distinction; it does not grant anonymous
Gateway access or introduce new permissions. Customers create bookings through
Booking; direct ShowSeat hold/book/release APIs require `inventory:write`.
Refund and reconciliation administration HTTP APIs were removed and explicitly
denied in `ae85063`; their historical R27 descriptions do not authorize FE use.

## Operation IDs

| Source location | Explicit operation IDs relevant to these flows |
| --- | --- |
| `MovieController` | `getMovieCatalog`, `updateMovieMetadata` |
| `MovieScreeningVersionController` | `listMovieScreeningVersions`, `getMovieScreeningVersion`, `createMovieScreeningVersion`, `updateMovieScreeningVersionActive` |
| `ShowtimeSeatMapController` | `getShowtimeSeatMap` |
| `ShowtimeSeatQuoteController` | `getShowtimeSeatQuote` |
| `ShowtimeGroupedSeatQuoteController` | `quoteShowtimeSeats` |
| `PaymentManagementController` | `listBookingPaymentsForAdmin`, `getPaymentForAdmin` |

Other handlers in this review, including movie create/update, showtime create
and customer Booking operations, do not declare explicit `operationId` values.
Their generated FE function names are evidence of a prior export, not proof of
collision-free IDs in the next runtime spec. No operation IDs were renamed.
Before client regeneration, review all paths in each service spec for missing,
duplicate or unexpectedly suffixed IDs. Preserve existing client imports when
settling any naming change; do not assume IDs must be unique across independent
service documents.

## Request/response examples

All IDs, titles, amounts and times below are synthetic. Replace IDs with actual
records from the owning service. Request bodies are complete examples for the
stated operation. Response examples are **excerpts**, with unrelated DTO fields
omitted; they are not full schemas or captured runtime output.

### Admin: create and replace a movie

`POST /api/v1/movies`:

```json
{
  "title": "Contract Example",
  "description": "Synthetic integration example",
  "durationMinutes": 120,
  "releaseDate": "2026-11-01",
  "status": "NOW_SHOWING",
  "genreIds": ["019a5a00-0000-7000-8000-000000000001"],
  "screeningVersions": [{
    "contentFormat": "2D",
    "projectionTechnology": "STANDARD",
    "audioLanguage": "en",
    "presentationType": "SUBTITLED",
    "subtitleLanguage": "vi"
  }]
}
```

`201` response excerpt (raw DTO):

```json
{
  "id": "019a5a00-0000-7000-8000-000000000002",
  "title": "Contract Example",
  "screeningVersions": [{
    "id": "019a5a00-0000-7000-8000-000000000003",
    "movieId": "019a5a00-0000-7000-8000-000000000002",
    "contentFormat": "2D",
    "projectionTechnology": "STANDARD",
    "audioLanguage": "en",
    "presentationType": "SUBTITLED",
    "subtitleLanguage": "vi",
    "active": true,
    "version": 0
  }]
}
```

For `PUT /api/v1/movies/019a5a00-0000-7000-8000-000000000002`, use the full
create body above and replace its `screeningVersions` value with:

```json
[{
  "id": "019a5a00-0000-7000-8000-000000000003",
  "expectedVersion": 0,
  "contentFormat": "2D",
  "projectionTechnology": "STANDARD",
  "audioLanguage": "en",
  "presentationType": "SUBTITLED",
  "subtitleLanguage": "vi",
  "active": true
}]
```

The response is `200` with the same raw DTO shape. Load every persisted version
first, including inactive ones. Keep each ID and use its returned `version` as
`expectedVersion`; supply new versions without either field. Definition fields
of persisted versions are immutable. Language comparison uses the same normalized
identity as request duplicate detection: entity construction trims input, then
comparison lowercases language values with `Locale.ROOT`. Case-only resubmission
is accepted without rewriting the stored language text; ID/version checks and
actual definition changes retain their existing conflict behavior. This Java key
does not emulate all MySQL accent/Unicode collation rules; the existing database
unique constraint remains authoritative for persisted uniqueness.
To stop offering a version, retain it with
`active: false`, rather than omit or delete it. Both movie writes require a
non-null, non-empty list. `2D`/`3D` are wire format values; projection technology
is a separate field. Do not derive supported combinations by Cartesian product.

Service-level failures include `MOVIE_SCREENING_VERSION_SET_INCOMPLETE` (400),
`MOVIE_SCREENING_VERSION_CONFLICT` (409), and
`MOVIE_SCREENING_VERSION_ALREADY_EXISTS` (409). Bean validation may reject an
invalid body first with `VALIDATION_ERROR`; the service code is not guaranteed
for every invalid HTTP request.

The retained standalone version-create endpoint uses the same definition body
as a create-list item. It rejects a duplicate full identity, including an
inactive match. This P0.3 change adds its missing OpenAPI 409 response without
changing that behavior or the existing operation ID.

Duplicate-create `409` error excerpt:

```json
{
  "success": false,
  "error": {
    "code": "MOVIE_SCREENING_VERSION_ALREADY_EXISTS",
    "message": "An identical movie screening version already exists",
    "category": "BUSINESS"
  }
}
```

### Admin: schedule a showtime

`POST /api/v1/showtimes`:

```json
{
  "movieId": "019a5a00-0000-7000-8000-000000000002",
  "roomId": "019a5a00-0000-7000-8000-000000000004",
  "roomLayoutId": "019a5a00-0000-7000-8000-000000000005",
  "movieScreeningVersionId": "019a5a00-0000-7000-8000-000000000003",
  "pricingPolicyVersionId": "019a5a00-0000-7000-8000-000000000006",
  "startsAt": "2026-12-01T10:00:00Z",
  "endsAt": "2026-12-01T12:00:00Z",
  "breakMinutes": 20
}
```

`201` response excerpt:

```json
{
  "id": "019a5a00-0000-7000-8000-000000000007",
  "status": "SCHEDULED",
  "roomLayoutId": "019a5a00-0000-7000-8000-000000000005",
  "pricingPolicyVersionId": "019a5a00-0000-7000-8000-000000000006"
}
```

The layout must belong to the room and be published. The screening version must
be active and belong to the movie; room capability, release date and schedule
overlap are validated by Inventory using its service contracts. Supplying an
applicable published pricing version generates ShowSeat fares. Omit it to
schedule without generating seats. Creating a showtime does not open booking;
the existing `PATCH /api/v1/showtimes/{showtimeId}/open` transition has its own
pricing/readiness guards. Unknown legacy price properties are rejected by
`CreateShowtimeRequest`, not silently accepted.

### Customer: quote then create a booking

For the legacy/default-group path, use
`GET /api/v1/showtimes/{showtimeId}/seat-quote?seatNumbers=H7&seatNumbers=H8`.
For per-seat ticket groups, use
`POST /api/v1/showtimes/019a5a00-0000-7000-8000-000000000007/seat-quote`:

```json
{
  "seats": [{
    "seatNumber": "H7",
    "ticketGroupId": "019a5a00-0000-7000-8000-000000000008"
  }]
}
```

`200` grouped-quote response excerpt:

```json
{
  "showtimeId": "019a5a00-0000-7000-8000-000000000007",
  "seatCount": 1,
  "capacity": 2,
  "currency": "VND",
  "totalAmount": 180000,
  "seats": [{
    "seatNumber": "H7",
    "seatType": "COUPLE",
    "capacity": 2,
    "ticketGroupId": "019a5a00-0000-7000-8000-000000000008",
    "unit": "PAIR",
    "price": 180000
  }]
}
```

The group must belong to the assigned published pricing version. Omit it to
use that version's default group. The quote does not reserve seats or verify
ticket-group eligibility. COUPLE counts as one inventory seat and two people;
its price is for the entire pair. Do not multiply it by capacity or impose a
single price across ticket groups, time bands or formats.

`POST /api/v1/bookings` using the quoted selection:

```json
{
  "clientRequestId": "contract-example-001",
  "showtimeId": "019a5a00-0000-7000-8000-000000000007",
  "expectedTotalAmount": 180000,
  "seats": [{
    "seatNumber": "H7",
    "ticketGroupId": "019a5a00-0000-7000-8000-000000000008"
  }]
}
```

`202` response excerpt (application envelope retained):

```json
{
  "success": true,
  "data": {
    "id": "019a5a00-0000-7000-8000-000000000009",
    "clientRequestId": "contract-example-001",
    "status": "PENDING"
  }
}
```

Exactly one non-empty selection list is accepted: `seatNumbers` or `seats`.
Omit the unused property; sending it as an empty list still fails validation.
`expectedTotalAmount` is an optional comparison value, not authoritative pricing.
Reservation and payment continuation are asynchronous. Follow the `Location`
with the current user's booking GET; do not treat `202` as a completed hold or
confirmed payment. Reuse `clientRequestId` for an identical retry by the same
user. Reusing it with a different fingerprint yields
`BOOKING_CLIENT_REQUEST_ID_PAYLOAD_MISMATCH` (409). See
[Booking design](16_BOOKING_SERVICE_DESIGN.md) for lifecycle and event details.

### Admin: inspect payment attempts

`GET /api/v1/payments/management/by-booking/019a5a00-0000-7000-8000-000000000009?page=0&size=20`
returns `200` with a wrapped page. Empty result example:

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": {
      "page": 0, "size": 20, "totalElements": 0,
      "totalPages": 0, "first": true, "last": true
    }
  }
}
```

An empty page means no matching Payment record exists. Use the returned
Payment IDs for management detail/transaction reads. No customer payment-init,
refund, reconciliation or Notification endpoint is added by this reference.

## Migration and FE rollout

No migration or wire-format change is introduced in this P0.3 increment.
Existing Movie V2 adds `projection_technology` and expands the full-identity
unique key; Inventory V2 adds and backfills showtime projection snapshots.
Keep each service's migrations with its matching service deployment. Deploy
Movie's updated contract before Inventory depends on its projection metadata,
then export specs from the actual deployed revisions before regenerating FE.
Never edit an applied migration or hand-edit generated clients to hide drift.

For regeneration: record service URL, actual source/build revision, working-tree
state, export time and OpenAPI version; review IDs, success bodies, errors and
security; regenerate through the existing FE workflow only when authorized.
This review does not modify FE or certify runtime/schema compatibility.

## Remaining P0.3 work

- Fetch runtime specs with known source provenance when runtime inspection is
  available; review whole-spec operation IDs and the emitted 409 schema.
- Reconcile the account/layout/pricing source review and examples in the
  [supplement](20_FE_ACCOUNT_LAYOUT_PRICING_CONTRACT.md) with runtime specs and
  the supported account authentication topology. User list/search APIs are not
  provided by the current account controllers and are not added here.
- Track the source/target matrix discrepancy under P2: current seat-map
  documentation declares COUPLE `columnSpan=2`, whereas BE roadmap P2 targets
  one cell per couple seat. P0.3 does not change that business contract or ask
  FE to convert a matrix to geometry.
- P0.2 verification remains open. Tests, builds and automated checks are still
  skipped. R28 Notification remains deferred. No P0.3 runtime closure is claimed.
