package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.AdmissionType;
import com.easleyjs.membership.entity.CheckIn;
import com.easleyjs.membership.entity.CheckInMethod;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
        UUID id,
        UUID memberId,
        UUID checkedInById,
        Instant createdAt,
        CheckInMethod method,
        Long eventId,
        AdmissionType admissionType
) {
    public static CheckInResponse from(CheckIn checkIn) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getMember().getId(),
                checkIn.getCheckedInBy().getId(),
                checkIn.getCreatedAt(),
                checkIn.getMethod(),
                checkIn.getEventId(),
                checkIn.getAdmissionType()
        );
    }
}
