package ru.fsp.jobsearcher.api.common;

public enum ErrorCode {
    NOT_FOUND("JS_001"),
    VALIDATION("JS_002"),
    FORBIDDEN("JS_003"),
    CONFLICT("JS_004"),
    UNAUTHORIZED("JS_005"),
    CONSENT_REQUIRED("JS_006"),
    GRADE_COOLDOWN("JS_007"),
    BAD_STATE("JS_008"),
    LLM_UNAVAILABLE("JS_009"),
    INTERNAL("JS_999");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
