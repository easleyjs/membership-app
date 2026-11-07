package com.example.adminportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MemberNoteCreateRequest(
        @NotNull UUID createdById,
        @NotBlank @Size(max = 2000) String content
        ) {}
