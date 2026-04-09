package com.wex.assessment.repository;

import com.wex.assessment.domain.ExchangeRate;
import com.wex.assessment.domain.RateCacheStats;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RateRepository {

    void replaceAll(List<ExchangeRate> rates, Instant refreshedAt);

    Optional<ExchangeRate> findMostRecentOnOrBefore(String currencyCode, LocalDate purchaseDate, LocalDate windowStart);

    RateCacheStats getStats();
}

