package com.wex.assessment.treasury;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wex.assessment.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class HttpTreasuryClientTest {

    @Test
    void fetchRatesHandlesPagingAndDeduplicatesCurrencyDatePairs() {
        AppProperties properties = new AppProperties();
        properties.getTreasury().setBaseUrl(URI.create("https://example.test/"));

        TreasuryTransport transport = uri -> {
            String rawQuery = uri.getRawQuery() == null ? "" : uri.getRawQuery();

            if ("/".equals(uri.getPath()) && !rawQuery.contains("page%5Bnumber%5D=2")) {
                return """
                    {
                      "data": [
                        {
                          "country": "Canada",
                          "currency": "Dollar",
                          "country_currency_desc": "Canada-Dollar",
                          "effective_date": "2025-03-31",
                          "exchange_rate": "1.35"
                        },
                        {
                          "country": "Belgium",
                          "currency": "Euro",
                          "country_currency_desc": "Belgium-Euro",
                          "effective_date": "2025-03-31",
                          "exchange_rate": "0.91"
                        },
                        {
                          "country": "France",
                          "currency": "Euro",
                          "country_currency_desc": "France-Euro",
                          "effective_date": "2025-03-31",
                          "exchange_rate": "0.91"
                        }
                      ],
                      "links": {
                        "next": "&page%5Bnumber%5D=2&page%5Bsize%5D=1000"
                      }
                    }
                    """;
            }

            if ("/".equals(uri.getPath()) && rawQuery.contains("page%5Bnumber%5D=2")) {
                return """
                    {
                      "data": [
                        {
                          "country": "Japan",
                          "currency": "Yen",
                          "country_currency_desc": "Japan-Yen",
                          "effective_date": "2025-03-31",
                          "exchange_rate": "149.85"
                        }
                      ],
                      "links": {}
                    }
                    """;
            }

            throw new IllegalStateException("unexpected URI " + uri);
        };

        HttpTreasuryClient client = new HttpTreasuryClient(
                new ObjectMapper()
                        .findAndRegisterModules()
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
                transport,
                properties
        );

        var rates = client.fetchRates();

        assertThat(rates).hasSize(3);
        assertThat(rates.get(0).currencyCode()).isEqualTo("CAD");
        assertThat(rates.get(1).currencyCode()).isEqualTo("EUR");
        assertThat(rates.get(2).currencyCode()).isEqualTo("JPY");
    }
}
