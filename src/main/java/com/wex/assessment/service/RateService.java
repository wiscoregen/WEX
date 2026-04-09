package com.wex.assessment.service;

import com.wex.assessment.domain.ExchangeRate;
import com.wex.assessment.domain.RateCacheStats;
import com.wex.assessment.error.AppException;
import com.wex.assessment.repository.RateRepository;
import com.wex.assessment.treasury.TreasuryClient;
import com.wex.assessment.treasury.TreasuryCurrencyCatalog;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class RateService {

    private final RateRepository rateRepository;
    private final TreasuryClient treasuryClient;
    private final Clock clock;

    public RateService(RateRepository rateRepository, TreasuryClient treasuryClient, Clock clock) {
        this.rateRepository = rateRepository;
        this.treasuryClient = treasuryClient;
        this.clock = clock;
    }

    public RateCacheStats refreshCache() {
        var rates = treasuryClient.fetchRates();
        rateRepository.replaceAll(rates, clock.instant());
        return rateRepository.getStats();
    }

    public ExchangeRate lookupRate(String currencyCode, LocalDate purchaseDate) {
        String normalizedCode = currencyCode.trim().toUpperCase();

        if ("USD".equals(normalizedCode)) {
            return new ExchangeRate("USD", "United States", "US Dollar", "United States-US Dollar", "1", purchaseDate);
        }

        if (!TreasuryCurrencyCatalog.isTreasurySupportedCurrency(normalizedCode)) {
            throw AppException.unsupportedCurrency(normalizedCode);
        }

        LocalDate windowStart = purchaseDate.minusMonths(6);
        return rateRepository.findMostRecentOnOrBefore(normalizedCode, purchaseDate, windowStart)
                .orElseThrow(() -> AppException.noRateAvailable(normalizedCode, purchaseDate.toString()));
    }

    public RateCacheStats getStats() {
        return rateRepository.getStats();
    }
}
