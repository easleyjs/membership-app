package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.Membership;
import com.easleyjs.membership.entity.MembershipProgram;
import com.easleyjs.membership.entity.MembershipStatus;
import com.easleyjs.membership.entity.MembershipType;

import java.time.Instant;


public record MembershipResponse(
        Long id,
        MembershipProgram program,
        MembershipType type,
        MembershipStatus status,
        Instant createdAt,
        String createdBy,
        Instant validFrom,
        Instant validUntil
) {
    public static MembershipResponse from(Membership membership) {
        return new MembershipResponse(
                membership.getId(),
                membership.getProgram(),
                membership.getType(),
                membership.getStatus(),
                membership.getCreatedAt(),
                membership.getCreatedBy() == null
                        ? null
                        : membership.getCreatedBy().getMember().getFirstName(),
                membership.getValidFrom(),
                membership.getValidUntil()
        );
    }
}
