package com.example.adminportal.dto;

import com.example.adminportal.entity.Event;
import com.example.adminportal.entity.EventLocation;
import com.example.adminportal.entity.EventStatus;
import com.example.adminportal.entity.Member;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean membershipRequired,
        boolean coverRequired,
        EventLocation location,
        EventStatus status
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
                event.getStatus()
        );
    }
}
