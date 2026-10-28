import argparse
import os
import re
import secrets
import shutil
import subprocess
import sys
import time
import difflib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

SERVICES = (
    "user-service",
    "movie-service",
    "inventory-service",
    "booking-service",
    "payment-service",
)

VERSIONED_SQL = re.compile(r"^V(\d+)__.+\.sql$")

BASELINE_NAME = "V1__baseline_schema.sql"
SEED_NAME = "V2__seed_authorities.sql"

AUTHORITY_QUERIES = {
    "roles": (
        "SELECT HEX(id), name, description, version "
        "FROM roles ORDER BY id"
    ),
    "permissions": (
        "SELECT HEX(id), code, description, version "
        "FROM permissions ORDER BY id"
    ),
    "role_permissions": (
        "SELECT HEX(role_id), HEX(permission_id), "
        "HEX(assigned_by_user_id) "
        "FROM role_permissions ORDER BY role_id, permission_id"
    ),
}


def run(command, sql=None):
    result = subprocess.run(
        command,
        input=sql,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )

    if result.returncode != 0:
        raise RuntimeError(
            result.stderr.strip() or "Command failed"
        )

    return result.stdout


def quote(identifier):
    return "`" + identifier.replace("`", "``") + "`"


def migration_directory(service):
    return (
        ROOT
        / "services"
        / service
        / "src/main/resources/db/migration"
    )


def migration_files(service):
    directory = migration_directory(service)

    if not directory.is_dir():
        raise RuntimeError(f"Missing migration directory: {directory}")

    files = []

    for path in directory.glob("*.sql"):
        match = VERSIONED_SQL.fullmatch(path.name)

        if match is None:
            raise RuntimeError(f"Unsupported migration filename: {path}")

        files.append((int(match.group(1)), path))

    if not files:
        raise RuntimeError(f"No migrations found: {directory}")

    versions = [version for version, path in files]

    if len(versions) != len(set(versions)):
        raise RuntimeError(f"Duplicate migration versions: {service}")

    if any(path.name == BASELINE_NAME for version, path in files):
        raise RuntimeError(f"Already consolidated: {service}")

    return [path for version, path in sorted(files)]


class TemporaryMySQL:
    def __init__(self):
        self.name = "cinema-migration-squash-" + secrets.token_hex(6)
        self.password = secrets.token_hex(24)

    def start(self):
        run(
            [
                "docker",
                "run",
                "--detach",
                "--rm",
                "--network",
                "none",
                "--name",
                self.name,
                "--env",
                "MYSQL_ROOT_PASSWORD=" + self.password,
                "mysql:8.4.0",
            ]
        )

        deadline = time.monotonic() + 120

        while time.monotonic() < deadline:
            try:
                self.sql(None, "SELECT 1;")
                return
            except RuntimeError:
                time.sleep(1)

        raise RuntimeError("Temporary MySQL did not become ready")

    def command(self, executable):
        return [
            "docker",
            "exec",
            "--interactive",
            "--env",
            "MYSQL_PWD=" + self.password,
            self.name,
            executable,
            "--user=root",
            "--protocol=tcp",
            "--host=127.0.0.1",
            "--default-character-set=utf8mb4",
        ]

    def sql(self, database, sql):
        command = self.command("mysql") + [
            "--batch",
            "--skip-column-names",
        ]

        if database is not None:
            command.append("--database=" + database)

        return run(command, sql)

    def create_database(self, database):
        self.sql(
            None,
            f"CREATE DATABASE {quote(database)} "
            "CHARACTER SET utf8mb4 "
            "COLLATE utf8mb4_0900_ai_ci;",
        )

    def schema(self, database):
        command = self.command("mysqldump") + [
            "--no-data",
            "--skip-add-drop-table",
            "--skip-comments",
            "--skip-dump-date",
            "--skip-lock-tables",
            "--no-tablespaces",
            "--set-gtid-purged=OFF",
            "--column-statistics=0",
            database,
        ]

        return run(command).strip() + "\n"

    def data_snapshot(self, database, service):
        tables = self.sql(
            database,
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_type = 'BASE TABLE'
            ORDER BY table_name;
            """,
        ).splitlines()

        snapshot = {}

        for table in tables:
            count = int(
                self.sql(
                    database,
                    f"SELECT COUNT(*) FROM {quote(table)};",
                ).strip()
            )

            rows = ""

            if count:
                if (
                    service != "user-service"
                    or table not in AUTHORITY_QUERIES
                ):
                    raise RuntimeError(
                        "Unexpected seeded data; preserve it explicitly "
                        f"before consolidation: {service}.{table}"
                    )

                rows = self.sql(
                    database,
                    AUTHORITY_QUERIES[table],
                )

            snapshot[table] = (count, rows)

        return snapshot

    def close(self):
        subprocess.run(
            [
                "docker",
                "rm",
                "--force",
                "--volumes",
                self.name,
            ],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=False,
        )


def replace_migrations(selected, source_files, originals, outputs):
    for service in selected:
        if migration_files(service) != source_files[service]:
            raise RuntimeError(
                f"Migration files changed during generation: {service}"
            )

    for path, content in originals.items():
        if path.read_bytes() != content:
            raise RuntimeError(
                f"Migration content changed during generation: {path}"
            )

    temporary_files = []

    try:
        for path, content in outputs.items():
            temporary = path.with_name(path.name + ".tmp")
            temporary_files.append(temporary)
            temporary.write_bytes(content)
            os.replace(temporary, path)

        for path in originals:
            if path not in outputs:
                path.unlink()

    except BaseException:
        for path in outputs:
            if path not in originals:
                path.unlink(missing_ok=True)

        for path, content in originals.items():
            path.write_bytes(content)

        raise

    finally:
        for path in temporary_files:
            path.unlink(missing_ok=True)

def normalize_schema_for_comparison(schema):
    # COLLATE utf8mb4_unicode_ci already implies CHARACTER SET utf8mb4.
    # Match only VARCHAR column declarations at the beginning of a line.
    return re.sub(
        r"(?m)^([ \t]+`(?:``|[^`\r\n])+`[ \t]+varchar\([0-9]+\))"
        r" CHARACTER SET utf8mb4"
        r"(?= COLLATE utf8mb4_unicode_ci(?:[ \t]|$))",
        r"\1",
        schema,
    )


def main():
    parser = argparse.ArgumentParser(
        description="Squash Flyway migrations for fresh databases."
    )
    parser.add_argument(
        "--services",
        nargs="+",
        choices=SERVICES,
        default=list(SERVICES),
    )
    args = parser.parse_args()

    if not (ROOT / "pom.xml").is_file():
        raise RuntimeError("Run this script from the cinema-system repository")

    if shutil.which("docker") is None:
        raise RuntimeError("Docker CLI is required")

    selected = list(dict.fromkeys(args.services))

    source_files = {
        service: migration_files(service)
        for service in selected
    }

    originals = {
        path: path.read_bytes()
        for files in source_files.values()
        for path in files
    }

    for path, content in originals.items():
        text = content.decode("utf-8-sig")

        if re.search(r"\$\{[^}]+\}", text):
            raise RuntimeError(
                f"Flyway placeholders require explicit handling: {path}"
            )

    outputs = {}
    server = TemporaryMySQL()

    try:
        print("Starting isolated MySQL 8.4.0", flush=True)
        server.start()

        for service in selected:
            print(f"Consolidating {service}", flush=True)

            source_database = service.replace("-", "_") + "_source"
            check_database = service.replace("-", "_") + "_check"

            server.create_database(source_database)

            for path in source_files[service]:
                server.sql(
                    source_database,
                    originals[path].decode("utf-8-sig"),
                )

            schema = server.schema(source_database)

            source_data = server.data_snapshot(
                source_database,
                service,
            )

            seed = None

            if service == "user-service":
                seed_path = (
                    migration_directory(service)
                    / "V3__seed_initial_authorities.sql"
                )

                if seed_path not in originals:
                    raise RuntimeError(
                        "Original authority seed migration was not found"
                    )

                seed = originals[seed_path]

            server.create_database(check_database)
            server.sql(check_database, schema)

            if seed is not None:
                server.sql(
                    check_database,
                    seed.decode("utf-8-sig"),
                )

            restored_schema = server.schema(check_database)

            expected_schema = normalize_schema_for_comparison(schema)
            actual_schema = normalize_schema_for_comparison(
                restored_schema
            )

            if actual_schema != expected_schema:
                schema_diff = "".join(
                    difflib.unified_diff(
                        expected_schema.splitlines(keepends=True),
                        actual_schema.splitlines(keepends=True),
                        fromfile=f"{service}/original-schema.sql",
                        tofile=f"{service}/restored-schema.sql",
                        n=5,
                    )
                )

                raise RuntimeError(
                    f"Schema comparison failed: {service}\n\n"
                    f"{schema_diff}"
                )

            check_data = server.data_snapshot(
                check_database,
                service,
            )

            if check_data != source_data:
                raise RuntimeError(
                    f"Seed comparison failed: {service}"
                )

            header = (
                "-- Fresh-database schema baseline.\n"
                "-- Consolidated from the previous versioned migrations.\n\n"
            )

            directory = migration_directory(service)

            outputs[directory / BASELINE_NAME] = (
                header + schema
            ).encode("utf-8")

            if seed is not None:
                outputs[directory / SEED_NAME] = seed

            print(
                f"Verified {service}: "
                f"{len(source_files[service])} old migrations",
                flush=True,
            )

    finally:
        server.close()

    replace_migrations(
        selected,
        source_files,
        originals,
        outputs,
    )

    for service in selected:
        count = 2 if service == "user-service" else 1
        print(f"Updated {service}: {count} migration files")

    print("Review git diff before committing.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, UnicodeError) as error:
        print(str(error), file=sys.stderr)
        raise SystemExit(1)
