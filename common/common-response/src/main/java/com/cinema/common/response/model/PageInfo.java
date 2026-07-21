package com.cinema.common.response.model;

public record PageInfo(

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean first,

        boolean last

) {
}
