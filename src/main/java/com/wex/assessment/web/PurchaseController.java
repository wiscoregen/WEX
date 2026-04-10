package com.wex.assessment.web;

import com.wex.assessment.service.CreatePurchaseCommand;
import com.wex.assessment.service.PurchaseService;
import com.wex.assessment.service.RateService;
import com.wex.assessment.support.CurrencySupport;
import com.wex.assessment.support.MoneyUtils;
import com.wex.assessment.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final RateService rateService;

    public PurchaseController(PurchaseService purchaseService, RateService rateService) {
        this.purchaseService = purchaseService;
        this.rateService = rateService;
    }

    @PostMapping(path = "/api/v1/purchases", consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePurchaseResponse createPurchase(@RequestBody CreatePurchaseRequest request) {
        LocalDate transactionDate = parseTransactionDate(request.transactionDate());
        long amountCents = MoneyUtils.parseUsdToCents(request.purchaseAmount());

        var purchase = purchaseService.create(new CreatePurchaseCommand(
                request.description(),
                transactionDate,
                amountCents
        ));

        return new CreatePurchaseResponse(
                purchase.id(),
                purchase.description(),
                purchase.transactionDate(),
                MoneyUtils.formatCents(purchase.amountCents())
        );
    }

    @GetMapping(path = "/api/v1/purchases/{purchaseId}", produces = "application/json")
    public ConvertedPurchaseResponse getConvertedPurchase(
            @PathVariable String purchaseId,
            @RequestParam String currency
    ) {
        String normalizedPurchaseId = normalizePurchaseId(purchaseId);

        String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase();
        if (normalizedCurrency.isEmpty()) {
            throw AppException.badRequest("currency query parameter is required");
        }
        if (!CurrencySupport.isValidIso4217(normalizedCurrency)) {
            throw AppException.badRequest("currency must be a valid ISO 4217 alpha code");
        }

        var convertedPurchase = purchaseService.getConvertedPurchase(normalizedPurchaseId, normalizedCurrency);

        return new ConvertedPurchaseResponse(
                convertedPurchase.purchase().id(),
                convertedPurchase.purchase().description(),
                convertedPurchase.purchase().transactionDate(),
                MoneyUtils.formatCents(convertedPurchase.purchase().amountCents()),
                convertedPurchase.targetCurrency(),
                convertedPurchase.exchangeRate(),
                convertedPurchase.exchangeRateEffectiveDate(),
                MoneyUtils.formatCents(convertedPurchase.convertedAmountCents())
        );
    }

    @GetMapping(path = "/health", produces = "application/json")
    public HealthResponse health() {
        return new HealthResponse("ok", rateService.getStats());
    }

    private LocalDate parseTransactionDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw AppException.badRequest("transactionDate is required");
        }

        try {
            return LocalDate.parse(rawDate);
        } catch (Exception ex) {
            throw AppException.badRequest("transactionDate must be a valid date in YYYY-MM-DD format");
        }
    }

    private String normalizePurchaseId(String rawPurchaseId) {
        if (rawPurchaseId == null || rawPurchaseId.trim().isEmpty()) {
            throw AppException.badRequest("purchase id is required");
        }

        String purchaseId = rawPurchaseId.trim();

        try {
            UUID uuid = UUID.fromString(purchaseId);
            if (uuid.version() != 4) {
                throw AppException.badRequest("purchase id must be a valid UUID v4");
            }
            return purchaseId;
        } catch (IllegalArgumentException exception) {
            throw AppException.badRequest("purchase id must be a valid UUID v4");
        }
    }
}
