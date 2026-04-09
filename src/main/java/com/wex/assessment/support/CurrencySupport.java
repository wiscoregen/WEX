package com.wex.assessment.support;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

public final class CurrencySupport {

    private static final Set<String> ISO_CODES = Currency.getAvailableCurrencies()
            .stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toUnmodifiableSet());

    private CurrencySupport() {
    }

    public static boolean isValidIso4217(String currencyCode) {
        return currencyCode != null && ISO_CODES.contains(currencyCode.trim().toUpperCase());
    }
}

