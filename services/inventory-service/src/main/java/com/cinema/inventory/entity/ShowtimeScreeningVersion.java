package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PresentationType;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.UUID;

@Embeddable
public class ShowtimeScreeningVersion {

    @Column(
            name = "movie_screening_version_id",
            columnDefinition = "BINARY(16)",
            updatable = false)
    private UUID id;

    @Column(name = "movie_screening_version_revision", updatable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", length = 20, updatable = false)
    private ContentFormat contentFormat;

    @Column(name = "audio_language", length = 100, updatable = false)
    private String audioLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "presentation_type", length = 30, updatable = false)
    private PresentationType presentationType;

    @Column(name = "subtitle_language", length = 100, updatable = false)
    private String subtitleLanguage;

    protected ShowtimeScreeningVersion() {}

    public ShowtimeScreeningVersion(
            UUID id,
            Long version,
            ContentFormat contentFormat,
            String audioLanguage,
            PresentationType presentationType,
            String subtitleLanguage) {

        String audio = audioLanguage == null ? "" : audioLanguage.trim();
        String subtitle = subtitleLanguage == null ? "" : subtitleLanguage.trim();

        if (id == null
                || version == null
                || version < 0
                || contentFormat == null
                || presentationType == null
                || audio.isEmpty()
                || audio.length() > 100
                || subtitle.length() > 100) {
            throw invalid();
        }

        if (presentationType == PresentationType.SUBTITLED) {
            if (subtitle.isEmpty()) {
                throw invalid();
            }
        } else if (!subtitle.isEmpty()) {
            throw invalid();
        }

        this.id = id;
        this.version = version;
        this.contentFormat = contentFormat;
        this.audioLanguage = audio;
        this.presentationType = presentationType;
        this.subtitleLanguage = subtitle.isEmpty() ? null : subtitle;
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    public String getAudioLanguage() {
        return audioLanguage;
    }

    public PresentationType getPresentationType() {
        return presentationType;
    }

    public String getSubtitleLanguage() {
        return subtitleLanguage;
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.SCREENING_VERSION_SNAPSHOT_INVALID);
    }
}
