package com.wex.assessment.treasury;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.assessment.config.AppProperties;
import com.wex.assessment.domain.ExchangeRate;
import com.wex.assessment.error.AppException;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

@Component
public class HttpTreasuryClient implements TreasuryClient {

    private final ObjectMapper objectMapper;
    private final TreasuryTransport transport;
    private final AppProperties appProperties;

    public HttpTreasuryClient(ObjectMapper objectMapper, TreasuryTransport transport, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.transport = transport;
        this.appProperties = appProperties;
    }

    @Override
    public List<ExchangeRate> fetchRates() {
        URI nextUri = buildInitialUri();
        Map<String, ExchangeRate> uniqueRates = new LinkedHashMap<>();

        while (nextUri != null) {
            FiscalResponse response = readPage(nextUri);
            List<FiscalRecord> records = response.data() == null ? List.of() : response.data();

            for (FiscalRecord record : records) {
                String currencyCode = TreasuryCurrencyCatalog.resolveCurrencyCode(
                        record.country(),
                        record.currency(),
                        record.countryCurrencyDescriptor()
                ).orElse(null);

                if (currencyCode == null) {
                    continue;
                }

                LocalDate effectiveDate;
                try {
                    effectiveDate = LocalDate.parse(record.effectiveDate());
                } catch (Exception ex) {
                    continue;
                }

                String key = currencyCode + "|" + effectiveDate;
                uniqueRates.putIfAbsent(
                        key,
                        new ExchangeRate(
                                currencyCode,
                                record.country(),
                                record.currency(),
                                record.countryCurrencyDescriptor(),
                                record.exchangeRate(),
                                effectiveDate
                        )
                );
            }

            nextUri = resolveNextUri(nextUri, response.links() == null ? null : response.links().next());
        }

        if (uniqueRates.isEmpty()) {
            throw AppException.internal("refresh treasury rate cache", new IllegalStateException("treasury API returned no mappable exchange rates"));
        }

        List<ExchangeRate> rates = new ArrayList<>(uniqueRates.values());
        rates.sort(Comparator
                .comparing(ExchangeRate::currencyCode)
                .thenComparing(ExchangeRate::effectiveDate, Comparator.reverseOrder()));
        return rates;
    }

    private URI buildInitialUri() {
        String baseUri = appProperties.getTreasury().getBaseUrl().toString();
        String separator = baseUri.contains("?") ? "&" : "?";

        return URI.create(
                baseUri
                        + separator
                        + "fields=country,currency,country_currency_desc,effective_date,exchange_rate"
                        + "&sort=-effective_date"
                        + "&page%5Bsize%5D=1000"
        );
    }

    private FiscalResponse readPage(URI uri) {
        try {
            String response = transport.get(uri);
            return objectMapper.readValue(response, FiscalResponse.class);
        } catch (AppException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            throw AppException.internal("decode treasury response", ex);
        }
    }

    private URI resolveNextUri(URI currentUri, String nextLink) {
        if (nextLink == null || nextLink.isBlank()) {
            return null;
        }

        String trimmed = nextLink.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return URI.create(trimmed);
        }

        if (trimmed.startsWith("&") || trimmed.startsWith("?")) {
            return appendRelativeQuery(currentUri, trimmed);
        }

        if (trimmed.startsWith("/")) {
            return appProperties.getTreasury().getBaseUrl().resolve(trimmed);
        }

        return currentUri.resolve(trimmed);
    }

    private URI appendRelativeQuery(URI currentUri, String nextLink) {
        String normalizedQuery = nextLink.replaceFirst("^[&?]+", "");

        String base = requestPrefixWithoutPaging(currentUri);
        String separator = base.contains("?") ? "&" : "?";
        return URI.create(base + separator + normalizedQuery);
    }

    private String requestPrefixWithoutPaging(URI uri) {
        StringBuilder builder = new StringBuilder()
                .append(uri.getScheme())
                .append("://")
                .append(uri.getRawAuthority())
                .append(uri.getRawPath());

        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return builder.toString();
        }

        List<String> retainedParameters = new ArrayList<>();
        for (String parameter : rawQuery.split("&")) {
            if (parameter.startsWith("page%5B") || parameter.startsWith("page[")) {
                continue;
            }
            if (!parameter.isBlank()) {
                retainedParameters.add(parameter);
            }
        }

        if (!retainedParameters.isEmpty()) {
            builder.append('?').append(String.join("&", retainedParameters));
        }

        return builder.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FiscalResponse(
            @JsonProperty("data") List<FiscalRecord> data,
            @JsonProperty("links") FiscalLinks links
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FiscalLinks(
            @JsonProperty("next") String next
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FiscalRecord(
            @JsonProperty("country") String country,
            @JsonProperty("currency") String currency,
            @JsonProperty("country_currency_desc") String countryCurrencyDescriptor,
            @JsonProperty("effective_date") String effectiveDate,
            @JsonProperty("exchange_rate") String exchangeRate
    ) {
    }
}
