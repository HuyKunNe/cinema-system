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

- Continuation from `0ec5532` adds the paged, permission-protected
  [showtime management API](22_SHOWTIME_MANAGEMENT_API.md). It preserves the older
  array endpoints used by current FE and has not been adopted by that client.

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

P3.3 continuation from `1744372`: the existing PricingFareResolver now enforces
non-null/nonnegative PRICED amounts and the seat-type fare unit for pricing quotes,
grouped showtime quotes, generated default prices and reservation snapshots.
STANDARD/VIP/ACCESSIBLE use SEAT; COUPLE uses PAIR. Zero remains configured/free,
UNCONFIGURED remains inconsistent and NOT_APPLICABLE remains unavailable. This
does not multiply pair amounts, synchronize 2D/3D fares, change eligibility rules
or reprice stored bookings. Legacy GET seat-quote reads ShowSeat default-group
price snapshots; grouped POST uses the selected group's published fare resolver.

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

| Flow / route                                                         | Body and status from source                                                                                | Authorization boundary                                                                         |
| -------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| `POST /api/v1/movies`                                                | `201`, raw `MovieResponse`; movie and versions in one transaction                                          | Service: `movie:manage`; Gateway requires authentication for writes                            |
| `PUT /api/v1/movies/{id}`                                            | `200`, raw `MovieResponse`; full replacement including every persisted screening version                   | Same as create                                                                                 |
| `GET /api/v1/movies/catalog`                                         | `200`, raw `PageResponse<MovieResponse>`                                                                   | Public GET at service and Gateway                                                              |
| `GET /api/v1/movies/{movieId}/screening-versions`                    | `200`, raw array; omit `active` to include inactive versions                                               | Public GET                                                                                     |
| `POST /api/v1/movies/{movieId}/screening-versions`                   | `201`, raw version; duplicate full identity is `409`, not an idempotent success                            | Service: `movie:manage`; retained existing API, not an extra step after movie creation         |
| `PUT /api/v1/movies/{movieId}/screening-versions/{versionId}/active` | `200`, raw version; requires `active` and `expectedVersion`                                                | Service: `movie:manage`                                                                        |
| `POST /api/v1/showtimes`                                             | `201`, raw `ShowtimeResponse`                                                                              | Service: `showtime:manage`; authenticated write at Gateway                                     |
| `POST /api/v1/showtimes/batch`                                       | `201`, raw ordered `ShowtimeResponse[]`; accepts 1–50 complete showtime requests and one `clientRequestId` | Service: `showtime:manage`; authenticated write at Gateway                                     |
| `GET /api/v1/showtimes/bookable`                                     | `200`, raw array; `cinemaId`, `from`, `to`, optional `movieId`                                             | Public GET                                                                                     |
| `GET /api/v1/showtimes/{showtimeId}/seat-map`                        | `200`, raw `ShowtimeSeatMapResponse`; read does not reserve seats                                          | Public GET                                                                                     |
| `GET /api/v1/showtimes/{showtimeId}/seat-quote`                      | `200`, raw `ShowtimeSeatQuoteResponse`; repeated `seatNumbers` query parameters                            | Public GET; default-group quote                                                                |
| `POST /api/v1/showtimes/{showtimeId}/seat-quote`                     | `200`, raw `ShowtimeGroupedSeatQuoteResponse`; per-seat ticket groups                                      | Direct Inventory permits anonymous POST; current Gateway requires authentication for this POST |
| `POST /api/v1/bookings`                                              | `202`, `ApiResponse<BookingResponse>`, `Location` points to the booking                                    | Authenticated; owner derived from `CurrentUser.id()`                                           |
| `GET /api/v1/bookings`, `GET /api/v1/bookings/{bookingId}`           | `200`, wrapped page or booking                                                                             | Authenticated; service enforces ownership                                                      |
| `POST /api/v1/bookings/{bookingId}/cancel`                           | `200`, wrapped booking                                                                                     | Authenticated owner; valid lifecycle state required                                            |
| `POST /api/v1/auth/registrations`                                    | `202`, generic response; email verification intent only, no password accepted                              | Public; per-address resend cooldown                                                            |
| `POST /api/v1/auth/email-verifications/confirm`                      | `204`; consumes email token and chosen password, creates verified `USER` account                           | Public; requires token possession                                                              |
| `GET /api/v1/bookings/management/**`                                 | Admin read operations from `BookingManagementController`                                                   | ADMIN role at service and Gateway                                                              |
| `GET /api/v1/payments/management/by-booking/{bookingId}`             | `200`, wrapped page of payment attempts                                                                    | Service: ADMIN **and** `payment:read`; Gateway: ADMIN                                          |
| `GET /api/v1/payments/management/{paymentId}`                        | `200`, wrapped payment                                                                                     | Same as payment list                                                                           |
| `GET /api/v1/payments/bookings/{bookingId}/checkout`                 | `200`, wrapped payment status and nullable `checkoutUri`                                                   | Authenticated owner; Payment Service matches both booking and current user                     |

The Gateway and each service independently enforce their configured boundary.
This review records the POST quote distinction; it does not grant anonymous
Gateway access or introduce new permissions. Customers create bookings through
Booking; direct ShowSeat hold/book/release APIs require `inventory:write`.
Refund and reconciliation administration HTTP APIs were removed and explicitly
denied in `ae85063`; their historical R27 descriptions do not authorize FE use.

Registration and Google OIDC setup, payloads and remaining FE verification UI
are documented in
[`27_USER_REGISTRATION_GOOGLE_LOGIN.md`](27_USER_REGISTRATION_GOOGLE_LOGIN.md).

For same-day multi-slot creation, use the existing atomic batch route instead
of issuing sequential single-showtime POSTs. Reuse `clientRequestId` only for an
identical ordered payload; the same ID with a changed payload returns 409. The
complete request, transaction, overlap, rollback and retry contract is in
[`24_SAME_DAY_SHOWTIME_BATCH.md`](24_SAME_DAY_SHOWTIME_BATCH.md).

## Operation IDs

| Source location                      | Explicit operation IDs relevant to these flows                                                                               |
| ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| `MovieController`                    | `getMovieCatalog`, `updateMovieMetadata`                                                                                     |
| `MovieScreeningVersionController`    | `listMovieScreeningVersions`, `getMovieScreeningVersion`, `createMovieScreeningVersion`, `updateMovieScreeningVersionActive` |
| `ShowtimeSeatMapController`          | `getShowtimeSeatMap`                                                                                                         |
| `ShowtimeSeatQuoteController`        | `getShowtimeSeatQuote`                                                                                                       |
| `ShowtimeGroupedSeatQuoteController` | `quoteShowtimeSeats`                                                                                                         |
| `PaymentManagementController`        | `listBookingPaymentsForAdmin`, `getPaymentForAdmin`                                                                          |

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
  "screeningVersions": [
    {
      "contentFormat": "2D",
      "projectionTechnology": "STANDARD",
      "audioLanguage": "en",
      "presentationType": "SUBTITLED",
      "subtitleLanguage": "vi"
    }
  ]
}
```

`201` response excerpt (raw DTO):

```json
{
  "id": "019a5a00-0000-7000-8000-000000000002",
  "title": "Contract Example",
  "screeningVersions": [
    {
      "id": "019a5a00-0000-7000-8000-000000000003",
      "movieId": "019a5a00-0000-7000-8000-000000000002",
      "contentFormat": "2D",
      "projectionTechnology": "STANDARD",
      "audioLanguage": "en",
      "presentationType": "SUBTITLED",
      "subtitleLanguage": "vi",
      "active": true,
      "version": 0
    }
  ]
}
```

For `PUT /api/v1/movies/019a5a00-0000-7000-8000-000000000002`, use the full
create body above and replace its `screeningVersions` value with:

```json
[
  {
    "id": "019a5a00-0000-7000-8000-000000000003",
    "expectedVersion": 0,
    "contentFormat": "2D",
    "projectionTechnology": "STANDARD",
    "audioLanguage": "en",
    "presentationType": "SUBTITLED",
    "subtitleLanguage": "vi",
    "active": true
  }
]
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
  "seats": [
    {
      "seatNumber": "H7",
      "ticketGroupId": "019a5a00-0000-7000-8000-000000000008"
    }
  ]
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
  "seats": [
    {
      "seatNumber": "H7",
      "seatType": "COUPLE",
      "capacity": 2,
      "ticketGroupId": "019a5a00-0000-7000-8000-000000000008",
      "unit": "PAIR",
      "price": 180000
    }
  ]
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
  "seats": [
    {
      "seatNumber": "H7",
      "ticketGroupId": "019a5a00-0000-7000-8000-000000000008"
    }
  ]
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
      "page": 0,
      "size": 20,
      "totalElements": 0,
      "totalPages": 0,
      "first": true,
      "last": true
    }
  }
}
```

An empty page means no matching Payment record exists. Use the returned
Payment IDs for management detail/transaction reads. No customer payment-init,
refund, reconciliation or Notification endpoint is added by this reference.

### Customer: read the hosted checkout URL

After the booking reaches `RESERVED`, the authenticated owner may poll
`GET /api/v1/payments/bookings/{bookingId}/checkout`. A missing Payment record
for that user returns `404`; once created, the response is `200` even while the
provider worker is still processing. `checkoutUri` is non-null only when the
latest charge attempt is `PENDING_PROVIDER`, the reservation has not expired,
and the provider returned an absolute HTTP(S) checkout URL. The response is
`Cache-Control: no-store`; FE must not log or persist the URL and should navigate
to it only while the reservation remains valid. This is a read endpoint; it does
not create another payment attempt or change booking/payment status.

```json
{
  "success": true,
  "data": {
    "bookingId": "019a5a00-0000-7000-8000-000000000009",
    "paymentId": "019a5a00-0000-7000-8000-000000000010",
    "status": "PENDING_PROVIDER",
    "checkoutUri": "https://pay.example/checkout/session",
    "holdExpiresAt": "2026-11-12T16:30:00Z"
  }
}
```

The current `MOCK` provider completes internally and returns no checkout URL.
This contract only surfaces a URL from a configured hosted-checkout adapter; it
does not provide that provider integration or its callback credentials.

## Migration and FE rollout

The P0.3 contract review itself introduced no migration. The separate P4.3
checkout-read contract adds Payment V2, which only adds nullable
`payment_transactions.checkout_uri`; existing rows remain null. Rollback, if
needed before relying on persisted hosted sessions, is a manual
`ALTER TABLE payment_transactions DROP COLUMN checkout_uri`; this discards any
active provider checkout URLs. Migration and rollback have not been run.
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

## P4.1 continuation — complete seat reservation (2026-11-11)

Source review found that SeatReservationRequestedConsumerServiceImpl acquired
seat locks but did not use its existing ownership/availability helper or its
SEAT_NOT_FOUND/SEAT_UNAVAILABLE rejection paths. Consequently an ungrouped
request could reserve only the returned subset, or report success while some
returned seats remained held/booked by another booking. The existing
SeatReservationRequestedIntegrationTest already specifies missing-seat rejection
and one winning booking under contention; those cases were not executed here.

The consumer now validates the complete normalized seat set after acquiring
PESSIMISTIC_WRITE locks and checking Showtime eligibility, before grouped pricing,
total calculation or hold mutation:

| Condition                                                                                      | Existing event/reason                                        | Seat changes                                          |
| ---------------------------------------------------------------------------------------------- | ------------------------------------------------------------ | ----------------------------------------------------- |
| Any requested seat missing, including an empty lookup                                          | seat-reservation-rejected / SEAT_NOT_FOUND                   | None; affectedSeats contains missing numbers.         |
| Any seat booked, unavailable, held by another booking, expired or held with a different expiry | seat-reservation-rejected / SEAT_UNAVAILABLE                 | None; affectedSeats contains unusable numbers.        |
| Every seat available or already held by this booking with the same unexpired deadline          | Existing price/expiry/eligibility checks, then seat-reserved | Hold AVAILABLE seats; retain matching existing holds. |

Hold deadlines compare instants: UTC and +07:00 representations of the same
expiry are equivalent. A held seat without an expiry is not reusable. Expired
holds are not reclaimed by this consumer; the existing expiry lifecycle remains
responsible for release. No other booking's hold is overwritten.

The existing processed-event marker and rejection/success Outbox records remain
in the caller's transaction. Duplicate-event handling, the final expiry and
Showtime eligibility checks, event versions, correlation/causation IDs, schemas
and REST APIs are unchanged. Booking already accepts both rejection reason codes;
no client, permission, MySQL migration or OAuth/OIDC change is required. FE and
Booking source were not modified.

Added unexecuted SeatReservationLockedSetTest source cases for missing/empty
lookups, grouped-pricing rejection before resolution, every unusable-seat case,
unchanged ownership on rejection and same-booking reuse with different timestamp
offsets. No tests, applications, Kafka messages or database commands were run.
Runtime concurrency/rollback and HTTP verification remain open; P0.2 and P4.1
are not closed by this source fix.

Manual development-environment review (not performed here): request an available
seat together with a missing one, then with a seat held by another booking.
Confirm each booking receives the corresponding rejection and the available
seat is not partially held. For a fully available set, confirm one reservation
contains all seats. Review duplicate delivery for one processed marker/Outbox
result and matching same-booking holds for unchanged ownership/deadline.

Inventory, reactor dependencies and all test sources compiled successfully with:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/inventory-service -am -DskipTests test-compile
```

`git diff --check` passed. Compilation is not test execution or runtime evidence.

## P4.2 continuation — reservation deadline instants (2026-11-11)

SeatReservedConsumerServiceImpl previously compared Booking.expiresAt with the
Inventory seat-reserved holdExpiresAt using OffsetDateTime.equals. That compares
both the local timestamp and offset, so equivalent UTC/+07:00 representations
could raise RESERVATION_RESULT_MISMATCH before completing the Booking snapshots.

The consumer now uses isEqual to require the same instant. It does not change
the stored deadline, extend a hold, round precision or introduce a grace period.
Any different deadline instant still fails with the existing mismatch exception
before reading or completing BookingSeat snapshots, saving the Booking or
creating payment-requested. Existing event registration/transaction rollback,
duplicate delivery, seat/group/price matching and payment-requested behavior
remain unchanged. Event/REST schemas, MySQL migration/configuration, OAuth/OIDC,
FE clients and service permissions are unchanged.

Added unexecuted source regression cases to SeatReservedConsumerServiceImplTest:
matching instants at +07:00/-05:00 proceed through snapshot/payment Outbox
creation while preserving the stored deadline; genuinely different instants,
including +/-1 nanosecond, fail without modifying Booking/snapshots or creating
a payment request. The existing minute-level mismatch case is retained.

Manual review (not executed here): in a development event flow, compare a
booking's expiresAt with a seat-reserved deadline expressed at another offset
but the same instant. Confirm RESERVED, unchanged expiration and the existing
payment-requested result. A different deadline must fail with the mismatch
exception, with no persisted snapshot/state/payment Outbox changes. Review
duplicate delivery separately through the existing processed-event mechanism.
No tests, applications, event submissions or database commands were run for
this source change. P0.2 and P4.2 runtime gates remain open.

Booking, reactor dependencies and all test sources compiled successfully with:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/booking-service -am -DskipTests test-compile
```

`git diff --check` passed. No tests were executed; compile success does not prove
runtime Kafka, transaction rollback or MySQL behavior.

## P4.3 continuation — provider-result deadline snapshot (2026-11-11)

PaymentProviderOperationPreparationServiceImpl captures the existing Payment
deadline in ProviderChargeCommand before calling the provider. On result
application, PaymentProviderResultApplicationServiceImpl locks Payment then
PaymentTransaction, checks lease ownership and validates the immutable command
snapshot against the stored records.

The deadline comparison now requires the same instant with isEqual rather than
OffsetDateTime.equals, which also compares the offset. A command deadline at
+07:00/-05:00 representing the stored UTC instant no longer raises
PAYMENT_TRANSACTION_DATA_MISMATCH. IDs, provider, idempotency key, amount,
currency and lease ownership checks are unchanged. A genuinely different
deadline instant still fails before any result-state changes or terminal Outbox
handling; no tolerance, rounding or hold extension is introduced.

Result lateness is still evaluated by the existing Payment entity using the
stored deadline and result clock. A success at/after hold expiration still uses
RECONCILIATION_REQUIRED; it is not converted into an on-time success by offset
normalization. Stale/replaced processing owners remain rejected. API/event
schemas, terminal event publication rules, return URLs, OAuth/OIDC, MySQL
configuration/migrations and FE are unchanged. No refund behavior is added.

Added unexecuted parameterized source cases to
PaymentProviderResultApplicationServiceImplTest for equivalent +07:00/-05:00
deadlines before/at expiration, and real +/-1 nanosecond mismatches preserving
payment/transaction status, lease owner and absence of terminal Outbox handling.
The existing stale-owner and late-success regression sources remain unchanged.

Manual development review (not performed here): compare a claimed command's
deadline in a different offset with the locked Payment deadline. Verify an
equivalent instant follows the normal on-time or late-result path, preserving
the stored deadline. A different instant must fail with the existing mismatch
error without applying the provider result. Runtime provider/Kafka, transaction
rollback and MySQL concurrency verification remain pending. No tests,
applications, provider calls, endpoint calls or database commands were executed.

Payment, reactor dependencies and all test sources compiled successfully with:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/payment-service -am -DskipTests test-compile
```

`git diff --check` passed. Tests were not executed; compilation does not close
the P0.2/P4.3 runtime verification gates.

## P5.1 continuation — customer booking history ordering (2026-11-11)

The existing `GET /api/v1/bookings?page=0&size=20` now orders by
`createdAt DESC, id DESC`. Previously equal creation timestamps had no unique
ordering, so their positions in offset pages were not guaranteed. The new
ordering matches the existing management service and uses Booking's unique ID
only as a tie-breaker; it does not replace createdAt as the primary key.

The endpoint still derives the owner from CurrentUser.id() and uses
findAllByUserId. Existing wrapped PageResponse, batched seat-snapshot loading,
default page=0/size=20 and controller limits page>=0, size=1..100 are unchanged.
The generated FE findAllBookings client already targets this API; no parallel
history endpoint, new request/response type or FE modification is needed. No
authorization, OAuth/OIDC, Kafka or MySQL schema/index/configuration changes.

This is deterministic ordering for an unchanged dataset, not a snapshot across
requests. New/deleted records between page loads can still shift offset pages;
this change introduces no cursor contract or frozen-list guarantee.

Manual review (not executed here): on a development dataset with tied createdAt
values, compare adjacent page boundaries and repeat the same reads without
changing the data. Confirm descending IDs within each timestamp tie, no overlap
or skipped records at those boundaries, unchanged totals and owner-only results.
No tests, application startup, endpoint calls or database writes were performed.
P0.2/P5.1 runtime verification remains pending.

Booking and reactor production sources compiled successfully with:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/booking-service -am -DskipTests compile
```

The sandboxed attempt failed while reading the cached Eureka dependency; the
same command succeeded with approved access outside the sandbox. Static review
confirmed owner scoping and the existing controller/client contract;
`git diff --check` passed. No tests were written or executed for this small
ordering change; no runtime behavior or MySQL query plan is certified by compile.
