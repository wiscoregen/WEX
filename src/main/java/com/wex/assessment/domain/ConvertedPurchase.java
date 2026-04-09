package com.wex.assessment.domain;

import java.time.LocalDate;

public record ConvertedPurchase(
        Purchase purchase,
        String targetCurrency,
        String exchangeRate,
        LocalDate exchangeRateEffectiveDate,
        long convertedAmountCents
) {
}

