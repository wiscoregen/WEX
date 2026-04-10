package com.wex.assessment.service;

import com.wex.assessment.domain.ConvertedPurchase;
import com.wex.assessment.domain.ExchangeRate;
import com.wex.assessment.domain.Purchase;
import com.wex.assessment.domain.RateCacheStats;
import com.wex.assessment.error.AppException;
import com.wex.assessment.error.ErrorCode;
import com.wex.assessment.repository.PurchaseRepository;
import com.wex.assessment.repository.RateRepository;
import com.wex.assessment.treasury.TreasuryClient;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseServiceTest {

    @Test
    void createTrimsDescriptionAndGeneratesServerSideUuid() {
        InMemoryPurchaseRepository purchaseRepository = new InMemoryPurchaseRepository();
        PurchaseService service = new PurchaseService(purchaseRepository, newRateService(List.of()));

        Purchase purchase = service.create(new CreatePurchaseCommand(
                "  Team lunch  ",
                LocalDate.parse("2025-04-10"),
                1_235L
        ));

        assertThat(purchase.description()).isEqualTo("Team lunch");
        assertThat(purchase.transactionDate()).isEqualTo(LocalDate.parse("2025-04-10"));
        assertThat(purchase.amountCents()).isEqualTo(1_235L);
        assertThat(UUID.fromString(purchase.id()).version()).isEqualTo(4);
        assertThat(purchaseRepository.findById(purchase.id())).contains(purchase);
    }

    @Test
    void createRejectsBlankDescription() {
        PurchaseService service = new PurchaseService(new InMemoryPurchaseRepository(), newRateService(List.of()));

        assertThatThrownBy(() -> service.create(new CreatePurchaseCommand("   ", LocalDate.parse("2025-04-10"), 100L)))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getCode()).isEqualTo(ErrorCode.BAD_REQUEST))
                .hasMessage("description is required");
    }

    @Test
    void createRejectsDescriptionLongerThanFiftyCharacters() {
        PurchaseService service = new PurchaseService(new InMemoryPurchaseRepository(), newRateService(List.of()));

        assertThatThrownBy(() -> service.create(new CreatePurchaseCommand("x".repeat(51), LocalDate.parse("2025-04-10"), 100L)))
                .isInstanceOf(AppException.class)
                .hasMessage("description must not exceed 50 characters");
    }

    @Test
    void createRejectsMissingTransactionDate() {
        PurchaseService service = new PurchaseService(new InMemoryPurchaseRepository(), newRateService(List.of()));

        assertThatThrownBy(() -> service.create(new CreatePurchaseCommand("Team lunch", null, 100L)))
                .isInstanceOf(AppException.class)
                .hasMessage("transactionDate is required");
    }

    @Test
    void createRejectsNonPositiveAmount() {
        PurchaseService service = new PurchaseService(new InMemoryPurchaseRepository(), newRateService(List.of()));

        assertThatThrownBy(() -> service.create(new CreatePurchaseCommand("Team lunch", LocalDate.parse("2025-04-10"), 0L)))
                .isInstanceOf(AppException.class)
                .hasMessage("purchaseAmount must be greater than zero");
    }

    @Test
    void getConvertedPurchaseReturnsUsdAtParity() {
        InMemoryPurchaseRepository purchaseRepository = new InMemoryPurchaseRepository();
        purchaseRepository.save(new Purchase("550e8400-e29b-41d4-a716-446655440000", "Lunch", LocalDate.parse("2025-04-10"), 1_235L));

        PurchaseService service = new PurchaseService(purchaseRepository, newRateService(List.of()));

        ConvertedPurchase converted = service.getConvertedPurchase("550e8400-e29b-41d4-a716-446655440000", " usd ");

        assertThat(converted.targetCurrency()).isEqualTo("USD");
        assertThat(converted.exchangeRate()).isEqualTo("1");
        assertThat(converted.exchangeRateEffectiveDate()).isEqualTo(LocalDate.parse("2025-04-10"));
        assertThat(converted.convertedAmountCents()).isEqualTo(1_235L);
    }

    @Test
    void getConvertedPurchaseConvertsWithMostRecentHistoricalRate() {
        InMemoryPurchaseRepository purchaseRepository = new InMemoryPurchaseRepository();
        purchaseRepository.save(new Purchase("550e8400-e29b-41d4-a716-446655440000", "Lunch", LocalDate.parse("2025-04-10"), 1_235L));

        PurchaseService service = new PurchaseService(purchaseRepository, newRateService(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.90", LocalDate.parse("2025-03-31"))
        )));

        ConvertedPurchase converted = service.getConvertedPurchase("550e8400-e29b-41d4-a716-446655440000", "EUR");

        assertThat(converted.targetCurrency()).isEqualTo("EUR");
        assertThat(converted.exchangeRate()).isEqualTo("0.90");
        assertThat(converted.convertedAmountCents()).isEqualTo(1_112L);
    }

    @Test
    void getConvertedPurchaseRejectsUnknownPurchaseId() {
        PurchaseService service = new PurchaseService(new InMemoryPurchaseRepository(), newRateService(List.of()));

        assertThatThrownBy(() -> service.getConvertedPurchase("550e8400-e29b-41d4-a716-446655440000", "USD"))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getCode()).isEqualTo(ErrorCode.PURCHASE_NOT_FOUND));
    }

    private RateService newRateService(List<ExchangeRate> rates) {
        InMemoryRateRepository repository = new InMemoryRateRepository();
        repository.replaceAll(rates, Instant.parse("2025-04-01T00:00:00Z"));
        TreasuryClient client = () -> List.of();
        return new RateService(repository, client, Clock.fixed(Instant.parse("2025-04-01T00:00:00Z"), ZoneOffset.UTC));
    }

    private static final class InMemoryPurchaseRepository implements PurchaseRepository {
        private final Map<String, Purchase> purchases = new LinkedHashMap<>();

        @Override
        public Purchase save(Purchase purchase) {
            purchases.put(purchase.id(), purchase);
            return purchase;
        }

        @Override
        public Optional<Purchase> findById(String purchaseId) {
            return Optional.ofNullable(purchases.get(purchaseId));
        }
    }

    private static final class InMemoryRateRepository implements RateRepository {
        private final Map<String, List<ExchangeRate>> ratesByCurrency = new LinkedHashMap<>();
        private Instant lastRefreshedUtc;

        @Override
        public void replaceAll(List<ExchangeRate> rates, Instant refreshedAt) {
            ratesByCurrency.clear();
            for (ExchangeRate rate : rates) {
                ratesByCurrency.computeIfAbsent(rate.currencyCode(), ignored -> new ArrayList<>()).add(rate);
            }
            for (List<ExchangeRate> currencyRates : ratesByCurrency.values()) {
                currencyRates.sort(Comparator.comparing(ExchangeRate::effectiveDate).reversed());
            }
            lastRefreshedUtc = refreshedAt;
        }

        @Override
        public Optional<ExchangeRate> findMostRecentOnOrBefore(String currencyCode, LocalDate purchaseDate, LocalDate windowStart) {
            return ratesByCurrency.getOrDefault(currencyCode, List.of())
                    .stream()
                    .filter(rate -> !rate.effectiveDate().isAfter(purchaseDate))
                    .filter(rate -> !rate.effectiveDate().isBefore(windowStart))
                    .findFirst();
        }

        @Override
        public RateCacheStats getStats() {
            int rateCount = ratesByCurrency.values().stream().mapToInt(List::size).sum();
            return new RateCacheStats(ratesByCurrency.size(), rateCount, lastRefreshedUtc);
        }
    }
}
