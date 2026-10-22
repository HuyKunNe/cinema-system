package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ShowtimeResponse(
        UUID id,
        UUID movieId,
        UUID roomId,
        String roomName,
        UUID cinemaId,
        String cinemaName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        ShowtimeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID roomLayoutId,
        Long roomLayoutVersion,
        UUID pricingPolicyId,
        Long pricingPolicyVersion,
        ExperienceType experienceType,
        ContentFormat contentFormat,
        LocalDate officialReleaseDate,
        Long movieVersion,
        Boolean advanceScreening) {}
