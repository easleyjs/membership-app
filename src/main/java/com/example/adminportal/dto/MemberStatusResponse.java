package com.example.adminportal.dto;

import com.example.adminportal.entity.Member;
import com.example.adminportal.entity.MemberStatus;

import java.util.UUID;

public record MemberStatusResponse(
        MemberStatus status
) {
    public static MemberStatusResponse from(Member member) {
        return new MemberStatusResponse(
                member.getStatus()
        );
    }
}
