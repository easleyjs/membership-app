package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.UserRole;

import java.util.UUID;

public record AppUserResponse(
        UUID id,
        String firstName,
        String lastName,
        UserRole role
) {
    public static AppUserResponse from(AppUser appUser) {
        return new AppUserResponse(
                appUser.getId(),
                appUser.getMember().getFirstName(),
                appUser.getMember().getLastName(),
                appUser.getRole()
        );
    }
}
