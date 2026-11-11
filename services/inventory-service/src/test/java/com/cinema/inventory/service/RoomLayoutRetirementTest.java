package com.cinema.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.dto.request.PublishRoomLayoutRequest;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomLayoutMapper;
import com.cinema.inventory.repository.RoomLayoutElementRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomLayoutSeatRepository;
import com.cinema.inventory.repository.RoomRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class RoomLayoutRetirementTest {

    private final RoomLayoutRepository layouts = mock(RoomLayoutRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomLayoutSeatBindingService bindings = mock(RoomLayoutSeatBindingService.class);
    private final RoomLayoutPublicationService service = new RoomLayoutPublicationService(
            layouts, mock(RoomLayoutSeatRepository.class), mock(RoomLayoutElementRepository.class),
            mock(RoomLayoutPublicationValidator.class), mock(RoomLayoutMapper.class),
            Clock.systemUTC(), bindings, rooms);
    private final Room room = mock(Room.class);
    private final UUID roomId = UUID.randomUUID();

    @Test
    void publishingRetiresEveryPreviousPublishedLayoutForTheRoom() {
        var first = layout(1);
        var second = layout(2);
        var next = layout(3);
        var publicationTime = OffsetDateTime.parse("2026-11-10T00:00:00Z");
        first.publish(publicationTime);
        second.publish(publicationTime);
        prepare(next, List.of(first, second));

        service.publish(next.getId(), new PublishRoomLayoutRequest(0L));

        assertEquals(RoomLayoutStatus.PUBLISHED, next.getStatus());
        assertEquals(RoomLayoutStatus.RETIRED, first.getStatus());
        assertEquals(RoomLayoutStatus.RETIRED, second.getStatus());
        assertEquals(publicationTime, first.getPublishedAt());
        var order = inOrder(rooms, layouts);
        order.verify(rooms).findByIdForUpdate(roomId);
        order.verify(layouts).findByIdForUpdate(next.getId());
        order.verify(layouts).findPublishedForUpdate(roomId);
    }

    @Test
    void cannotPublishAnOlderDraftOverTheCurrentLayout() {
        var current = layout(3);
        current.publish(OffsetDateTime.parse("2026-11-10T00:00:00Z"));
        var obsolete = layout(2);
        obsolete.defineMatrix(6, 6);
        prepare(obsolete, List.of(current));

        var error = assertThrows(ConflictException.class,
                () -> service.publish(obsolete.getId(), new PublishRoomLayoutRequest(0L)));

        assertEquals(InventoryErrorCode.ROOM_LAYOUT_PUBLICATION_SUPERSEDED, error.getErrorCode());
        assertEquals(RoomLayoutStatus.PUBLISHED, current.getStatus());
        assertEquals(RoomLayoutStatus.DRAFT, obsolete.getStatus());
        verify(bindings, never()).bindForPublication(obsolete);
        verify(layouts, never()).flush();
    }

    private RoomLayout layout(long revision) {
        return new RoomLayout(room, revision, BigDecimal.TEN, BigDecimal.TEN);
    }

    private void prepare(RoomLayout target, List<RoomLayout> previous) {
        when(layouts.findRoomId(target.getId())).thenReturn(Optional.of(roomId));
        when(rooms.findByIdForUpdate(roomId)).thenReturn(Optional.of(room));
        when(layouts.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
        when(layouts.findPublishedForUpdate(roomId)).thenReturn(previous);
    }
}
