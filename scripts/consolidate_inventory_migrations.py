from pathlib import Path
import re

migration_dir = Path(
    "services/inventory-service/src/main/resources/db/migration"
)

legacy_files = {}

for path in migration_dir.glob("V*__*.sql"):
    match = re.fullmatch(r"V(\d+)__.+\.sql", path.name)

    if match is None:
        raise SystemExit(f"Unsupported migration name: {path.name}")

    version = int(match.group(1))

    if version in legacy_files:
        raise SystemExit(f"Duplicate migration version: V{version}")

    legacy_files[version] = path

if set(legacy_files) != set(range(1, 19)):
    raise SystemExit(
        "Expected exactly the existing Inventory migrations V1 through V18. "
        "Review local changes before consolidating."
    )

groups = [
    ("V1__inventory_core.sql", range(1, 3)),
    ("V2__room_layout.sql", range(3, 8)),
    ("V3__cinema_pricing.sql", range(8, 19)),
]

for output_name, _ in groups:
    output_path = migration_dir / output_name

    if output_path.exists():
        raise SystemExit(f"Output already exists: {output_path}")

# Read and prepare all content before writing or removing source files.
source_sql = {
    version: path.read_text(encoding="utf-8")
    for version, path in legacy_files.items()
}

fare_sql = source_sql[16]

required_tokens = {
    "first_showing_condition": 2,
    "chk_fare_rules_first_showing": 1,
    "'FIRST_SHOWING'": 1,
    "'NOT_FIRST_SHOWING'": 1,
}

for token, expected_count in required_tokens.items():
    if fare_sql.count(token) != expected_count:
        raise SystemExit(
            f"Unexpected V16 content for {token!r}. "
            "No files have been changed."
        )

replacements = [
    ("first_showing_condition", "advance_screening_condition"),
    ("chk_fare_rules_first_showing", "chk_fare_rules_advance_screening"),
    ("'NOT_FIRST_SHOWING'", "'REGULAR_SCREENING'"),
    ("'FIRST_SHOWING'", "'ADVANCE_SCREENING'"),
]

for old, new in replacements:
    fare_sql = fare_sql.replace(old, new)

source_sql[16] = fare_sql

outputs = {}

for output_name, versions in groups:
    sections = [
        "-- Consolidated schema for fresh Inventory databases.\n"
    ]

    for version in versions:
        sections.append(
            f"-- Source: {legacy_files[version].name}\n"
            f"{source_sql[version].strip()}\n"
        )

    outputs[output_name] = "\n".join(sections)

# Exclusive creation prevents overwriting an existing output.
for output_name, content in outputs.items():
    output_path = migration_dir / output_name

    with output_path.open("x", encoding="utf-8", newline="\n") as handle:
        handle.write(content)

# Remove only the eighteen source files already read above.
for path in legacy_files.values():
    path.unlink()

for output_name in outputs:
    print(f"Created {migration_dir / output_name}")

