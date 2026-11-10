package com.easleyjs.membership.dto;

import jakarta.validation.constraints.NotNull;

public record AppUserStatusRequest(
    @NotNull Boolean enabled
) {}
