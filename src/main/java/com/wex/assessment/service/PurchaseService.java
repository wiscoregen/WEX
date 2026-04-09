package com.wex.assessment.service;

import com.wex.assessment.domain.ConvertedPurchase;
import com.wex.assessment.domain.Purchase;
import com.wex.assessment.error.AppException;
import com.wex.assessment.repository.PurchaseRepository;
import com.wex.assessment.support.MoneyUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final RateService rateService;

    public PurchaseService(PurchaseRepository purchaseRepository, RateService rateService) {
        this.purchaseRepository = purchaseRepository;
        this.rateService = rateService;
    }

    public Purchase create(CreatePurchaseCommand command) {
        String description = command.description() == null ? "" : command.description().trim();
        if (description.isEmpty()) {
            throw AppException.badRequest("description is required");
        }
        if (description.length() > 50) {
            throw AppException.badRequest("description must not exceed 50 characters");
        }
        if (command.transactionDate() == null) {
            throw AppException.badRequest("transactionDate is required");
        }
        if (command.amountCents() <= 0) {
            throw AppException.badRequest("purchaseAmount must be greater than zero");
        }

        Purchase purchase = new Purchase(
                UUID.randomUUID().toString(),
                description,
                command.transactionDate(),
                command.amountCents()
        );

        return purchaseRepository.save(purchase);
    }

    public ConvertedPurchase getConvertedPurchase(String purchaseId, String targetCurrency) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> AppException.purchaseNotFound(purchaseId));

        var rate = rateService.lookupRate(targetCurrency, purchase.transactionDate());
        long convertedAmountCents = MoneyUtils.multiplyCentsByRateAndRound(purchase.amountCents(), rate.exchangeRate());

        return new ConvertedPurchase(
                purchase,
                targetCurrency.trim().toUpperCase(),
                rate.exchangeRate(),
                rate.effectiveDate(),
                convertedAmountCents
        );
    }
}

