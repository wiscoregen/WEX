package com.wex.assessment.treasury;

import com.wex.assessment.config.AppProperties;
import com.wex.assessment.error.AppException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class JavaHttpTreasuryTransport implements TreasuryTransport {

    private final HttpClient httpClient;
    private final AppProperties appProperties;

    public JavaHttpTreasuryTransport(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(appProperties.getTreasury().getRequestTimeout())
                .build();
    }

    @Override
    public String get(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(appProperties.getTreasury().getRequestTimeout())
                    .header("Accept", "application/json")
                    .header("User-Agent", "wex-purchase-service/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw AppException.internal("call treasury API",
                        new IllegalStateException("treasury API returned status " + response.statusCode()));
            }
            return response.body();
        } catch (IllegalArgumentException ex) {
            throw AppException.internal("call treasury API", ex);
        } catch (IOException ex) {
            throw AppException.internal("call treasury API", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AppException.internal("call treasury API", ex);
        }
    }
}
