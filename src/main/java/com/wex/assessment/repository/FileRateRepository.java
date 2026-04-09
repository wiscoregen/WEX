package com.wex.assessment.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.assessment.config.AppProperties;
import com.wex.assessment.domain.ExchangeRate;
import com.wex.assessment.domain.RateCacheStats;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class FileRateRepository implements RateRepository {

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, List<ExchangeRate>> ratesByCurrency = new HashMap<>();
    private Instant lastRefreshedUtc;

    public FileRateRepository(AppProperties properties, ObjectMapper objectMapper) {
        this.filePath = Path.of(properties.getDataDir(), "treasury-rates.json");
        this.objectMapper = objectMapper;

        RateFile rateFile = FileStorageSupport.readOrDefault(
                objectMapper,
                filePath,
                RateFile.class,
                () -> new RateFile(null, List.of())
        );

        this.lastRefreshedUtc = rateFile.lastRefreshedUtc();
        rebuildIndex(rateFile.rates());
    }

    @Override
    public void replaceAll(List<ExchangeRate> rates, Instant refreshedAt) {
        lock.writeLock().lock();
        try {
            rebuildIndex(rates);
            this.lastRefreshedUtc = refreshedAt;
            persistLocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<ExchangeRate> findMostRecentOnOrBefore(String currencyCode, LocalDate purchaseDate, LocalDate windowStart) {
        lock.readLock().lock();
        try {
            List<ExchangeRate> rates = ratesByCurrency.getOrDefault(currencyCode, List.of());
            for (ExchangeRate rate : rates) {
                if (rate.effectiveDate().isAfter(purchaseDate)) {
                    continue;
                }
                if (rate.effectiveDate().isBefore(windowStart)) {
                    break;
                }
                return Optional.of(rate);
            }
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public RateCacheStats getStats() {
        lock.readLock().lock();
        try {
            int rateCount = ratesByCurrency.values().stream().mapToInt(List::size).sum();
            return new RateCacheStats(ratesByCurrency.size(), rateCount, lastRefreshedUtc);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void persistLocked() {
        List<ExchangeRate> rates = new ArrayList<>();
        for (List<ExchangeRate> currencyRates : ratesByCurrency.values()) {
            rates.addAll(currencyRates);
        }

        rates.sort(Comparator
                .comparing(ExchangeRate::currencyCode)
                .thenComparing(ExchangeRate::effectiveDate, Comparator.reverseOrder())
                .thenComparing(ExchangeRate::treasuryDescriptor, Comparator.nullsLast(Comparator.naturalOrder())));

        FileStorageSupport.writeAtomically(objectMapper, filePath, new RateFile(lastRefreshedUtc, rates));
    }

    private void rebuildIndex(List<ExchangeRate> rates) {
        ratesByCurrency.clear();

        for (ExchangeRate rate : rates) {
            ratesByCurrency.computeIfAbsent(rate.currencyCode(), ignored -> new ArrayList<>()).add(rate);
        }

        for (List<ExchangeRate> currencyRates : ratesByCurrency.values()) {
            currencyRates.sort(Comparator
                    .comparing(ExchangeRate::effectiveDate, Comparator.reverseOrder())
                    .thenComparing(ExchangeRate::treasuryDescriptor, Comparator.nullsLast(Comparator.naturalOrder())));
        }
    }

    private record RateFile(Instant lastRefreshedUtc, List<ExchangeRate> rates) {
    }
}
