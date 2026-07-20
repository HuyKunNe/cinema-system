package com.cinema.common.jackson.module;

import com.cinema.common.jackson.serializer.UuidSerializer;
import com.cinema.common.jackson.deserializer.UuidDeserializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.UUID;

public class CinemaJacksonModule extends SimpleModule {

    public CinemaJacksonModule() {

        super("cinema-jackson-module");

        addSerializer(UUID.class, new UuidSerializer());

        addDeserializer(UUID.class, new UuidDeserializer());

    }

}
