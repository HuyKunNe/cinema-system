package com.cinema.common.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.UUID;

public class UuidSerializer extends StdSerializer<UUID> {

    public UuidSerializer() {

        super(UUID.class);

    }

    @Override
    public void serialize(
            UUID value,
            JsonGenerator generator,
            SerializerProvider provider) throws IOException {
        generator.writeString(value.toString());
    }

}
