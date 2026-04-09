package com.wex.assessment.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.assessment.config.AppProperties;
import com.wex.assessment.domain.Purchase;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class FilePurchaseRepository implements PurchaseRepository {

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, Purchase> purchasesById = new LinkedHashMap<>();

    public FilePurchaseRepository(AppProperties properties, ObjectMapper objectMapper) {
        this.filePath = Path.of(properties.getDataDir(), "purchases.json");
        this.objectMapper = objectMapper;

        PurchaseFile purchaseFile = FileStorageSupport.readOrDefault(
                objectMapper,
                filePath,
                PurchaseFile.class,
                () -> new PurchaseFile(List.of())
        );

        for (Purchase purchase : purchaseFile.purchases()) {
            purchasesById.put(purchase.id(), purchase);
        }
    }

    @Override
    public Purchase save(Purchase purchase) {
        lock.writeLock().lock();
        try {
            purchasesById.put(purchase.id(), purchase);
            persistLocked();
            return purchase;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Purchase> findById(String purchaseId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(purchasesById.get(purchaseId));
        } finally {
            lock.readLock().unlock();
        }
    }

    private void persistLocked() {
        List<Purchase> purchases = new ArrayList<>(purchasesById.values());
        purchases.sort(Comparator.comparing(Purchase::id));
        FileStorageSupport.writeAtomically(objectMapper, filePath, new PurchaseFile(purchases));
    }

    private record PurchaseFile(List<Purchase> purchases) {
    }
}

