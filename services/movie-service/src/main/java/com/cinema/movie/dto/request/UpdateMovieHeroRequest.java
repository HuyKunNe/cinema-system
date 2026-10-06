package com.cinema.movie.dto.request;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public class UpdateMovieHeroRequest {

    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            description = "Omit to preserve the current value. Null is invalid.")
    private Boolean heroEnabled;

    @Min(0)
    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "0",
            description = "Lower values appear first. Omit to preserve. Null is invalid.")
    private Integer heroPriority;

    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            description = "Offset timestamp. Omit to preserve; null removes the lower bound.")
    private OffsetDateTime heroStartsAt;

    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            description = "Offset timestamp. Omit to preserve; null removes the upper bound.")
    private OffsetDateTime heroEndsAt;

    @JsonIgnore
    @Schema(hidden = true)
    private final Set<String> provided = new HashSet<>();

    public Boolean getHeroEnabled() {
        return heroEnabled;
    }

    @JsonSetter("heroEnabled")
    public void setHeroEnabled(Boolean heroEnabled) {
        provided.add("heroEnabled");
        this.heroEnabled = heroEnabled;
    }

    public Integer getHeroPriority() {
        return heroPriority;
    }

    @JsonSetter("heroPriority")
    public void setHeroPriority(Integer heroPriority) {
        provided.add("heroPriority");
        this.heroPriority = heroPriority;
    }

    public OffsetDateTime getHeroStartsAt() {
        return heroStartsAt;
    }

    @JsonSetter("heroStartsAt")
    public void setHeroStartsAt(OffsetDateTime heroStartsAt) {
        provided.add("heroStartsAt");
        this.heroStartsAt = heroStartsAt;
    }

    public OffsetDateTime getHeroEndsAt() {
        return heroEndsAt;
    }

    @JsonSetter("heroEndsAt")
    public void setHeroEndsAt(OffsetDateTime heroEndsAt) {
        provided.add("heroEndsAt");
        this.heroEndsAt = heroEndsAt;
    }

    public boolean hasField(String name) {
        return provided.contains(name);
    }

    public boolean hasChanges() {
        return !provided.isEmpty();
    }
}
