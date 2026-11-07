package com.example.adminportal.dto;

import com.example.adminportal.entity.Member;
import com.example.adminportal.entity.MemberStatus;

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
