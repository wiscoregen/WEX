package com.wex.assessment.web;

import java.time.LocalDate;

public record ConvertedPurchaseResponse(
        String id,
        String description,
        LocalDate transactionDate,
        String originalAmountUsd,
        String targetCurrency,
        String exchangeRate,
        LocalDate exchangeRateEffectiveDate,
        String convertedAmount
) {
}

