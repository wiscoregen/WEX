package com.wex.assessment.config;

import com.wex.assessment.service.RateService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TreasuryCacheLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreasuryCacheLifecycle.class);

    private final RateService rateService;
    private final AppProperties appProperties;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TreasuryCacheLifecycle(RateService rateService, AppProperties appProperties) {
        this.rateService = rateService;
        this.appProperties = appProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        refreshSafely("startup");

        long refreshIntervalMillis = Math.max(1_000L, appProperties.getTreasury().getRefreshInterval().toMillis());
        scheduler.scheduleWithFixedDelay(
                () -> refreshSafely("background"),
                refreshIntervalMillis,
                refreshIntervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void refreshSafely(String source) {
        try {
            var stats = rateService.refreshCache();
            LOGGER.info(
                    "{} treasury cache refresh completed: {} currencies, {} rates",
                    source,
                    stats.currencyCount(),
                    stats.rateCount()
            );
        } catch (Exception ex) {
            LOGGER.warn("{} treasury cache refresh failed, continuing with the local cache", source, ex);
        }
    }
}

