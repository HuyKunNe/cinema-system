package com.cinema.common.response.model;

import java.util.List;

public record ErrorBody(

        String code,
        String message,

        String category,

        List<ValidationError> details

) {
}
