package com.easleyjs.membership.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<String> details,
        String path,
        Instant timestamp
) {

    public static ApiErrorResponse of(String code, String message, String path) {
        return new ApiErrorResponse(code, message, List.of(), path, Instant.now());
    }

    public static ApiErrorResponse of(String code, String message, List<String> details, String path) {
        return new ApiErrorResponse(code, message, details, path, Instant.now());
    }
}