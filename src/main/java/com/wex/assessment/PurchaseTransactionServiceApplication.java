package com.wex.assessment;

import com.wex.assessment.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Clock;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PurchaseTransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PurchaseTransactionServiceApplication.class, args);
    }

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
