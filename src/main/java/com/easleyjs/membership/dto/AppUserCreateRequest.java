package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.UserRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AppUserCreateRequest(
        @NotNull UUID memberId,
        String username,
        String password,
        @NotNull UserRole role
) {}
