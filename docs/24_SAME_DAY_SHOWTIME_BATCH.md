# Create multiple showtime slots on one day

Reviewed checkout `8a609ed`. The existing Backend batch endpoint already supports
this workflow; do not introduce a second batch API or issue sequential single
POSTs from the UI to simulate atomic creation.

## Existing API

```http
POST /api/v1/showtimes/batch
Content-Type: application/json
```

Inventory requires showtime:manage; Gateway authenticates and forwards the token.
CreateShowtimeBatchRequest accepts one clientRequestId and 1–50 complete
CreateShowtimeRequest objects. All slots can share movie, room, screening version,
layout, pricing and break minutes while their timestamps differ.

Example only: these are three 95-minute slots in one local day, with 15-minute
breaks. Replace resource IDs with current returned IDs before use. No request in
this document has been executed.

```json
{
  "clientRequestId": "99befa02-53dc-43a1-af48-b72ff07a14e2",
  "showtimes": [
    {
      "movieId": "03da36dd-7998-5759-ad29-302a7645a083",
      "roomId": "01a1c394-4014-77c5-bb78-edb7ecc0c0fa",
      "movieScreeningVersionId": "01a1c383-28d4-71a3-96ff-5cd65f88e407",
      "roomLayoutId": "01a1c395-0c02-7944-baea-3bf2d4a550de",
      "pricingPolicyVersionId": "01a1c396-a24e-748e-9113-e851fe5d0897",
      "startsAt": "2026-11-12T09:00:00+07:00",
      "endsAt": "2026-11-12T10:35:00+07:00",
      "breakMinutes": 15
    },
    {
      "movieId": "03da36dd-7998-5759-ad29-302a7645a083",
      "roomId": "01a1c394-4014-77c5-bb78-edb7ecc0c0fa",
      "movieScreeningVersionId": "01a1c383-28d4-71a3-96ff-5cd65f88e407",
      "roomLayoutId": "01a1c395-0c02-7944-baea-3bf2d4a550de",
      "pricingPolicyVersionId": "01a1c396-a24e-748e-9113-e851fe5d0897",
      "startsAt": "2026-11-12T11:00:00+07:00",
      "endsAt": "2026-11-12T12:35:00+07:00",
      "breakMinutes": 15
    },
    {
      "movieId": "03da36dd-7998-5759-ad29-302a7645a083",
      "roomId": "01a1c394-4014-77c5-bb78-edb7ecc0c0fa",
      "movieScreeningVersionId": "01a1c383-28d4-71a3-96ff-5cd65f88e407",
      "roomLayoutId": "01a1c395-0c02-7944-baea-3bf2d4a550de",
      "pricingPolicyVersionId": "01a1c396-a24e-748e-9113-e851fe5d0897",
      "startsAt": "2026-11-12T14:00:00+07:00",
      "endsAt": "2026-11-12T15:35:00+07:00",
      "breakMinutes": 15
    }
  ]
}
```

## Guarantees already in source

- Return 201 and a raw array of ShowtimeResponse, in request order. Returned
  showtimes start SCHEDULED; creation does not open booking immediately. The
  automatic sales lifecycle opens them at local midnight seven calendar days
  before their show date and closes them five minutes after `endsAt`; see
  `22_SHOWTIME_MANAGEMENT_API.md`.
- Validate future start, movie duration/release date, active movie version,
  room/cinema availability, room format/projection compatibility and layout/pricing
  readiness using the same create logic as a single showtime.
- Cache Movie reads within the request, then lock all involved rooms in stable
  UUID order. Persist the entire batch in one transaction. A failure on any slot
  rolls back all slots, generated seats and the batch idempotency record.
- Detect overlap with existing showtimes and earlier slots in the same batch,
  including cleanup break time. A next slot may start at the prior readyAt;
  cancelled showtimes do not block scheduling. Input order need not be chronological.
- Reusing clientRequestId with the identical ordered payload returns the stored
  result without creating again. Different payload/order with the same ID returns
  409 INVENTORY_SHOWTIME_BATCH_REQUEST_CONFLICT. Equivalent offset representations
  of the same instants produce the same fingerprint.
- Item-level business failures preserve the original error code/status and add
  error.details field paths such as showtimes[1] (zero-based). Transport validation
  can identify nested fields such as showtimes[1].startsAt.
- Generic batch creation can also carry different rooms/dates. A same-day UI can
  limit shared inputs without narrowing or breaking this existing Backend API.

## Current FE gap and concrete integration

The generated Inventory client already exports createShowtimeBatch and its DTO.
AdminShowtimeCreatePage and use-admin-showtime-slots already provide multiple
planning rows, duration-based end times, readyAt and client-side overlap checks.
However, creationRequest returns null for multipleSlots, canSubmit requires
!multipleSlots, and the UI displays that saving multiple slots is unavailable.
The current mutation calls only createShowtime and expects one response.

To enable the UI within an explicitly authorized FE task:

1. Build a complete request per validated slot using the shared context/date.
2. Call the existing batch client once for multiple rows. Keep a clientRequestId
   stable for the exact ordered payload across transport retries; never generate
   a fresh ID automatically after an uncertain result.
3. Validate the returned array length, unique IDs and each item's complete
   movie/room/version/layout/pricing/time/scheduling context against its request.
4. Show all created slots and refresh affected showtime/room-capacity caches only
   after a confirmed result. Keep errors and uncertain outcomes distinct. Surface
   showtimes[index] errors next to the matching planning row.
5. Preserve single-slot behavior. Opening booking remains an explicit subsequent
   action for each created showtime, rather than automatic batch creation behavior.

No Backend code, API, DTO, permission, Kafka contract or MySQL migration change
is required for this existing capability. The showtime_batch_requests table is
already in Inventory V1. No tests, builds, database commands or batch POSTs were
run during this source review. FE remains read only until scope approval.
