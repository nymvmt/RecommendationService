package com.example.recommendation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${azure.docintel.endpoint}")
    private String endpoint;

    @Value("${azure.docintel.key}")
    private String key;

    @Bean
    public WebClient docIntelClient() {
        return WebClient.builder()
                .baseUrl(endpoint)
                .defaultHeader("Ocp-Apim-Subscription-Key", key)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();
    }
}