package com.example.adminportal.dto;

import com.example.adminportal.entity.AppUser;
import com.example.adminportal.entity.UserRole;

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
