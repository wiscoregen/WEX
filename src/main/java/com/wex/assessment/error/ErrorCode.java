package com.wex.assessment.error;

public enum ErrorCode {
    BAD_REQUEST("bad_request"),
    ROUTE_NOT_FOUND("route_not_found"),
    PURCHASE_NOT_FOUND("purchase_not_found"),
    METHOD_NOT_ALLOWED("method_not_allowed"),
    UNSUPPORTED_MEDIA_TYPE("unsupported_media_type"),
    NOT_ACCEPTABLE("not_acceptable"),
    UNSUPPORTED_CURRENCY("unsupported_currency"),
    NO_RATE_AVAILABLE("no_rate_available"),
    INTERNAL_ERROR("internal_error");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
