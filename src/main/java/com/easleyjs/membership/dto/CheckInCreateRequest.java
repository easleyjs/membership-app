package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.CheckInMethod;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;
import java.time.Instant;

public record CheckInCreateRequest(
        @NotNull UUID id,
        @NotNull UUID memberId,
        @NotNull UUID staffMemberId,
        @NotNull CheckInMethod method,
        @NotNull Instant createdAt,
        Long eventId
) {}
