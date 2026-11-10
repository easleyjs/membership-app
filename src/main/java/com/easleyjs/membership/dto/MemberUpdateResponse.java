package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemberUpdateResponse(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull Long memberNumber

) {
    public static MemberUpdateResponse from(Member member) {
        return new MemberUpdateResponse(
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getMemberNumber()
        );
    }
}