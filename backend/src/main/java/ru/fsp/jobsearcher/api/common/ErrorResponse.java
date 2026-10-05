package ru.fsp.jobsearcher.api.common;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.code(), message, Instant.now());
    }
}
