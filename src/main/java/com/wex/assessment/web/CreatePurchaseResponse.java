package com.wex.assessment.web;

import java.time.LocalDate;

public record CreatePurchaseResponse(
        String id,
        String description,
        LocalDate transactionDate,
        String purchaseAmountUsd
) {
}

