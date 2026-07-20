package com.cinema.common.core.uuid;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {
    }

    public static UUID generate() {

        long timestamp = Instant.now().toEpochMilli();

        long mostSigBits = timestamp << 16;

        long randomPart = RANDOM.nextLong();

        mostSigBits |= (randomPart >>> 48) & 0x0FFF;

        mostSigBits |= 0x7000;

        long leastSigBits = randomPart;

        leastSigBits &= 0x3FFFFFFFFFFFFFFFL;

        leastSigBits |= 0x8000000000000000L;

        return new UUID(mostSigBits, leastSigBits);
    }

}
