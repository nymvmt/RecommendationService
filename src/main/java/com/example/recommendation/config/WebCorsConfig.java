package com.example.recommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class WebCorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        
        // 모든 도메인 허용 (와일드카드)
        cfg.setAllowedOriginPatterns(Collections.singletonList("*"));
        
        // 모든 HTTP 메서드 허용
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        
        // 모든 헤더 허용
        cfg.setAllowedHeaders(Collections.singletonList("*"));
        
        // 노출할 헤더
        cfg.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        
        // 쿠키 및 인증 정보 허용
        cfg.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (1시간)
        cfg.setMaxAge(3600L);
        
        // 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        
        return new CorsFilter(src);
    }
}