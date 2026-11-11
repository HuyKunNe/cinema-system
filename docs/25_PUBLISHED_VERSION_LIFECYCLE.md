# Published version lifecycle

## Source changes (2026-11-11)

Room layouts now support `DRAFT`, `PUBLISHED`, `RETIRED`. Publication locks the
room before the draft and previous published layouts. A successful publication
retires all previously published layouts for that room in the same transaction.
A draft whose layoutVersion is not newer than every published layout fails with
409 `INVENTORY_ROOM_LAYOUT_PUBLICATION_SUPERSEDED`. Validation/binding failures
roll back the transaction. No layout cells, seat IDs or Showtime references are
rewritten by retirement. Existing publishedAt is retained.

New Showtimes accept only a PUBLISHED layout. Existing Showtimes can read their
assigned PUBLISHED or RETIRED layout for seat-map and seat-generation operations.
Room seat-map reads with an explicit retired layout ID support history; reads
without an ID still select the latest PUBLISHED layout. Copying a PUBLISHED or
RETIRED matrix creates a new DRAFT. Copy locks room before source layout to avoid
reversing publication's lock order. Draft-only editing/deletion remains unchanged.

Pricing revisions already have RETIRED in the enum/schema. Added the entity
transition and compatibility for assigned Showtime pricing: refresh/open,
generation, ticket-price reads, grouped quotes and reservation pricing accept
the already assigned immutable RETIRED revision. New assignments and standalone
fare quotes still require PUBLISHED. Assignment takes a shared version-row lock
to serialize with retirement. A retired revision can be copied into a new draft;
it cannot be edited/deleted/republished directly. Retirement does not reassign a
Showtime, migrate seats, change prices or emit Kafka events.

## Pricing publication replacement

The continuation uses the conservative scope-preservation rule: a replacement
must cover the union of cinemas from every currently PUBLISHED revision of the
same policy. A different policy (for example weekday/weekend/holiday) is never
retired implicitly. Adding cinemas is allowed; removing a currently covered
cinema is rejected rather than silently ending its pricing coverage. A separate
explicit scope-withdrawal workflow is outside this change.

Publication holds the policy write lock, then locks the target and the published
revisions of that policy. The new revision must be newer than every published
revision. After scope and fare validation, publishing the target and retiring
all previous published revisions commit together. Failure rolls back both. The
old rows retain their cinema membership, publishedAt, publishedBy and fare data;
Showtime references and stored seat/booking prices are not reassigned or updated.

Additional conflicts from the existing publish endpoint:

| HTTP | Code | Meaning |
| --- | --- | --- |
| 409 | INVENTORY_PRICING_POLICY_PUBLICATION_SUPERSEDED | A newer/equal revision is already published; create a newer draft. |
| 409 | INVENTORY_PRICING_POLICY_PUBLICATION_SCOPE_CONFLICT | Add the missing currently covered cinemas to the draft before publishing. |

Publishing the same still-PUBLISHED revision remains an idempotent metadata
read, without another retirement pass. Publishing a RETIRED revision is rejected
by the existing draft-only guard; a replay cannot reactivate the old revision.

## MySQL rollout and rollback

Apply Inventory V4 before deploying code which writes RETIRED room layouts.
The migration changes only the two layout status/publication CHECK constraints;
it preserves rows, foreign keys and indexes. No JDBC, dialect or datasource
changes. Existing multiple PUBLISHED layouts are left intact until the next
successful publication for that room; they are then retired atomically.
Pricing needs no new migration: RETIRED is already accepted by the existing
MySQL constraint. Legacy multiple PUBLISHED pricing revisions are retained until
the next successful draft publication of that policy, which must cover the union
of their scopes and retires them all. No database backfill is executed here.

MySQL DDL implicitly commits. Back up/check the schema before deployment; do not
assume transaction rollback undoes ALTER TABLE. A failed deployment should first
be diagnosed from the actual constraint state. Do not run the old application
against RETIRED layout rows: its enum/read checks do not support them. Prefer a
forward fix. Rolling back would require an explicitly approved data policy for
retired rows, restoring the old constraints only after all such rows are handled;
do not delete history or automatically promote retired layouts as a rollback.

## API / FE

No endpoint, request field, success response shape, authorization or event changes.
RoomLayoutStatus adds RETIRED to generated OpenAPI enums. Export the runtime spec
and regenerate/update FE clients and status validators before coordinated rollout.
Read-only FE inspection found PUBLISHED-only checks in matrix copy and a
PUBLISHED-only parser for selecting the current layout. Keep current-layout
selection strict, but add RETIRED support to history/copy views. Do not offer
RETIRED versions for new Showtimes. FE was not modified.

## Verification

Added unexecuted regression sources for retired pricing reuse versus new
assignment, immutable retired entities, fare resolver boundaries and layout
publication retirement/order protection. Maven is used only with `-DskipTests`
for compilation. No tests, applications, endpoint calls or database/migration
commands have been executed for this change. Runtime MySQL locking, migration,
transaction rollback and HTTP/OpenAPI verification remain pending.

Manual review in a development environment after deployment:

1. Publish a newer layout for a room with an existing published layout. Verify
   new=PUBLISHED, old=RETIRED and unchanged historical layout/Showtime IDs.
2. Attempt assigning the retired layout to a new Showtime: expect the existing
   layout-not-published conflict. Read the old Showtime seat map successfully.
3. Verify an old SCHEDULED Showtime can still open using its assigned retired
   layout/pricing, assuming its unchanged pricing and seat configuration is valid.
4. Copy a retired layout/pricing revision to a draft; verify edit/delete remain
   draft-only. Publishing an obsolete layout draft should fail with 409.
5. Review simultaneous publications and a failing publication transaction on
   MySQL, confirming only the successful new layout becomes current and failed
   publication does not retire the previous layout.
6. For pricing, publish a newer draft covering all cinemas of the currently
   published revisions of one policy. Verify new=PUBLISHED, previous=RETIRED;
   revisions of other policies are unchanged. Confirm historical Showtime
   quotes/open/hold still use the previously assigned IDs and fares.
7. Attempt publishing a draft omitting a covered cinema and one older than the
   current published revision: expect the respective 409 codes above without
   any retirement. Invalid fare validation must also leave the current version
   published. Repeating publish for the current revision must not change it.

PricingPolicyRetirementTest adds unexecuted source cases for scope union,
retirement metadata preservation, obsolete drafts, invalid fare validation,
idempotent publish and rejecting reactivation of retired revisions. MySQL
concurrency/rollback and HTTP checks remain pending.

Production and all test sources compiled successfully (including the added
regressions) with:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/inventory-service -am -DskipTests test-compile
```

The initial sandboxed attempt could not read a Maven test dependency; the same
command succeeded with approved access outside the sandbox. `git diff --check`
passed. `test-compile` compiles test source but does not execute tests.
