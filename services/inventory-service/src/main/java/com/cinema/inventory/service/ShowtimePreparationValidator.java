package com.cinema.inventory.service;

import com.cinema.common.api.screening.ScreeningFormat;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.client.MovieScreeningClient.MovieSnapshot;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.entity.PricingTicketCategory;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingSeatMappingStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketEligibilityMode;
import com.cinema.inventory.enums.TicketPricingRole;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.FareRuleRepository;
import com.cinema.inventory.repository.PricingSeatTypeRepository;
import com.cinema.inventory.repository.PricingTicketCategoryRepository;
import com.cinema.inventory.service.pricing.DraftSaleScopeResolver;
import com.cinema.inventory.service.pricing.FareRuleEvaluationContext;
import com.cinema.inventory.service.pricing.FareRuleSelector;
import com.cinema.inventory.service.pricing.PricingPolicyPeriod;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ShowtimePreparationValidator {

    private final PricingTicketCategoryRepository categoryRepository;
    private final PricingSeatTypeRepository seatTypeRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final FareRuleRepository fareRepository;
    private final DraftSaleScopeResolver scopeResolver;
    private final FareRuleSelector selector;

    public ShowtimePreparationValidator(
            PricingTicketCategoryRepository categoryRepository,
            PricingSeatTypeRepository seatTypeRepository,
            CinemaHolidayDateRepository holidayRepository,
            FareRuleRepository fareRepository,
            DraftSaleScopeResolver scopeResolver,
            FareRuleSelector selector) {

        this.categoryRepository = categoryRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.holidayRepository = holidayRepository;
        this.fareRepository = fareRepository;
        this.scopeResolver = scopeResolver;
        this.selector = selector;
    }

    public void requireScreening(
            Room room,
            MovieSnapshot movie,
            ExperienceType experienceType,
            ContentFormat contentFormat) {

        ScreeningFormat format;

        try {
            String content =
                    switch (contentFormat) {
                        case TWO_D -> "2D";
                        case THREE_D -> "3D";
                    };

            format = ScreeningFormat.valueOf(experienceType.name() + "_" + content);
        } catch (IllegalArgumentException exception) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_FORMAT_NOT_SUPPORTED);
        }

        if (!"UPCOMING".equals(movie.status()) && !"NOW_SHOWING".equals(movie.status())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_MOVIE_NOT_SCHEDULABLE);
        }

        if (movie.releaseDate() == null) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_MOVIE_RELEASE_DATE_REQUIRED);
        }

        boolean movieSupports =
                movie.configuredFormats().stream().anyMatch(option -> option.code() == format);

        if (!room.getScreeningFormats().contains(format) || !movieSupports) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_FORMAT_NOT_SUPPORTED);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requirePolicyCoverage(
            CinemaPricingPolicy policy,
            ExperienceType experienceType,
            ContentFormat contentFormat,
            LocalDate officialReleaseDate,
            OffsetDateTime startsAt,
            List<LayoutSeatSnapshot> seats) {

        policy.requirePublishedConfiguration();

        if (!PricingPolicyPeriod.from(policy).contains(startsAt.toInstant())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_NOT_EFFECTIVE);
        }

        if (seats.isEmpty()) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_INCOMPLETE);
        }

        List<PricingTicketCategory> regularCategories =
                categoryRepository.findAllByPolicy_IdOrderByCodeAsc(policy.getId()).stream()
                        .filter(category -> category.getPricingRole() == TicketPricingRole.REGULAR)
                        .toList();

        if (regularCategories.size() != 1
                || regularCategories.getFirst().getEligibilityMode()
                        != TicketEligibilityMode.UNRESTRICTED) {
            throw invalidBinding();
        }

        String regularCategoryCode = regularCategories.getFirst().getCode();

        OffsetDateTime localStartsAt =
                startsAt.toInstant().atZone(ZoneId.of(policy.getTimeZone())).toOffsetDateTime();

        boolean holiday =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(policy.getId()).stream()
                        .anyMatch(
                                entry ->
                                        entry.getHolidayDate().equals(localStartsAt.toLocalDate()));

        List<PricingSeatType> catalog =
                seatTypeRepository.findAllByPolicy_IdOrderByCodeAsc(policy.getId());

        var fares = fareRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(policy.getId());

        Set<SeatType> physicalTypes =
                seats.stream().map(LayoutSeatSnapshot::seatType).collect(Collectors.toSet());

        for (SeatType physicalType : physicalTypes) {
            List<PricingSeatType> mappings =
                    catalog.stream()
                            .filter(
                                    entry ->
                                            entry.getMappingStatus()
                                                    == PricingSeatMappingStatus.CONFIRMED)
                            .filter(entry -> entry.getPhysicalSeatType() == physicalType)
                            .toList();

            if (mappings.size() != 1) {
                throw invalidBinding();
            }

            FareRuleEvaluationContext context =
                    new FareRuleEvaluationContext(
                            policy.getId(),
                            localStartsAt,
                            holiday,
                            experienceType,
                            contentFormat,
                            regularCategoryCode,
                            mappings.getFirst().getCode(),
                            officialReleaseDate,
                            Set.of());

            if (!scopeResolver.resolve(context).allowsMinute(context.minuteOfDay())) {
                throw fareUnavailable();
            }

            var selection = selector.select(fares, context);

            if (selection.status() != FareRuleSelectionStatus.MATCHED) {
                throw fareUnavailable();
            }

            FareUnit expectedUnit =
                    physicalType == SeatType.COUPLE ? FareUnit.PER_PAIR : FareUnit.PER_TICKET;

            if (selection.winners().getFirst().getFareUnit() != expectedUnit) {
                throw fareUnavailable();
            }
        }
    }

    private static ConflictException invalidBinding() {
        return new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_BINDING_INVALID);
    }

    private static ConflictException fareUnavailable() {
        return new ConflictException(InventoryErrorCode.SHOWTIME_FARE_NOT_AVAILABLE);
    }
}
