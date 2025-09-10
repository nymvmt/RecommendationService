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
        
        // 재시도 로직 포함
        int retryCount = 0;
        while (retryCount < 3) {
            try {
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
                
                if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                    UserResponse userResponse = apiResponse.getData();
                    log.info("UserService에서 사용자 정보 조회 성공 - userId: {}, username: {}", 
                            userId, userResponse.getUsername());
                    return userResponse;
                }
                
            } catch (Exception e) {
                retryCount++;
                log.warn("UserService 호출 실패 - {}회차 재시도, userId: {}", retryCount, userId, e);
                
                if (retryCount < 3) {
                    try {
                        Thread.sleep(500); // 0.5초 대기 후 재시도
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("UserService 재시도 실패 - userId: {}", userId);
        return null;
    }
    
    /**
     * 모든 사용자 목록 조회
     * user-service의 GET /users API 호출
     */
    public Object getAllUsers() {
        log.info("UserService에서 모든 사용자 목록 조회 시작");
        
        // 재시도 로직 포함
        int retryCount = 0;
        while (retryCount < 3) {
            try {
                WebClient webClient = webClientBuilder
                        .baseUrl(userServiceUrl)
                        .defaultHeader("X-API-Key", apiKey)
                        .defaultHeader("User-Agent", "recommendation-service/1.0")
                        .build();
                
                Object apiResponse = webClient
                        .get()
                        .uri("/users")
                        .retrieve()
                        .bodyToMono(Object.class)
                        .block();
                
                if (apiResponse != null) {
                    log.info("UserService에서 모든 사용자 목록 조회 성공");
                    return apiResponse;
                }
                
            } catch (Exception e) {
                retryCount++;
                log.warn("UserService 모든 사용자 목록 호출 실패 - {}회차 재시도", retryCount, e);
                
                if (retryCount < 3) {
                    try {
                        Thread.sleep(500); // 0.5초 대기 후 재시도
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("UserService 모든 사용자 목록 재시도 실패");
        throw new RuntimeException("사용자 목록 조회에 실패했습니다");
    }
}
