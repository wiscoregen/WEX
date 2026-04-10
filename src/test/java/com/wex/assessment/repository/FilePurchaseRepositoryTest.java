package com.wex.assessment.repository;

import com.wex.assessment.TestSupport;
import com.wex.assessment.config.AppProperties;
import com.wex.assessment.domain.Purchase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FilePurchaseRepositoryTest {

    @Test
    void savePersistsPurchaseAcrossRepositoryInstances() {
        AppProperties properties = TestSupport.newProperties("wex-purchase-repository-tests");
        FilePurchaseRepository repository = new FilePurchaseRepository(properties, TestSupport.newObjectMapper());
        Purchase purchase = new Purchase("550e8400-e29b-41d4-a716-446655440000", "Lunch", LocalDate.parse("2025-04-10"), 1_235L);

        repository.save(purchase);

        FilePurchaseRepository reloaded = new FilePurchaseRepository(properties, TestSupport.newObjectMapper());
        assertThat(reloaded.findById(purchase.id())).contains(purchase);
    }

    @Test
    void saveOverwritesExistingPurchaseId() {
        AppProperties properties = TestSupport.newProperties("wex-purchase-repository-tests");
        FilePurchaseRepository repository = new FilePurchaseRepository(properties, TestSupport.newObjectMapper());

        repository.save(new Purchase("550e8400-e29b-41d4-a716-446655440000", "Lunch", LocalDate.parse("2025-04-10"), 1_235L));
        repository.save(new Purchase("550e8400-e29b-41d4-a716-446655440000", "Updated lunch", LocalDate.parse("2025-04-11"), 2_000L));

        assertThat(repository.findById("550e8400-e29b-41d4-a716-446655440000"))
                .contains(new Purchase("550e8400-e29b-41d4-a716-446655440000", "Updated lunch", LocalDate.parse("2025-04-11"), 2_000L));
    }

    @Test
    void findByIdReturnsEmptyWhenPurchaseDoesNotExist() {
        AppProperties properties = TestSupport.newProperties("wex-purchase-repository-tests");
        FilePurchaseRepository repository = new FilePurchaseRepository(properties, TestSupport.newObjectMapper());

        assertThat(repository.findById("550e8400-e29b-41d4-a716-446655440000")).isEmpty();
    }
}
