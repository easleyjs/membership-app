package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record AppUserUpdateRequest(@NotNull UserRole role, @NotNull Boolean enabled) {}
