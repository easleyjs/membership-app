package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.AdmissionType;
import com.easleyjs.membership.entity.CheckIn;
import com.easleyjs.membership.entity.CheckInMethod;
import com.easleyjs.membership.entity.Member;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CheckInResponse(
        UUID id,
        UUID memberId,
        UUID checkedInById,
        Instant createdAt,
        CheckInMethod method,
        Long eventId,
        AdmissionType admissionType,
        String checkedInByName
) {
    public static CheckInResponse from(CheckIn checkIn) {
        Member staff = checkIn.getCheckedInBy().getMember();
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getMember().getId(),
                checkIn.getCheckedInBy().getId(),
                checkIn.getCreatedAt(),
                checkIn.getMethod(),
                checkIn.getEventId(),
                checkIn.getAdmissionType(),
                staff == null ? null : (Objects.toString(staff.getFirstName(), "") + " "
                        + Objects.toString(staff.getLastName(), "")).strip()
        );
    }
}
