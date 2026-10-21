package com.cinema.inventory.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ContentFormat {

    @JsonProperty("2D")
    TWO_D,

    @JsonProperty("3D")
    THREE_D
}
