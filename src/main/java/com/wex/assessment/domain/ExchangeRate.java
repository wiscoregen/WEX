package com.wex.assessment.domain;

import java.time.LocalDate;

public record ExchangeRate(
        String currencyCode,
        String country,
        String currencyName,
        String treasuryDescriptor,
        String exchangeRate,
        LocalDate effectiveDate
) {
}

