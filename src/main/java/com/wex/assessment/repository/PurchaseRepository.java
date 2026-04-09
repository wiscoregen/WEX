package com.wex.assessment.repository;

import com.wex.assessment.domain.Purchase;

import java.util.Optional;

public interface PurchaseRepository {

    Purchase save(Purchase purchase);

    Optional<Purchase> findById(String purchaseId);
}

