package com.cinema.movie.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "movies")
public class Movie extends BaseEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    @Column(name = "backdrop_url", length = 500)
    private String backdropUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_rating", length = 10)
    private AgeRating ageRating;

    @Column(name = "hero_enabled", nullable = false)
    private boolean heroEnabled;

    @Column(name = "hero_priority", nullable = false)
    private int heroPriority;

    @Column(name = "hero_starts_at")
    private OffsetDateTime heroStartsAt;

    @Column(name = "hero_ends_at")
    private OffsetDateTime heroEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MovieStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "genre_id", nullable = false))
    private Set<Genre> genres = new HashSet<>();

    public Movie() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public void setBackdropUrl(String backdropUrl) {
        this.backdropUrl = backdropUrl;
    }

    public AgeRating getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(AgeRating ageRating) {
        this.ageRating = ageRating;
    }

    public MovieStatus getStatus() {
        return status;
    }

    public boolean isHeroEnabled() {
        return heroEnabled;
    }

    public void setHeroEnabled(boolean heroEnabled) {
        this.heroEnabled = heroEnabled;
    }

    public int getHeroPriority() {
        return heroPriority;
    }

    public void setHeroPriority(int heroPriority) {
        this.heroPriority = heroPriority;
    }

    public OffsetDateTime getHeroStartsAt() {
        return heroStartsAt;
    }

    public void setHeroStartsAt(OffsetDateTime heroStartsAt) {
        this.heroStartsAt = heroStartsAt;
    }

    public OffsetDateTime getHeroEndsAt() {
        return heroEndsAt;
    }

    public void setHeroEndsAt(OffsetDateTime heroEndsAt) {
        this.heroEndsAt = heroEndsAt;
    }

    public void setStatus(MovieStatus status) {
        this.status = status;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres.clear();

        if (genres != null) {
            this.genres.addAll(genres);
        }
    }
}
