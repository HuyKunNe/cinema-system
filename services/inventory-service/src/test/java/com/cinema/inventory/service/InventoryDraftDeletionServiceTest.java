package com.cinema.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.exception.exception.VersionConflictException;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InventoryDraftDeletionServiceTest {

    @Mock private RoomLayoutRepository layouts;
    @Mock private CinemaPricingPolicyRepository policies;
    @Mock private CinemaPricingPolicyVersionRepository versions;
    @Mock private ShowtimeRepository showtimes;
    @InjectMocks private InventoryDraftDeletionService service;

    private final Cinema cinema = new Cinema("Cinema", "Address", "City");

    @Test
    void deletedLayoutCannotBeReadMutatedOrPublishedAgain() {
        RoomLayout layout = layout();
        when(layouts.findByIdForUpdate(layout.getId())).thenReturn(Optional.of(layout));

        service.deleteRoomLayoutDraft(layout.getId(), 0L);

        assertThrows(NotFoundException.class, layout::requireNotDeleted);
        assertThrows(NotFoundException.class, layout::markContentChanged);
        assertThrows(NotFoundException.class, () -> layout.publish(OffsetDateTime.now()));
        assertThrows(NotFoundException.class,
                () -> service.deleteRoomLayoutDraft(layout.getId(), 0L));
        verify(layouts).flush();
        verify(layouts, never()).delete(layout);
    }

    @Test
    void staleLayoutVersionDoesNotDelete() {
        RoomLayout layout = layout();
        when(layouts.findByIdForUpdate(layout.getId())).thenReturn(Optional.of(layout));

        var error = assertThrows(VersionConflictException.class,
                () -> service.deleteRoomLayoutDraft(layout.getId(), 9L));

        assertEquals(0L, error.getCurrentVersion());
        layout.requireDraft();
        verify(layouts, never()).flush();
    }

    @Test
    void publishedLayoutCannotBeDeleted() {
        RoomLayout layout = layout();
        layout.publish(OffsetDateTime.now());
        when(layouts.findByIdForUpdate(layout.getId())).thenReturn(Optional.of(layout));

        var error = assertThrows(ConflictException.class,
                () -> service.deleteRoomLayoutDraft(layout.getId(), 0L));

        assertEquals(InventoryErrorCode.ROOM_LAYOUT_NOT_DRAFT, error.getErrorCode());
        layout.requireNotDeleted();
        verify(layouts, never()).flush();
    }

    @Test
    void referencedLayoutCannotBeDeleted() {
        RoomLayout layout = layout();
        when(layouts.findByIdForUpdate(layout.getId())).thenReturn(Optional.of(layout));
        when(showtimes.existsByRoomLayout_Id(layout.getId())).thenReturn(true);

        var error = assertThrows(ConflictException.class,
                () -> service.deleteRoomLayoutDraft(layout.getId(), 0L));

        assertEquals(InventoryErrorCode.ROOM_LAYOUT_IN_USE, error.getErrorCode());
        layout.requireDraft();
        verify(layouts, never()).flush();
    }

    @Test
    void deletedPricingDraftRetainsRevisionAndIdempotencyIdentity() {
        var revision = pricingDraft();
        var policy = revision.getPolicy();
        var requestId = policy.getClientRequestId();
        stubPricing(revision);

        service.deletePricingPolicyDraft(cinema.getId(), policy.getId(), revision.getId(), 0L);

        assertThrows(NotFoundException.class, revision::requireNotDeleted);
        assertThrows(NotFoundException.class, revision::requireDraft);
        assertEquals(1L, revision.getRevision());
        assertEquals(requestId, policy.getClientRequestId());
        verify(versions).flush();
        verify(versions, never()).delete(revision);
        verify(policies, never()).delete(policy);
    }

    @Test
    void stalePricingVersionDoesNotDelete() {
        var revision = pricingDraft();
        stubPricing(revision);

        var error = assertThrows(VersionConflictException.class,
                () -> service.deletePricingPolicyDraft(
                        cinema.getId(), revision.getPolicy().getId(), revision.getId(), 2L));

        assertEquals(0L, error.getCurrentVersion());
        revision.requireDraft();
        verify(versions, never()).flush();
    }

    @Test
    void wrongCinemaCannotDeletePricingDraft() {
        var revision = pricingDraft();
        stubPricing(revision);

        assertThrows(NotFoundException.class,
                () -> service.deletePricingPolicyDraft(
                        UUID.randomUUID(), revision.getPolicy().getId(), revision.getId(), 0L));

        revision.requireDraft();
        verify(versions, never()).flush();
    }

    @Test
    void publishedPricingVersionCannotBeDeleted() {
        var revision = pricingDraft();
        revision.publish(OffsetDateTime.now(), "manager");
        stubPricing(revision);

        var error = assertThrows(ConflictException.class,
                () -> service.deletePricingPolicyDraft(
                        cinema.getId(), revision.getPolicy().getId(), revision.getId(), 0L));

        assertEquals(InventoryErrorCode.PRICING_POLICY_NOT_DRAFT, error.getErrorCode());
        revision.requireNotDeleted();
        verify(versions, never()).flush();
    }

    @Test
    void referencedPricingDraftCannotBeDeleted() {
        var revision = pricingDraft();
        stubPricing(revision);
        when(showtimes.existsByPricingPolicyVersion_Id(revision.getId())).thenReturn(true);

        var error = assertThrows(ConflictException.class,
                () -> service.deletePricingPolicyDraft(
                        cinema.getId(), revision.getPolicy().getId(), revision.getId(), 0L));

        assertEquals(InventoryErrorCode.PRICING_POLICY_VERSION_IN_USE, error.getErrorCode());
        revision.requireDraft();
        verify(versions, never()).flush();
    }

    @Test
    void negativeVersionDoesNotReachRepositories() {
        assertThrows(ValidationException.class,
                () -> service.deleteRoomLayoutDraft(UUID.randomUUID(), -1L));
        assertThrows(ValidationException.class,
                () -> service.deletePricingPolicyDraft(
                        cinema.getId(), UUID.randomUUID(), UUID.randomUUID(), -1L));
        org.mockito.Mockito.verifyNoInteractions(layouts, policies, versions, showtimes);
    }

    private RoomLayout layout() {
        return new RoomLayout(new Room(cinema, "Room", RoomType.STANDARD),
                1, BigDecimal.TEN, BigDecimal.TEN);
    }

    private CinemaPricingPolicyVersion pricingDraft() {
        var policy = new CinemaPricingPolicy(cinema, UUID.randomUUID(), "a".repeat(64), "b".repeat(64));
        return new CinemaPricingPolicyVersion(policy, 1,
                new CinemaPricingPolicyVersion.Metadata("Prices", null, PricingDayGroup.WEEKDAY),
                Set.of(cinema));
    }

    private void stubPricing(CinemaPricingPolicyVersion revision) {
        when(policies.findByIdForUpdate(revision.getPolicy().getId()))
                .thenReturn(Optional.of(revision.getPolicy()));
        when(versions.findForUpdate(revision.getPolicy().getId(), revision.getId()))
                .thenReturn(Optional.of(revision));
    }
}
