# Draft deletion — Inventory API

Source increment based on checkout `51133a4`. This contract adds draft deletion
without removing published data or changing matrix/pricing semantics. No Swagger
annotations are used. FE was inspected read only and has not been integrated.

## HTTP contract

Both operations require `inventory:manage` at Inventory. Existing Gateway policy
requires that authority for pricing and authentication for layout routes; Inventory
independently enforces the layout authority. No new scope or role is introduced.

| Operation | Route | Success |
| --- | --- | --- |
| `deleteRoomLayoutDraft` | `DELETE /api/v1/room-layouts/{layoutId}?expectedVersion={version}` | `204`, no body |
| `deleteCinemaPricingPolicyDraft` | `DELETE /api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/versions/{versionId}?expectedVersion={version}` | `204`, no body |

`expectedVersion` is a required, nonnegative integer query parameter. Use the
optimistic `version` from the most recent resource response, not `layoutVersion`
or pricing `revision`. DELETE has no request body. For example:

```http
DELETE /api/v1/room-layouts/019a5a00-0000-7000-8000-000000000005?expectedVersion=7
```

The pricing operation deletes the selected shared version for **all cinemas in
its scope**. The URL's cinema must belong to that version and the policy/version
IDs must match; it does not remove only one cinema from a pricing scope.

| Condition | Result |
| --- | --- |
| No authentication / missing permission | `401` / `403` |
| Missing, malformed or negative expectedVersion | `400` |
| Missing resource, wrong pricing scope or already deleted draft | `404` |
| Existing layout is not DRAFT | `409 INVENTORY_ROOM_LAYOUT_NOT_DRAFT` |
| Existing pricing version is not DRAFT | `409 INVENTORY_PRICING_POLICY_NOT_DRAFT` |
| Stale layout version | `409 INVENTORY_ROOM_LAYOUT_VERSION_CONFLICT` with `error.currentVersion` |
| Stale pricing version | `409 INVENTORY_PRICING_POLICY_VERSION_CONFLICT` with `error.currentVersion` |
| Showtime references the draft layout/version | `409 INVENTORY_ROOM_LAYOUT_IN_USE` / `INVENTORY_PRICING_POLICY_VERSION_IN_USE` |

State validation precedes version validation. A published resource with an old
token therefore remains a not-draft conflict. Never automatically retry deletion
using `currentVersion`: reload the resource and ask the user to review its latest
state. A repeated DELETE after success returns `404`, with no further mutation.

## Persistence and concurrency

- Deletion sets a boolean `deleted` flag on the DRAFT resource. No physical parent,
  fare cell, time band, ticket group, scope, geometry, matrix cell or Seat is deleted.
  Status enums and successful response DTOs are unchanged.
- Layout deletion holds the layout's existing `PESSIMISTIC_WRITE` lock. Pricing
  deletion locks policy then version, matching existing pricing write/publication
  ordering. The flag change and optimistic-version increment commit together.
- Publish/edit/delete contenders serialize on these same rows. Whichever operation
  acquires the lock later validates the current state/version again. Deleted
  resources cannot be edited or published. Showtime assignment already requires
  published resources; an additional reference check rejects inconsistent draft
  references without altering the referencing showtime.
- Layout lists and content/metadata reads hide deleted drafts. Pricing latest,
  version-history, individual-version and fare-matrix reads hide deleted drafts.
  Latest pricing selection falls back to the highest remaining visible revision.
- Revision allocation still includes deleted rows, so identifiers and revision
  numbers are not reused. Pricing creation fingerprints and clientRequestId rows
  remain intact. Replaying an original create/copy key whose target draft was
  deleted returns `404`; it does not recreate the draft or return deleted content.
  Use a new clientRequestId to intentionally create a new draft.
- Published/retired pricing, published layouts, existing showtimes, Booking and
  Payment snapshots, Kafka events and prices are unchanged. No deletion event is
  needed because DRAFT resources are not published to those workflows.

## MySQL rollout and rollback

Apply Inventory migration `V3__add_draft_deletion_flags.sql` before deploying code
that reads the flags. It adds `BIT(1) NOT NULL DEFAULT b'0'` columns to
`room_layouts` and `cinema_pricing_policy_versions`; existing rows remain visible.
V1/V2, existing data types, JDBC/datasource configuration, foreign keys, unique
keys and indexes are unchanged. Existing owner/ID/revision indexes remain usable;
the flag is an additional read predicate, not a replacement for those indexes.

For a rolling deployment, enable deletion in FE only after all Inventory replicas
use the new code. Old replicas ignore the flags and could expose deleted drafts.
There is no destructive rollback script. Before rolling back to old code or
dropping flags, reconcile any rows already marked deleted; otherwise they become
visible again. Keep the columns/data during an application rollback investigation.
MySQL DDL is not an atomic multi-statement transaction: if migration execution
fails partway, inspect both tables and Flyway state before repair/retry. No
migration or database operation was executed by this increment.

## FE integration and manual review

Generate/adjust the Inventory client after exporting the new runtime spec. Show
delete only for DRAFT, pass its current `version`, confirm the selected resource,
then refresh layout lists or pricing latest/history after `204`. For pricing,
make the shared-cinema scope clear in confirmation. FE's current error normalizer
does not retain `currentVersion`; code-based conflict/reload handling remains valid.

Optional manual review in a development environment, using disposable existing
drafts: delete with a current version, then check GET/list returns no deleted
draft; repeat DELETE and confirm `404`. Compare stale-version, wrong-scope and
published-resource responses. Verify anonymous/underprivileged calls are denied
and a fresh draft receives a higher revision. These checks have not been run.

Service and MVC/security regression sources were added. They are not evidence of
runtime correctness until executed; no tests or database commands were run.

Compile-only verification completed successfully for Inventory and its reactor
dependencies with the existing offline Maven cache:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/inventory-service -am -DskipTests compile
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/inventory-service -am -DskipTests test-compile
```

Both commands returned BUILD SUCCESS. The second compiles test sources only;
neither command invokes a test runner or starts the application/Flyway. The first
sandboxed attempt was blocked by Java's access to a cached JAR; the successful
commands ran with approved access. Static deletion-path checks and
`git diff --check` also passed. HTTP, JPQL execution, migration and real concurrency
behavior remain unverified.
