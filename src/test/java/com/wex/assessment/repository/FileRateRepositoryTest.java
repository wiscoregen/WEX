package com.wex.assessment.repository;

import com.wex.assessment.TestSupport;
import com.wex.assessment.config.AppProperties;
import com.wex.assessment.domain.ExchangeRate;
import com.wex.assessment.domain.RateCacheStats;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileRateRepositoryTest {

    @Test
    void replaceAllPersistsRatesAndStatsAcrossRepositoryInstances() {
        AppProperties properties = TestSupport.newProperties("wex-rate-repository-tests");
        FileRateRepository repository = new FileRateRepository(properties, TestSupport.newObjectMapper());
        Instant refreshedAt = Instant.parse("2025-04-01T00:00:00Z");

        repository.replaceAll(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.90", LocalDate.parse("2025-03-31")),
                new ExchangeRate("JPY", "Japan", "Yen", "Japan-Yen", "149.85", LocalDate.parse("2025-03-31"))
        ), refreshedAt);

        FileRateRepository reloaded = new FileRateRepository(properties, TestSupport.newObjectMapper());
        RateCacheStats stats = reloaded.getStats();

        assertThat(stats.currencyCount()).isEqualTo(2);
        assertThat(stats.rateCount()).isEqualTo(2);
        assertThat(stats.lastRefreshedUtc()).isEqualTo(refreshedAt);
    }

    @Test
    void findMostRecentOnOrBeforeReturnsBoundaryRateWithinWindow() {
        AppProperties properties = TestSupport.newProperties("wex-rate-repository-tests");
        FileRateRepository repository = new FileRateRepository(properties, TestSupport.newObjectMapper());

        repository.replaceAll(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.82", LocalDate.parse("2024-08-01")),
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.90", LocalDate.parse("2025-03-31"))
        ), Instant.parse("2025-04-01T00:00:00Z"));

        assertThat(repository.findMostRecentOnOrBefore(
                "EUR",
                LocalDate.parse("2025-02-01"),
                LocalDate.parse("2024-08-01")
        )).contains(new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.82", LocalDate.parse("2024-08-01")));
    }

    @Test
    void findMostRecentOnOrBeforeReturnsEmptyWhenOnlyFutureRatesExist() {
        AppProperties properties = TestSupport.newProperties("wex-rate-repository-tests");
        FileRateRepository repository = new FileRateRepository(properties, TestSupport.newObjectMapper());

        repository.replaceAll(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.90", LocalDate.parse("2025-03-31"))
        ), Instant.parse("2025-04-01T00:00:00Z"));

        assertThat(repository.findMostRecentOnOrBefore(
                "EUR",
                LocalDate.parse("2025-03-01"),
                LocalDate.parse("2024-09-01")
        )).isEmpty();
    }
}
