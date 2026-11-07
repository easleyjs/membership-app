package com.example.adminportal.dto;

import com.example.adminportal.entity.AppUser;
import com.example.adminportal.entity.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record MemberNoteCreateRequest(
        @NotNull Member member,
        @NotNull AppUser createdBy,
        @NotBlank String content,
        @NotNull Instant createdAt
        ) {}