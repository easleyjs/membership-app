package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.Event;
import com.easleyjs.membership.entity.EventLocation;
import com.easleyjs.membership.entity.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        Long id,
        String name,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean membershipRequired,
        boolean coverRequired,
        EventLocation location,
        EventStatus status,
        UUID createdBy
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.isMembershipRequired(),
                event.isCoverRequired(),
                event.getLocation(),
                event.getStatus(),
                event.getCreatedBy() == null ? null : event.getCreatedBy().getId()
        );
    }
}
