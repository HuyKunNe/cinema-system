package com.cinema.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

class PublishedVersionLifecycleTest {

    @Test
    void retiredPricingRemainsUsableOnlyByAnAlreadyAssignedShowtime() {
        Cinema cinema = mock(Cinema.class);
        when(cinema.getId()).thenReturn(UUID.randomUUID());
        CinemaPricingPolicy policy = mock(CinemaPricingPolicy.class);
        when(policy.getId()).thenReturn(UUID.randomUUID());
        var revision = new CinemaPricingPolicyVersion(policy, 1,
                new CinemaPricingPolicyVersion.Metadata("Weekday", null, PricingDayGroup.WEEKDAY),
                Set.of(cinema));
        var publishedAt = OffsetDateTime.parse("2026-11-11T00:00:00Z");
        revision.publish(publishedAt, "admin");

        Room room = mock(Room.class);
        when(room.getCinema()).thenReturn(cinema);
        Showtime existing = emptyScheduledShowtime(room);
        existing.replacePricingPolicyVersion(revision);

        revision.retire();
        assertEquals(PricingPolicyVersionStatus.RETIRED, revision.getStatus());
        assertEquals(publishedAt, revision.getPublishedAt());
        existing.validateAssignedPricingPolicyVersion();
        assertEquals(revision.getId(), existing.getPricingPolicyVersion().getId());

        var error = assertThrows(ConflictException.class,
                () -> emptyScheduledShowtime(room).replacePricingPolicyVersion(revision));
        assertEquals(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_PUBLISHED, error.getErrorCode());
        assertEquals(PricingPolicyVersionStatus.DRAFT,
                revision.newDraft(2, UUID.randomUUID()).draft().getStatus());
        assertThrows(ConflictException.class, revision::deleteDraft);
        assertThrows(ConflictException.class, () -> revision.publish(publishedAt, "admin"));
    }

    @Test
    void retiredLayoutKeepsPublicationAndCannotBeEditedDeletedOrRepublished() {
        var layout = new RoomLayout(mock(Room.class), 1, BigDecimal.TEN, BigDecimal.TEN);
        var publicationTime = OffsetDateTime.parse("2026-11-11T00:00:00Z");
        layout.publish(publicationTime);
        layout.retire();

        assertEquals(RoomLayoutStatus.RETIRED, layout.getStatus());
        assertEquals(publicationTime, layout.getPublishedAt());
        assertThrows(ConflictException.class, layout::requireDraft);
        assertThrows(ConflictException.class, layout::deleteDraft);
        assertThrows(ConflictException.class, () -> layout.publish(publicationTime));
    }

    private static Showtime emptyScheduledShowtime(Room room) {
        Showtime showtime = org.springframework.beans.BeanUtils.instantiateClass(Showtime.class);
        ReflectionTestUtils.setField(showtime, "room", room);
        ReflectionTestUtils.setField(showtime, "status", ShowtimeStatus.SCHEDULED);
        return showtime;
    }
}
