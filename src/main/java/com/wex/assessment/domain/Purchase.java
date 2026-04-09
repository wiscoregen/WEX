package com.wex.assessment.domain;

import java.time.LocalDate;

public record Purchase(
        String id,
        String description,
        LocalDate transactionDate,
        long amountCents
) {
}

