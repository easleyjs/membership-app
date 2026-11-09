package com.example.adminportal.dto;

import com.example.adminportal.entity.AppUser;
import jakarta.validation.constraints.NotNull;

public record AppUserStatusRequest(
    @NotNull Boolean enabled
) {}
