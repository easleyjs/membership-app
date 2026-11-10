package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.MemberNote;
import com.easleyjs.membership.entity.*;

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
