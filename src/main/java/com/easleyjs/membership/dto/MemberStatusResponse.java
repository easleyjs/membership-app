package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.MemberStatus;

public record MemberStatusResponse(
        MemberStatus status
) {
    public static MemberStatusResponse from(Member member) {
        return new MemberStatusResponse(
                member.getStatus()
        );
    }
}
