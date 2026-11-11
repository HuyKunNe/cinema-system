package com.cinema.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.dto.request.PublishPricingPolicyRequest;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.dto.response.FareMatrixValidationResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.CinemaRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class PricingPolicyRetirementTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-11T02:00:00Z");
    private final CinemaRepository cinemas = mock(CinemaRepository.class);
    private final CinemaPricingPolicyRepository policies = mock(CinemaPricingPolicyRepository.class);
    private final CinemaPricingPolicyVersionRepository versions = mock(CinemaPricingPolicyVersionRepository.class);
    private final FareMatrixService matrices = mock(FareMatrixService.class);
    private final DraftFareMatrixValidationService validation = mock(DraftFareMatrixValidationService.class);
    private final PricingPolicyPublicationService service = new PricingPolicyPublicationService(
            cinemas, policies, versions, matrices, validation,
            mock(CinemaPricingPolicyMapper.class), Clock.fixed(NOW.toInstant(), NOW.getOffset()));
    private final Cinema firstCinema = cinema();
    private final Cinema secondCinema = cinema();
    private final CinemaPricingPolicy policy = policy();

    @Test
    void replacementRetiresAllPublishedRevisionsOfThePolicyAndPreservesTheirMetadata() {
        var first = published(1, Set.of(firstCinema));
        var second = published(2, Set.of(secondCinema));
        var replacement = revision(3, Set.of(firstCinema, secondCinema));
        prepare(replacement, List.of(first, second));
        matrixValidation(replacement, true);

        publish(replacement);

        assertEquals(PricingPolicyVersionStatus.PUBLISHED, replacement.getStatus());
        assertEquals(PricingPolicyVersionStatus.RETIRED, first.getStatus());
        assertEquals(PricingPolicyVersionStatus.RETIRED, second.getStatus());
        assertEquals(NOW.minusDays(1), first.getPublishedAt());
        assertEquals("original-actor", first.getPublishedBy());
        assertEquals(Set.of(secondCinema.getId()), second.getCinemaIds());
        var order = inOrder(policies, versions);
        order.verify(policies).findByIdForUpdate(policy.getId());
        order.verify(versions).findForUpdate(policy.getId(), replacement.getId());
        order.verify(versions).findPublishedForUpdate(policy.getId());
        order.verify(versions).flush();
    }

    @Test
    void droppingAnyPublishedCinemaIsRejectedBeforeFareValidationOrRetirement() {
        var first = published(1, Set.of(firstCinema));
        var second = published(2, Set.of(secondCinema));
        var replacement = revision(3, Set.of(firstCinema));
        prepare(replacement, List.of(first, second));

        var error = assertThrows(ConflictException.class, () -> publish(replacement));

        assertEquals(InventoryErrorCode.PRICING_POLICY_PUBLICATION_SCOPE_CONFLICT, error.getErrorCode());
        assertEquals(PricingPolicyVersionStatus.PUBLISHED, first.getStatus());
        assertEquals(PricingPolicyVersionStatus.PUBLISHED, second.getStatus());
        assertEquals(PricingPolicyVersionStatus.DRAFT, replacement.getStatus());
        verifyNoInteractions(matrices, validation);
        verify(versions, never()).flush();
    }

    @Test
    void obsoleteDraftCannotReplaceANewerPublishedRevision() {
        var current = published(3, Set.of(firstCinema));
        var obsolete = revision(2, Set.of(firstCinema));
        prepare(obsolete, List.of(current));

        var error = assertThrows(ConflictException.class, () -> publish(obsolete));

        assertEquals(InventoryErrorCode.PRICING_POLICY_PUBLICATION_SUPERSEDED, error.getErrorCode());
        assertEquals(PricingPolicyVersionStatus.PUBLISHED, current.getStatus());
        verifyNoInteractions(matrices, validation);
        verify(versions, never()).flush();
    }

    @Test
    void invalidFareMatrixDoesNotRetireCurrentRevision() {
        var current = published(1, Set.of(firstCinema));
        var replacement = revision(2, Set.of(firstCinema));
        prepare(replacement, List.of(current));
        matrixValidation(replacement, false);

        var error = assertThrows(ConflictException.class, () -> publish(replacement));

        assertEquals(InventoryErrorCode.PRICING_POLICY_NOT_PUBLISHABLE, error.getErrorCode());
        assertEquals(PricingPolicyVersionStatus.PUBLISHED, current.getStatus());
        assertEquals(PricingPolicyVersionStatus.DRAFT, replacement.getStatus());
        verify(versions, never()).flush();
    }

    @Test
    void replayOfPublishedVersionDoesNotRepeatRetirement() {
        var current = published(2, Set.of(firstCinema));
        prepare(current, List.of());

        publish(current);

        assertEquals(PricingPolicyVersionStatus.PUBLISHED, current.getStatus());
        verify(versions, never()).findPublishedForUpdate(policy.getId());
        verifyNoInteractions(matrices, validation);
        verify(versions, never()).flush();
    }

    @Test
    void retiredVersionCannotBeRepublishedOverTheCurrentRevision() {
        var old = published(1, Set.of(firstCinema));
        old.retire();
        prepare(old, List.of());

        var error = assertThrows(ConflictException.class, () -> publish(old));

        assertEquals(InventoryErrorCode.PRICING_POLICY_NOT_DRAFT, error.getErrorCode());
        assertEquals(PricingPolicyVersionStatus.RETIRED, old.getStatus());
        verifyNoInteractions(matrices, validation);
        verify(versions, never()).flush();
    }

    private void publish(CinemaPricingPolicyVersion target) {
        service.publish(firstCinema.getId(), policy.getId(), target.getId(),
                new PublishPricingPolicyRequest(0L), "admin");
    }

    private void prepare(CinemaPricingPolicyVersion target, List<CinemaPricingPolicyVersion> previous) {
        when(cinemas.existsById(firstCinema.getId())).thenReturn(true);
        when(policies.findByIdForUpdate(policy.getId())).thenReturn(Optional.of(policy));
        when(versions.findForUpdate(policy.getId(), target.getId())).thenReturn(Optional.of(target));
        when(versions.findPublishedForUpdate(policy.getId())).thenReturn(previous);
    }

    private void matrixValidation(CinemaPricingPolicyVersion target, boolean valid) {
        var matrix = mock(FareMatrixResponse.class);
        var result = mock(FareMatrixValidationResponse.class);
        when(matrices.getForPublication(firstCinema.getId(), policy.getId(), target.getId())).thenReturn(matrix);
        when(validation.validate(matrix)).thenReturn(result);
        when(result.valid()).thenReturn(valid);
    }

    private CinemaPricingPolicyVersion published(long revision, Set<Cinema> scope) {
        var version = revision(revision, scope);
        version.publish(NOW.minusDays(1), "original-actor");
        return version;
    }

    private CinemaPricingPolicyVersion revision(long revision, Set<Cinema> scope) {
        return new CinemaPricingPolicyVersion(policy, revision,
                new CinemaPricingPolicyVersion.Metadata("Weekday", null, PricingDayGroup.WEEKDAY), scope);
    }

    private static Cinema cinema() {
        var cinema = mock(Cinema.class);
        when(cinema.getId()).thenReturn(UUID.randomUUID());
        return cinema;
    }

    private static CinemaPricingPolicy policy() {
        var policy = mock(CinemaPricingPolicy.class);
        when(policy.getId()).thenReturn(UUID.randomUUID());
        return policy;
    }
}
