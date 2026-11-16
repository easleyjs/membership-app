package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.MemberNote;
import com.easleyjs.membership.entity.Member;

import java.time.Instant;
import java.util.Objects;

public record MemberNoteResponse(
        Long id,
        String createdBy,
        String content,
        Instant createdAt,
        String createdByName
) {
    public static MemberNoteResponse from(MemberNote memberNote) {
        Member author = memberNote.getCreatedBy().getMember();
        return new MemberNoteResponse(
                memberNote.getId(),
                author.getFirstName(),
                memberNote.getContent(),
                memberNote.getCreatedAt(),
                (Objects.toString(author.getFirstName(), "") + " "
                        + Objects.toString(author.getLastName(), "")).strip()
        );
    }
}
