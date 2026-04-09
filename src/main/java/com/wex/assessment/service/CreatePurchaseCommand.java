package com.wex.assessment.service;

import java.time.LocalDate;

public record CreatePurchaseCommand(
        String description,
        LocalDate transactionDate,
        long amountCents
) {
}

