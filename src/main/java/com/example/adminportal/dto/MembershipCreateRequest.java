package com.example.adminportal.dto;

import com.example.adminportal.entity.Membership;
import com.example.adminportal.entity.MembershipProgram;
import com.example.adminportal.entity.MembershipStatus;
import com.example.adminportal.entity.MembershipType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record MembershipCreateRequest(
    @NotNull MembershipProgram membershipProgram,
    @NotNull MembershipType membershipType,
    @NotNull Instant validFrom,
    Instant validUntil
) {
}
