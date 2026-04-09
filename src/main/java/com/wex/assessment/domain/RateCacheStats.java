package com.wex.assessment.domain;

import java.time.Instant;

public record RateCacheStats(
        int currencyCount,
        int rateCount,
        Instant lastRefreshedUtc
) {
}

