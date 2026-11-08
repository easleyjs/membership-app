package com.example.adminportal.dto;

import com.example.adminportal.entity.AppUser;

public record AppUserStatusResponse(
    boolean enabled
) {
    public static AppUserStatusResponse from(AppUser appUser) {
        return new AppUserStatusResponse(
            appUser.isEnabled()
        );
    }
}
