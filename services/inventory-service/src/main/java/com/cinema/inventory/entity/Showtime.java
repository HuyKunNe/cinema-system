package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.service.pricing.PricingPolicyPeriod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "showtimes")
public class Showtime extends BaseEntity {

    @Column(name = "movie_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID movieId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "room_layout_id", nullable = true, updatable = false)
    private RoomLayout roomLayout;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShowtimeStatus status = ShowtimeStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_policy_id", updatable = false)
    private CinemaPricingPolicy pricingPolicy;

    @Column(name = "pricing_policy_version", updatable = false)
    private Long pricingPolicyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_type", length = 30, updatable = false)
    private ExperienceType experienceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", length = 10, updatable = false)
    private ContentFormat contentFormat;

    @Column(name = "official_release_date", updatable = false)
    private LocalDate officialReleaseDate;

    @Column(name = "movie_version", updatable = false)
    private Long movieVersion;

    @Column(name = "room_layout_version", updatable = false)
    private Long roomLayoutVersion;

    @OneToMany(mappedBy = "showtime", fetch = FetchType.LAZY)
    private Set<ShowSeat> showSeats = new HashSet<>();

    protected Showtime() {}

    public Showtime(UUID movieId, Room room, OffsetDateTime startsAt, OffsetDateTime endsAt) {

        validateTimeRange(startsAt, endsAt);

        this.movieId = movieId;
        this.room = room;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = ShowtimeStatus.SCHEDULED;
    }

    public Showtime(
            UUID movieId,
            Room room,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            RoomLayout roomLayout) {

        this(movieId, room, startsAt, endsAt);

        if (roomLayout != null) {
            if (roomLayout.getId() == null) {
                throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
            }

            if (room == null || !room.getId().equals(roomLayout.getRoom().getId())) {
                throw new ValidationException(InventoryErrorCode.SHOWTIME_LAYOUT_ROOM_MISMATCH);
            }

            if (roomLayout.getStatus() != RoomLayoutStatus.PUBLISHED) {
                throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_NOT_PUBLISHED);
            }
        }

        this.roomLayout = roomLayout;
    }

    public static Showtime withPricingPolicy(
            UUID movieId,
            Room room,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            RoomLayout layout,
            CinemaPricingPolicy policy,
            ExperienceType experienceType,
            ContentFormat contentFormat,
            LocalDate officialReleaseDate,
            Long movieVersion) {

        if (movieId == null
                || room == null
                || layout == null
                || policy == null
                || experienceType == null
                || contentFormat == null
                || officialReleaseDate == null
                || movieVersion == null
                || movieVersion < 0) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_BINDING_INVALID);
        }

        policy.requirePublishedConfiguration();

        if (!room.getCinema().getId().equals(policy.getCinema().getId())
                || !PricingPolicyPeriod.from(policy).contains(startsAt.toInstant())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_BINDING_INVALID);
        }

        Showtime showtime = new Showtime(movieId, room, startsAt, endsAt, layout);

        showtime.pricingPolicy = policy;
        showtime.pricingPolicyVersion = policy.getPolicyVersion();
        showtime.experienceType = experienceType;
        showtime.contentFormat = contentFormat;
        showtime.officialReleaseDate = officialReleaseDate;
        showtime.movieVersion = movieVersion;
        showtime.roomLayoutVersion = layout.getLayoutVersion();

        return showtime;
    }

    public boolean usesPolicyPricing() {
        return pricingPolicy != null;
    }

    public CinemaPricingPolicy getPricingPolicy() {
        return pricingPolicy;
    }

    public Long getPricingPolicyVersion() {
        return pricingPolicyVersion;
    }

    public ExperienceType getExperienceType() {
        return experienceType;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    public LocalDate getOfficialReleaseDate() {
        return officialReleaseDate;
    }

    public Long getMovieVersion() {
        return movieVersion;
    }

    public Long getRoomLayoutVersion() {
        return roomLayoutVersion;
    }

    public Boolean getAdvanceScreening() {
        if (!usesPolicyPricing()) {
            return null;
        }

        LocalDate localDate =
                startsAt.toInstant().atZone(ZoneId.of(pricingPolicy.getTimeZone())).toLocalDate();

        return localDate.isBefore(officialReleaseDate);
    }

    public UUID getMovieId() {
        return movieId;
    }

    public void setMovieId(UUID movieId) {

        if (usesPolicyPricing() && !java.util.Objects.equals(this.movieId, movieId)) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }

        this.movieId = movieId;
    }

    public Room getRoom() {
        return room;
    }

    public RoomLayout getRoomLayout() {
        return roomLayout;
    }

    void assignRoom(Room room) {
        if (usesPolicyPricing() && (room == null || !this.room.getId().equals(room.getId()))) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }
        this.room = room;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public ShowtimeStatus getStatus() {
        return status;
    }

    public Set<ShowSeat> getShowSeats() {
        return Set.copyOf(showSeats);
    }

    public void changeSchedule(OffsetDateTime startsAt, OffsetDateTime endsAt) {

        validateTimeRange(startsAt, endsAt);

        if (usesPolicyPricing()) {
            requireStatus(ShowtimeStatus.SCHEDULED);

            if (!PricingPolicyPeriod.from(pricingPolicy).contains(startsAt.toInstant())) {
                throw new ConflictException(InventoryErrorCode.PRICING_POLICY_NOT_EFFECTIVE);
            }
        }

        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void openForBooking() {
        requireStatus(ShowtimeStatus.SCHEDULED);

        if (usesPolicyPricing()) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_BOOKING_NOT_READY);
        }

        this.status = ShowtimeStatus.OPEN_FOR_BOOKING;
    }

    public void openPolicyForBooking(OffsetDateTime now) {
        requireStatus(ShowtimeStatus.SCHEDULED);

        if (!usesPolicyPricing()
                || roomLayout == null
                || experienceType == null
                || contentFormat == null
                || officialReleaseDate == null
                || movieVersion == null
                || movieVersion < 0
                || pricingPolicyVersion == null
                || pricingPolicyVersion.longValue() != pricingPolicy.getPolicyVersion()
                || roomLayoutVersion == null
                || roomLayoutVersion.longValue() != roomLayout.getLayoutVersion()
                || roomLayout.getStatus() != RoomLayoutStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_BINDING_INVALID);
        }

        pricingPolicy.requirePublishedConfiguration();

        if (now == null || !startsAt.isAfter(now)) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_BOOKABLE);
        }

        if (!PricingPolicyPeriod.from(pricingPolicy).contains(startsAt.toInstant())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_NOT_EFFECTIVE);
        }

        status = ShowtimeStatus.OPEN_FOR_BOOKING;
    }

    public void close() {
        requireStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        this.status = ShowtimeStatus.CLOSED;
    }

    public void cancel() {
        if (status == ShowtimeStatus.COMPLETED) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }

        this.status = ShowtimeStatus.CANCELLED;
    }

    public void complete() {
        if (status != ShowtimeStatus.CLOSED) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }

        this.status = ShowtimeStatus.COMPLETED;
    }

    public void addShowSeat(ShowSeat showSeat) {
        if (showSeat == null) {
            return;
        }

        showSeats.add(showSeat);
        showSeat.assignShowtime(this);
    }

    private void requireStatus(ShowtimeStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }
    }

    private static void validateTimeRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {

        if (startsAt == null || endsAt == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_PERIOD_REQUIRED);
        }

        if (!endsAt.isAfter(startsAt)) {
            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_PERIOD);
        }
    }
}
