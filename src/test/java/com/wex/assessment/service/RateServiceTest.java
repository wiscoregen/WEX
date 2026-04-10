package com.wex.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wex.assessment.config.AppProperties;
import com.wex.assessment.domain.ExchangeRate;
import com.wex.assessment.domain.RateCacheStats;
import com.wex.assessment.error.AppException;
import com.wex.assessment.error.ErrorCode;
import com.wex.assessment.repository.FileRateRepository;
import com.wex.assessment.treasury.TreasuryClient;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateServiceTest {

    @Test
    void lookupRateReturnsUsdAtParityForPurchaseDate() {
        RateService service = new RateService(newRepository(), emptyClient(), fixedClock());

        ExchangeRate rate = service.lookupRate(" usd ", LocalDate.parse("2025-05-15"));

        assertThat(rate.currencyCode()).isEqualTo("USD");
        assertThat(rate.exchangeRate()).isEqualTo("1");
        assertThat(rate.effectiveDate()).isEqualTo(LocalDate.parse("2025-05-15"));
    }

    @Test
    void lookupRateUsesMostRecentRateOnOrBeforePurchaseDate() {
        FileRateRepository repository = newRepository();
        repository.replaceAll(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.88", LocalDate.parse("2025-02-01")),
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.90", LocalDate.parse("2025-04-30")),
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.95", LocalDate.parse("2025-05-20"))
        ), Instant.now());

        RateService service = new RateService(repository, emptyClient(), fixedClock());

        ExchangeRate rate = service.lookupRate("EUR", LocalDate.parse("2025-05-15"));

        assertThat(rate.exchangeRate()).isEqualTo("0.90");
        assertThat(rate.effectiveDate()).isEqualTo(LocalDate.parse("2025-04-30"));
    }

    @Test
    void lookupRateReturnsNoRateAvailableOutsideSixMonthWindow() {
        FileRateRepository repository = newRepository();
        repository.replaceAll(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.82", LocalDate.parse("2024-07-31"))
        ), Instant.now());

        RateService service = new RateService(repository, emptyClient(), fixedClock());

        assertThatThrownBy(() -> service.lookupRate("EUR", LocalDate.parse("2025-02-01")))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getCode()).isEqualTo(ErrorCode.NO_RATE_AVAILABLE))
                .satisfies(throwable -> assertThat(((AppException) throwable).getStatus().value()).isEqualTo(422));
    }

    @Test
    void lookupRateAllowsRateExactlyAtSixMonthBoundary() {
        FileRateRepository repository = newRepository();
        repository.replaceAll(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.82", LocalDate.parse("2024-08-01"))
        ), Instant.now());

        RateService service = new RateService(repository, emptyClient(), fixedClock());

        ExchangeRate rate = service.lookupRate("EUR", LocalDate.parse("2025-02-01"));

        assertThat(rate.exchangeRate()).isEqualTo("0.82");
        assertThat(rate.effectiveDate()).isEqualTo(LocalDate.parse("2024-08-01"));
    }

    @Test
    void lookupRateReturnsUnsupportedCurrencyForValidIsoCodeNotBackedByTreasuryMapping() {
        FileRateRepository repository = newRepository();
        RateService service = new RateService(repository, emptyClient(), fixedClock());

        assertThatThrownBy(() -> service.lookupRate("AOA", LocalDate.parse("2025-02-01")))
                .isInstanceOf(AppException.class)
                .satisfies(throwable -> assertThat(((AppException) throwable).getCode()).isEqualTo(ErrorCode.UNSUPPORTED_CURRENCY));
    }

    @Test
    void refreshCacheStoresFetchedRatesAndUpdatesStats() {
        FileRateRepository repository = newRepository();
        Clock clock = Clock.fixed(Instant.parse("2025-04-01T00:00:00Z"), ZoneOffset.UTC);
        TreasuryClient treasuryClient = () -> List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.90", LocalDate.parse("2025-03-31")),
                new ExchangeRate("JPY", "Japan", "Yen", "Japan-Yen", "149.85", LocalDate.parse("2025-03-31"))
        );

        RateService service = new RateService(repository, treasuryClient, clock);

        RateCacheStats stats = service.refreshCache();

        assertThat(stats.currencyCount()).isEqualTo(2);
        assertThat(stats.rateCount()).isEqualTo(2);
        assertThat(stats.lastRefreshedUtc()).isEqualTo(clock.instant());
        assertThat(service.lookupRate("EUR", LocalDate.parse("2025-04-10")).exchangeRate()).isEqualTo("0.90");
    }

    private FileRateRepository newRepository() {
        AppProperties properties = new AppProperties();
        properties.setDataDir(Path.of(System.getProperty("java.io.tmpdir"), "wex-rate-tests-" + System.nanoTime()).toString());
        properties.getTreasury().setBaseUrl(URI.create("https://example.test"));
        return new FileRateRepository(properties, new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    private TreasuryClient emptyClient() {
        return () -> List.of();
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
}
