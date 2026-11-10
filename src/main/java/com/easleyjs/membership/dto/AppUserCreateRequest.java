package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.UserRole;

import java.util.UUID;

public record AppUserCreateRequest(
        UUID memberId,
        String username,
        String password,
        UserRole role
) {}
