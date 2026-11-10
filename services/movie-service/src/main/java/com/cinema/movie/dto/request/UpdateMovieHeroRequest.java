package com.cinema.movie.dto.request;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.Min;

public class UpdateMovieHeroRequest {

    private Boolean heroEnabled;

    @Min(0)
    private Integer heroPriority;

    private OffsetDateTime heroStartsAt;

    private OffsetDateTime heroEndsAt;

    @JsonIgnore
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
