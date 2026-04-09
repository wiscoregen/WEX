package com.wex.assessment.web;

import com.wex.assessment.error.ErrorCode;

public record ApiErrorEnvelope(ApiError error) {

    public static ApiErrorEnvelope from(ErrorCode code, String message, int status) {
        return new ApiErrorEnvelope(new ApiError(code.value(), message, status));
    }
}

