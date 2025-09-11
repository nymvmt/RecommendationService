package com.example.recommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class WebCorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        
        // 모든 도메인 허용 (개발 및 프로덕션 환경 모두 지원)
        cfg.setAllowedOriginPatterns(List.of("*"));
        
        // 허용할 HTTP 메서드
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        
        // 허용할 헤더 (모든 헤더 허용)
        cfg.setAllowedHeaders(List.of("*"));
        
        // 쿠키 및 인증 정보 허용
        cfg.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (24시간)
        cfg.setMaxAge(86400L);
        
        // 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        
        return new CorsFilter(src);
    }
}