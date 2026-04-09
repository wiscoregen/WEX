package com.wex.assessment.web;

import com.fasterxml.jackson.databind.JsonNode;

public record CreatePurchaseRequest(
        String description,
        String transactionDate,
        JsonNode purchaseAmount
) {
}

