package com.cinema.inventory.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ExperienceType {
    STANDARD,
    PREMIUM_CINEMA,

    @JsonProperty("4DX")
    FOUR_DX,

    IMAX
}
