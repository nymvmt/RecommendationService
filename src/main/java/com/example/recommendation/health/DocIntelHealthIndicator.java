package com.example.recommendation.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DocIntelHealthIndicator implements HealthIndicator {

    private final WebClient client;
    private final String endpoint;

    public DocIntelHealthIndicator(
            WebClient docIntelClient,
            @Value("${azure.docintel.endpoint}") String endpoint) {
        this.client = docIntelClient;
        this.endpoint = endpoint;
    }

    @Override
    public Health health() {
        // 1) 필수 설정 존재 여부
        if (endpoint == null || endpoint.isBlank()) {
            return Health.down().withDetail("reason", "missing endpoint").build();
        }
        // 2) 네트워크 도달성만 간단 체크(유료 호출 방지): base에 GET 시도 → 200/401/403/404 모두 'reachable'로 간주
        try {
            int code = client.get().uri("/").exchangeToMono(r -> {
                return reactor.core.publisher.Mono.just(r.rawStatusCode());
            }).blockOptional().orElse(0);

            boolean reachable = (code == 200 || code == 401 || code == 403 || code == 404);
            return reachable
                    ? Health.up().withDetail("reachable", true).withDetail("statusCode", code).withDetail("endpoint", endpoint).build()
                    : Health.status("DEGRADED").withDetail("statusCode", code).withDetail("endpoint", endpoint).build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).withDetail("endpoint", endpoint).build();
        }
    }
}
