package com.cinema.common.lock.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class LockKeyGeneratorTest {

    @Test
    void shouldGenerateLockKey() {

        String result = LockKeyGenerator.generate(
                "seat:showtime:1:A1");

        assertThat(result)
                .isEqualTo(
                        "lock:seat:showtime:1:A1");
    }

}
