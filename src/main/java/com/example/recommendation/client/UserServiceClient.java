package com.example.recommendation.client;

import com.example.recommendation.dto.ApiResponse;
import com.example.recommendation.dto.UserResponse;
import org.springframework.core.ParameterizedTypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.user.url}")
    private String userServiceUrl;
    
    @Value("${services.user.api-key}")
    private String apiKey;
    
    public UserResponse getUserById(String userId) {
        log.info("UserService에서 사용자 정보 조회 시작 - userId: {}", userId);
        
        WebClient webClient = webClientBuilder
                .baseUrl(userServiceUrl)
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader("User-Agent", "recommendation-service/1.0")
                .build();
        
        ApiResponse<UserResponse> apiResponse = webClient
                .get()
                .uri("/users/{userId}", userId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {})
                .block();
        
        UserResponse userResponse = apiResponse != null && apiResponse.isSuccess() ? apiResponse.getData() : null;
        
        log.info("UserService에서 사용자 정보 조회 완료 - userId: {}, username: {}", 
                userId, userResponse != null ? userResponse.getUsername() : "null");
        
        return userResponse;
    }
}
