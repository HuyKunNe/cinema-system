# Configurable Pricing Policy Versions

Status: Requirements confirmed; implementation pending.
Confirmed: 2026-10-27.

## Scope

Each pricing policy version owns its time bands and ticket groups.

Time bands and ticket groups are configurable data.
Their identities must not depend on display names or fixed Java enums.

The existing fixed-enum implementation remains the runtime baseline
until the migration and API changes are implemented.

## Time bands

Each time band has:

- a stable identifier within its pricing version;
- an editable display name;
- a start time;
- an end time;
- a display order.

Administrators can add, remove, rename and reorder time bands in a draft.

New versions initially provide the familiar two bands:

- before 17:00;
- from 17:00.

These are initial configuration values, not permanent system limits.

### Matching rules

- Use the screening start in the cinema time zone.
- Include the start boundary and exclude the end boundary.
- Exactly 17:00 belongs to a band starting at 17:00.
- Reject overlapping bands because they would produce ambiguous prices.
- Allow an overnight band to continue through midnight.
- Represent a complete day explicitly as 00:00 to 24:00.
- Reject other equal start/end configurations.
- If no band matches a screening start, no applicable fare exists.
- Never substitute another band's price.

Renaming a band does not change its identity or amounts.

Changing its boundaries changes when its configured fares apply.
This is allowed only in a draft and must be checked before publication.

## Ticket groups

Each ticket group has:

- a stable identifier within its pricing version;
- an editable display name;
- an optional eligibility description;
- a display order;
- an indication of whether it is the default regular-price group.

Administrators can add, remove, rename and reorder groups in a draft.

The system is not limited to the previous four ticket categories.

### Default group

Each saved version has exactly one default regular-price group.

- Its initial display name can be Người lớn.
- Administrators can rename it.
- It has no eligibility condition.
- A booking request that does not identify a group uses this group.
- Removing the default group requires assigning another unconditional group
  as the default in the same saved configuration.
- Never locate the default group by its display name.

This replaces the fixed assumption that the default must be a Java ADULT enum.

### Eligibility descriptions

Eligibility is descriptive text in this implementation.

Examples can describe student identification or another qualification.

The backend must not:

- infer age thresholds from a group's name;
- infer conditions from a legacy enum name;
- claim to have verified eligibility from the description;
- automatically grant a discount because of words in the label.

Renaming a group does not automatically rewrite its eligibility description.

## Fare matrix identity

A fare cell is identified by:

- pricing policy version;
- pricing experience;
- content format;
- time-band identifier;
- ticket-group identifier;
- seat type.

Labels are presentation data, not matrix keys.

Adding a band or group creates additional combinations to configure.
New combinations must not silently inherit prices.

Removing a band or group from a draft must remove its associated draft
fare cells atomically.

Renaming or reordering a band or group preserves its existing amounts.

## Fare states and amounts

Preserve the existing distinction between:

- a configured price, including zero;
- NOT_APPLICABLE;
- an unconfigured combination.

Keep exact final prices.

- Do not add surcharges or multipliers.
- Enter 2D and 3D prices independently.
- Do not derive one format's amounts from another.
- Preserve the agreed couple-seat pricing rules.
- Preserve the existing premium-room category constraints.
- Generalize those validations across the configured groups and bands.

## Publication and revisions

Draft configurations are editable.

Publication freezes:

- metadata and cinema scopes;
- time-band definitions and names;
- ticket-group names and descriptions;
- the default-group selection;
- matrix states and amounts.

Changes after publication require a new draft revision.

Creating a draft from a published version copies the definitions and fares.
Copied cells must reference the copied version's corresponding definitions.

A showtime selects one exact published version.
Publishing another revision does not switch existing showtimes.

## Existing data migration

Preserve existing policy and version identifiers.

For each existing version:

- map BEFORE_17 to the initial 00:00–17:00 definition;
- map FROM_17 to the initial 17:00–24:00 definition;
- migrate its configured ticket categories to ticket-group records;
- map ADULT to the default regular-price group;
- preserve existing display labels as initial editable values;
- preserve every existing amount and non-sale state.

Legacy labels and enum names are migration inputs, not permanent rules.

Do not:

- reprice existing bookings;
- select a newer pricing version automatically;
- infer a pricing version from an old numeric seat price;
- convert an unconfigured cell to zero;
- silently map an unknown group or band to the default.

## API and frontend changes required

The implementation must replace enum-based fare references with identifiers.

API responses must provide enough definition data for FE to render:

- band names and boundaries;
- group names and eligibility descriptions;
- ordering;
- the default group;
- associated fare cells.

FE must render columns and band selectors from those responses.

Generated clients must be regenerated after the new contract is available.

The existing fixed-enum APIs cannot represent arbitrary groups or bands.
A deployment must explicitly handle that contract change.

## Showtime and booking integration

Resolve the matching band from the actual screening start.

Derive the experience and content format from the showtime context.

Confirmed room mapping:

- STANDARD → STANDARD;
- IMAX → IMAX;
- FOUR_DX → FOUR_DX;
- SCREEN_X → SCREEN_X;
- VIP → PREMIUM_CINEMA.

SeatType.VIP remains a separate seat type.

Booking without an explicit group uses the version's configured default group.

Booking and reservation integration must preserve the chosen group,
time band, pricing version and final amount in the pricing provenance.

Existing booking amount snapshots remain unchanged.

## Verification targets

- Arbitrary names and ordering survive save and reload.
- More than two bands and more than four groups are supported.
- Renaming definitions preserves cell amounts.
- Boundary and overnight matching are unambiguous.
- Overlapping bands are rejected.
- A screening outside configured bands has no applicable fare.
- Zero remains a configured price.
- New combinations remain unconfigured until explicitly completed.
- Exactly one unconditional default group exists.
- Published definitions cannot be edited.
- Draft copying preserves correspondence between definitions and cells.
- Migration preserves existing prices and non-sale states.
