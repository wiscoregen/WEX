package com.wex.assessment.web;

import com.wex.assessment.domain.RateCacheStats;

public record HealthResponse(
        String status,
        RateCacheStats rates
) {
}

