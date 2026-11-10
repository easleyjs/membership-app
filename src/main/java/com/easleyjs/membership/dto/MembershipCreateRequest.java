package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.MembershipProgram;
import com.easleyjs.membership.entity.MembershipType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record MembershipCreateRequest(
    @NotNull MembershipProgram membershipProgram,
    @NotNull MembershipType membershipType,
    @NotNull Instant validFrom,
    Instant validUntil
) {
}
