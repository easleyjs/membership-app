package com.example.adminportal.dto;

import com.example.adminportal.entity.CheckIn;
import com.example.adminportal.entity.CheckInMethod;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
        UUID id,
        UUID memberId,
        UUID checkedInById,
        Instant createdAt,
        CheckInMethod method,
        Long eventId
) {
    public static CheckInResponse from(CheckIn checkIn) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getMember().getId(),
                checkIn.getCheckedInBy().getId(),
                checkIn.getCreatedAt(),
                checkIn.getMethod(),
                checkIn.getEventId()
        );
    }
}
