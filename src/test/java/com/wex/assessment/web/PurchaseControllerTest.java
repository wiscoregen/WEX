package com.wex.assessment.web;

import com.wex.assessment.TestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.assessment.config.AppProperties;
import com.wex.assessment.domain.ExchangeRate;
import com.wex.assessment.repository.FilePurchaseRepository;
import com.wex.assessment.repository.FileRateRepository;
import com.wex.assessment.service.PurchaseService;
import com.wex.assessment.service.RateService;
import com.wex.assessment.treasury.TreasuryClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PurchaseControllerTest {

    @Test
    void createAndConvertPurchaseThroughHttpContract() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        rateRepository.replaceAll(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.90", LocalDate.parse("2025-03-31"))
        ), Instant.now());

        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        String responseBody = mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Team lunch",
                                  "transactionDate": "2025-04-10",
                                  "purchaseAmount": "12.345"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purchaseAmountUsd").value("12.35"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String purchaseId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", purchaseId).queryParam("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRate").value("0.90"))
                .andExpect(jsonPath("$.exchangeRateEffectiveDate").value("2025-03-31"))
                .andExpect(jsonPath("$.convertedAmount").value("11.12"));
    }

    @Test
    void getPurchaseRejectsInvalidIsoCurrencyCode() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", "550e8400-e29b-41d4-a716-446655440000").queryParam("currency", "EURO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"));
    }

    @Test
    void getPurchaseRejectsInvalidUuidWithJsonError() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", "<id>").queryParam("currency", "EUR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"))
                .andExpect(jsonPath("$.error.message").value("purchase id must be a valid UUID v4"))
                .andExpect(jsonPath("$.error.status").value(400));
    }

    @Test
    void createPurchaseRejectsUnsupportedMediaTypeWithJsonError() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("unsupported_media_type"))
                .andExpect(jsonPath("$.error.status").value(415));
    }

    @Test
    void createPurchaseRejectsMalformedJsonWithJsonError() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"))
                .andExpect(jsonPath("$.error.status").value(400));
    }

    @Test
    void getPurchaseRejectsMissingCurrencyParameterWithJsonError() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"))
                .andExpect(jsonPath("$.error.status").value(400));
    }

    @Test
    void wrongMethodReturnsJsonError() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(put("/api/v1/purchases/{purchaseId}", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("method_not_allowed"))
                .andExpect(jsonPath("$.error.status").value(405));
    }

    @Test
    void createPurchaseRejectsBlankDescription() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        MockMvc mockMvc = buildMockMvc(objectMapper, new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper));

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "   ",
                                  "transactionDate": "2025-04-10",
                                  "purchaseAmount": "12.35"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"))
                .andExpect(jsonPath("$.error.message").value("description is required"));
    }

    @Test
    void createPurchaseRejectsLongDescription() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        MockMvc mockMvc = buildMockMvc(objectMapper, new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper));

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "%s",
                                  "transactionDate": "2025-04-10",
                                  "purchaseAmount": "12.35"
                                }
                                """.formatted("x".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("description must not exceed 50 characters"));
    }

    @Test
    void createPurchaseRejectsInvalidTransactionDate() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        MockMvc mockMvc = buildMockMvc(objectMapper, new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper));

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Lunch",
                                  "transactionDate": "2025-02-30",
                                  "purchaseAmount": "12.35"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("transactionDate must be a valid date in YYYY-MM-DD format"));
    }

    @Test
    void createPurchaseRejectsMissingPurchaseAmount() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        MockMvc mockMvc = buildMockMvc(objectMapper, new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper));

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Lunch",
                                  "transactionDate": "2025-04-10"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("purchaseAmount is required"));
    }

    @Test
    void getPurchaseReturnsUsdWithoutTreasuryRateLookup() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        String purchaseId = createPurchase(mockMvc, objectMapper, "Team lunch", "2025-04-10", "\"12.35\"");

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", purchaseId).queryParam("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRate").value("1"))
                .andExpect(jsonPath("$.exchangeRateEffectiveDate").value("2025-04-10"))
                .andExpect(jsonPath("$.convertedAmount").value("12.35"));
    }

    @Test
    void getPurchaseReturnsPurchaseNotFoundError() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        MockMvc mockMvc = buildMockMvc(objectMapper, new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper));

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", "550e8400-e29b-41d4-a716-446655440000").queryParam("currency", "USD"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("purchase_not_found"))
                .andExpect(jsonPath("$.error.status").value(404));
    }

    @Test
    void getPurchaseReturnsUnsupportedCurrencyForTreasuryCatalogMiss() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        String purchaseId = createPurchase(mockMvc, objectMapper, "Team lunch", "2025-04-10", "\"12.35\"");

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", purchaseId).queryParam("currency", "AOA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("unsupported_currency"))
                .andExpect(jsonPath("$.error.status").value(400));
    }

    @Test
    void getPurchaseReturnsNoRateAvailableWhenSupportedCurrencyHasNoEligibleRate() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        String purchaseId = createPurchase(mockMvc, objectMapper, "Team lunch", "2025-04-10", "\"12.35\"");

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", purchaseId).queryParam("currency", "EUR"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("no_rate_available"))
                .andExpect(jsonPath("$.error.status").value(422));
    }

    @Test
    void getPurchaseRejectsBlankCurrencyParameter() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        MockMvc mockMvc = buildMockMvc(objectMapper, new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper));

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", "550e8400-e29b-41d4-a716-446655440000").queryParam("currency", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("currency query parameter is required"));
    }

    @Test
    void getPurchaseRejectsUnsupportedAcceptHeader() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        String purchaseId = createPurchase(mockMvc, objectMapper, "Team lunch", "2025-04-10", "\"12.35\"");

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", purchaseId)
                        .queryParam("currency", "USD")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.error.code").value("not_acceptable"))
                .andExpect(jsonPath("$.error.status").value(406));
    }

    @Test
    void healthReturnsCacheStatistics() throws Exception {
        ObjectMapper objectMapper = TestSupport.newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(TestSupport.newProperties("wex-web-tests"), objectMapper);
        rateRepository.replaceAll(List.of(
                new ExchangeRate("EUR", "Belgium", "Euro", "Belgium-Euro", "0.90", LocalDate.parse("2025-03-31")),
                new ExchangeRate("JPY", "Japan", "Yen", "Japan-Yen", "149.85", LocalDate.parse("2025-03-31"))
        ), Instant.parse("2025-04-01T00:00:00Z"));
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.rates.currencyCount").value(2))
                .andExpect(jsonPath("$.rates.rateCount").value(2))
                .andExpect(jsonPath("$.rates.lastRefreshedUtc").value("2025-04-01T00:00:00Z"));
    }

    private MockMvc buildMockMvc(ObjectMapper objectMapper, FileRateRepository rateRepository) {
        AppProperties properties = TestSupport.newProperties("wex-web-tests");
        FilePurchaseRepository purchaseRepository = new FilePurchaseRepository(properties, objectMapper);
        TreasuryClient treasuryClient = () -> List.of();
        RateService rateService = new RateService(rateRepository, treasuryClient, Clock.systemUTC());
        PurchaseService purchaseService = new PurchaseService(purchaseRepository, rateService);

        PurchaseController controller = new PurchaseController(purchaseService, rateService);

        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler(objectMapper))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private String createPurchase(MockMvc mockMvc, ObjectMapper objectMapper, String description, String transactionDate, String amountJson) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "%s",
                                  "transactionDate": "%s",
                                  "purchaseAmount": %s
                                }
                                """.formatted(description, transactionDate, amountJson)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("id").asText();
    }
}
