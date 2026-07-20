package com.cinema.common.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.UUID;

public class UuidDeserializer extends StdDeserializer<UUID> {

    public UuidDeserializer() {

        super(UUID.class);

    }

    @Override
    public UUID deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        return UUID.fromString(parser.getText());

    }

}
