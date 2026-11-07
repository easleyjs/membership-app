package com.example.adminportal.dto;

import com.example.adminportal.entity.Membership;
import com.example.adminportal.entity.MembershipProgram;
import com.example.adminportal.entity.MembershipStatus;
import com.example.adminportal.entity.MembershipType;

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
                membership.getCreatedBy().getMember().getFirstName(),
                membership.getValidFrom(),
                membership.getValidUntil()
        );
    }
}
