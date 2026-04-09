package com.wex.assessment.web;

public record ApiError(
        String code,
        String message,
        int status
) {
}

