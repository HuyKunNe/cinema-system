package com.cinema.common.search.model;

public record SearchHit<T>(

        T content,

        float score

) {

}
