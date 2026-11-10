package com.easleyjs.membership.dto;

import com.easleyjs.membership.entity.EventLocation;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventCreateRequest(
        @NotBlank String name,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean membershipRequired,
        boolean coverRequired,
        @NotNull EventLocation location
        ) {
    @AssertTrue(
            message = "Provide both start and end times, with end after start, "
                    + "or leave both empty"
    )
    public boolean isValidDateRange() {
        if (startsAt == null && endsAt == null) {
            return true;
        }

        return startsAt != null
                && endsAt != null
                && endsAt.isAfter(startsAt);
    }
}