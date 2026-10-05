package ru.fsp.jobsearcher.api.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ApiException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, message);
    }

    public static ApiException badState(String message) {
        return new ApiException(ErrorCode.BAD_STATE, HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException consentRequired(String message) {
        return new ApiException(ErrorCode.CONSENT_REQUIRED, HttpStatus.FORBIDDEN, message);
    }

    public static ApiException gradeCooldown(String message) {
        return new ApiException(ErrorCode.GRADE_COOLDOWN, HttpStatus.CONFLICT, message);
    }
}
