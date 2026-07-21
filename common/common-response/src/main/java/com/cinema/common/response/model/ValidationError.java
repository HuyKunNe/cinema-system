package com.cinema.common.response.model;

public record ValidationError(

        String field,

        String message

) {
}
