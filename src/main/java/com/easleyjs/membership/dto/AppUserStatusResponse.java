package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.AppUser;

public record AppUserStatusResponse(
    boolean enabled
) {
    public static AppUserStatusResponse from(AppUser appUser) {
        return new AppUserStatusResponse(
            appUser.isEnabled()
        );
    }
}
