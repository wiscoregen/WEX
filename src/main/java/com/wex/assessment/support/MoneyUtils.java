package com.wex.assessment.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.wex.assessment.error.AppException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static long parseUsdToCents(JsonNode rawAmount) {
        if (rawAmount == null || rawAmount.isNull()) {
            throw AppException.badRequest("purchaseAmount is required");
        }

        if (!rawAmount.isTextual() && !rawAmount.isNumber()) {
            throw AppException.badRequest("purchaseAmount must be a JSON string or number");
        }

        return parseUsdToCents(rawAmount.asText());
    }

    public static long parseUsdToCents(String rawAmount) {
        if (rawAmount == null || rawAmount.trim().isEmpty()) {
            throw AppException.badRequest("purchaseAmount is required");
        }

        try {
            BigDecimal amount = new BigDecimal(rawAmount.trim());
            if (amount.signum() <= 0) {
                throw AppException.badRequest("purchaseAmount must be greater than zero");
            }

            BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
            long cents = rounded.movePointRight(2).longValueExact();
            if (cents <= 0) {
                throw AppException.badRequest("purchaseAmount must be greater than zero");
            }

            return cents;
        } catch (NumberFormatException ex) {
            throw AppException.badRequest("purchaseAmount must be a positive decimal amount");
        } catch (ArithmeticException ex) {
            throw AppException.badRequest("purchaseAmount is too large");
        }
    }

    public static long multiplyCentsByRateAndRound(long cents, String exchangeRate) {
        try {
            BigDecimal rate = new BigDecimal(exchangeRate);
            BigDecimal convertedCents = BigDecimal.valueOf(cents)
                    .multiply(rate)
                    .setScale(0, RoundingMode.HALF_UP);

            return convertedCents.longValueExact();
        } catch (NumberFormatException ex) {
            throw AppException.internal("convert purchase amount", ex);
        } catch (ArithmeticException ex) {
            throw AppException.internal("convert purchase amount", ex);
        }
    }

    public static String formatCents(long cents) {
        boolean negative = cents < 0;
        long absolute = Math.abs(cents);
        String formatted = "%d.%02d".formatted(absolute / 100, absolute % 100);
        return negative ? "-" + formatted : formatted;
    }
}

