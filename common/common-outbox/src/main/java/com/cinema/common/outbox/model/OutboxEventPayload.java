package com.cinema.common.outbox.model;

import java.util.Map;

public record OutboxEventPayload(

        Map<String, Object> data

) {

}
