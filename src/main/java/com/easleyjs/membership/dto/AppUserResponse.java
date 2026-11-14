package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.UserRole;

import java.util.UUID;

public record AppUserResponse(
        UUID id,
        String firstName,
        String lastName,
        UserRole role,
        UUID memberId
) {
    public static AppUserResponse from(AppUser appUser) {
        Member member = appUser.getMember();
        return new AppUserResponse(
                appUser.getId(),
                member.getFirstName(),
                member.getLastName(),
                appUser.getRole(),
                member.getId()
        );
    }
}
