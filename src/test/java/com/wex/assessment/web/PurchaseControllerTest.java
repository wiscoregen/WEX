package com.wex.assessment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

import java.net.URI;
import java.nio.file.Path;
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
        ObjectMapper objectMapper = newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(newProperties(), objectMapper);
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
        ObjectMapper objectMapper = newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(newProperties(), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", "purchase-123").queryParam("currency", "EURO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"));
    }

    @Test
    void createPurchaseRejectsUnsupportedMediaTypeWithJsonError() throws Exception {
        ObjectMapper objectMapper = newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(newProperties(), objectMapper);
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
        ObjectMapper objectMapper = newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(newProperties(), objectMapper);
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
        ObjectMapper objectMapper = newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(newProperties(), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(get("/api/v1/purchases/{purchaseId}", "purchase-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"))
                .andExpect(jsonPath("$.error.status").value(400));
    }

    @Test
    void wrongMethodReturnsJsonError() throws Exception {
        ObjectMapper objectMapper = newObjectMapper();
        FileRateRepository rateRepository = new FileRateRepository(newProperties(), objectMapper);
        MockMvc mockMvc = buildMockMvc(objectMapper, rateRepository);

        mockMvc.perform(put("/api/v1/purchases/{purchaseId}", "purchase-123"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("method_not_allowed"))
                .andExpect(jsonPath("$.error.status").value(405));
    }

    private MockMvc buildMockMvc(ObjectMapper objectMapper, FileRateRepository rateRepository) {
        AppProperties properties = newProperties();
        FilePurchaseRepository purchaseRepository = new FilePurchaseRepository(properties, objectMapper);
        TreasuryClient treasuryClient = () -> List.of();
        RateService rateService = new RateService(rateRepository, treasuryClient, Clock.systemUTC());
        PurchaseService purchaseService = new PurchaseService(purchaseRepository, rateService);

        PurchaseController controller = new PurchaseController(purchaseService, rateService);

        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private AppProperties newProperties() {
        AppProperties properties = new AppProperties();
        properties.setDataDir(Path.of(System.getProperty("java.io.tmpdir"), "wex-web-tests-" + System.nanoTime()).toString());
        properties.getTreasury().setBaseUrl(URI.create("https://example.test/"));
        return properties;
    }

    private ObjectMapper newObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
