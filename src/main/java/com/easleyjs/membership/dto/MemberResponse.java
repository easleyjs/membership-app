package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.MemberStatus;

import java.util.UUID;

public record MemberResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Long memberNumber,
        MemberStatus status,
        boolean hasNotes,
        long noteCount
) {
    public static MemberResponse from(Member member, long noteCount) {
        return new MemberResponse(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getMemberNumber(),
                member.getStatus(),
                noteCount > 0,
                noteCount
        );
    }
}
