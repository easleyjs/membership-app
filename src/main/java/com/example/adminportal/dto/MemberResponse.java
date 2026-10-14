package com.example.adminportal.dto;

import com.example.adminportal.entity.Member;
import com.example.adminportal.entity.MemberStatus;

public record MemberResponse(
        String firstName,
        String lastName,
        String email,
        Long memberNumber,
        MemberStatus status
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getMemberNumber(),
                member.getStatus()
        );
    }
}
