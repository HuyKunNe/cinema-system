package com.cinema.movie.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.movie.error.MovieErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "movie_screening_versions")
public class MovieScreeningVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false, updatable = false)
    private Movie movie;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, updatable = false, length = 20)
    private ContentFormat contentFormat;

    @Column(name = "audio_language", nullable = false, updatable = false, length = 100)
    private String audioLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "presentation_type", nullable = false, updatable = false, length = 30)
    private PresentationType presentationType;

    @Column(name = "subtitle_language", nullable = false, updatable = false, length = 100)
    private String subtitleLanguage;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected MovieScreeningVersion() {}

    public MovieScreeningVersion(
            Movie movie,
            ContentFormat contentFormat,
            String audioLanguage,
            PresentationType presentationType,
            String subtitleLanguage) {

        if (movie == null
                || movie.getId() == null
                || contentFormat == null
                || presentationType == null
                || audioLanguage == null) {

            throw invalid();
        }

        String normalizedAudio = audioLanguage.trim();
        String normalizedSubtitle = subtitleLanguage == null ? "" : subtitleLanguage.trim();

        if (normalizedAudio.isEmpty()
                || normalizedAudio.length() > 100
                || normalizedSubtitle.length() > 100) {

            throw invalid();
        }

        if (presentationType == PresentationType.SUBTITLED) {
            if (normalizedSubtitle.isEmpty()) {
                throw invalid();
            }
        } else if (!normalizedSubtitle.isEmpty()) {
            throw invalid();
        }

        this.movie = movie;
        this.contentFormat = contentFormat;
        this.audioLanguage = normalizedAudio;
        this.presentationType = presentationType;
        this.subtitleLanguage = normalizedSubtitle;
    }

    public Movie getMovie() {
        return movie;
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
        return subtitleLanguage.isEmpty() ? null : subtitleLanguage;
    }

    public boolean isActive() {
        return active;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    private static ValidationException invalid() {
        return new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
    }
}
