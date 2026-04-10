package com.wex.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wex.assessment.config.AppProperties;

import java.net.URI;
import java.nio.file.Path;

public final class TestSupport {

    private TestSupport() {
    }

    public static ObjectMapper newObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static AppProperties newProperties(String prefix) {
        AppProperties properties = new AppProperties();
        properties.setDataDir(Path.of(System.getProperty("java.io.tmpdir"), prefix + "-" + System.nanoTime()).toString());
        properties.getTreasury().setBaseUrl(URI.create("https://example.test/"));
        return properties;
    }
}
