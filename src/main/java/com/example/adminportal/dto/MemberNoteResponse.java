package com.example.adminportal.dto;

import com.example.adminportal.entity.*;

import java.time.Instant;

public record MemberNoteResponse(
        Long id,
        String createdBy,
        String content,
        Instant createdAt
) {
    public static MemberNoteResponse from(MemberNote memberNote) {
        return new MemberNoteResponse(
                memberNote.getId(),
                memberNote.getCreatedBy().getMember().getFirstName(),
                memberNote.getContent(),
                memberNote.getCreatedAt()
        );
    }
}
