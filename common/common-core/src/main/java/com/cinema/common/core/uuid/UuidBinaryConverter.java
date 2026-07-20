package com.cinema.common.core.uuid;

import java.nio.ByteBuffer;
import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UuidBinaryConverter
        implements AttributeConverter<UUID, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(UUID uuid) {

        if (uuid == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocate(16);

        buffer.putLong(uuid.getMostSignificantBits());

        buffer.putLong(uuid.getLeastSignificantBits());

        return buffer.array();
    }

    @Override
    public UUID convertToEntityAttribute(byte[] bytes) {

        if (bytes == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        return new UUID(buffer.getLong(), buffer.getLong());
    }

}
