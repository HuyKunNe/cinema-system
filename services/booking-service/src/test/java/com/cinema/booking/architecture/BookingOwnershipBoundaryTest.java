package com.cinema.booking.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

class BookingOwnershipBoundaryTest {

    private static final List<String> SOURCE_EXTENSIONS =
            List.of(".java", ".sql", ".yml", ".yaml", ".properties", ".xml");

    private static final List<String> FORBIDDEN_INVENTORY_REFERENCES =
            List.of("com.cinema.inventory", "showseatrepository", "showseatentity");

    private static final List<String> FORBIDDEN_TABLE_REFERENCES =
            List.of("show_seats", "cinema_inventory_db");

    private final Path moduleRoot =
            Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();

    @Test
    void bookingModuleShouldNotDependOnInventoryService() throws IOException {

        Path pomPath = moduleRoot.resolve("pom.xml");

        assertThat(pomPath).exists().isRegularFile();

        String pom = Files.readString(pomPath).toLowerCase(Locale.ROOT);

        assertThat(pom).doesNotContain("<artifactid>inventory-service</artifactid>");

        assertThat(pom).doesNotContain("<artifactid>inventory_service</artifactid>");
    }

    @Test
    void bookingSourceShouldNotImportInventoryImplementation() throws IOException {

        List<ForbiddenReference> violations =
                findForbiddenReferences(FORBIDDEN_INVENTORY_REFERENCES);

        assertThat(violations)
                .as(
                        """
                        Booking Service must communicate with Inventory through
                        approved event contracts and must not import Inventory
                        implementation classes.
                        """)
                .isEmpty();
    }

    @Test
    void bookingSourceAndMigrationsShouldNotAccessShowSeats() throws IOException {

        List<ForbiddenReference> violations = findForbiddenReferences(FORBIDDEN_TABLE_REFERENCES);

        assertThat(violations)
                .as(
                        """
                        Inventory Service is the sole owner of show_seats.
                        Booking source code, SQL and configuration must not
                        read, write, create or reference Inventory-owned tables.
                        """)
                .isEmpty();
    }

    private List<ForbiddenReference> findForbiddenReferences(List<String> forbiddenValues)
            throws IOException {

        Path mainSourceRoot = moduleRoot.resolve("src/main");

        assertThat(mainSourceRoot).exists().isDirectory();

        try (Stream<Path> paths = Files.walk(mainSourceRoot)) {

            return paths.filter(Files::isRegularFile)
                    .filter(this::isScannableSource)
                    .flatMap(path -> findReferences(path, forbiddenValues).stream())
                    .toList();
        }
    }

    private List<ForbiddenReference> findReferences(Path path, List<String> forbiddenValues) {

        try {
            String content = Files.readString(path).toLowerCase(Locale.ROOT);

            return forbiddenValues.stream()
                    .filter(content::contains)
                    .map(
                            forbiddenValue ->
                                    new ForbiddenReference(
                                            moduleRoot.relativize(path).toString(), forbiddenValue))
                    .toList();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to inspect Booking source file: " + path, exception);
        }
    }

    private boolean isScannableSource(Path path) {

        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);

        return SOURCE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private record ForbiddenReference(String path, String value) {}
}
