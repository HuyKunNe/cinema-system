package com.cinema.common.core.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UuidGeneratorTest {

    @Test
    void shouldGenerateUuidVersion7() {
        UUID uuid = UuidGenerator.next();

        assertNotNull(uuid);
        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
    }

    @Test
    void shouldGenerateUniqueValues() {
        UUID first = UuidGenerator.next();
        UUID second = UuidGenerator.next();

        assertNotEquals(first, second);
    }
}
