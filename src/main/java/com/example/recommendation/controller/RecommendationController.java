package com.example.recommendation.controller;

import com.example.recommendation.client.UserServiceClient;
import com.example.recommendation.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {
    
    private final UserServiceClient userServiceClient;
    
    /**
     * 간단한 테스트 엔드포인트
     * GET /recommendations/test
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("recommendation-service is working!");
    }
    
    /**
     * 사용자 정보 조회 테스트
     * GET /recommendations/user/{userId}
     */
    @GetMapping("/user/{userId:.+}")
    public ResponseEntity<UserResponse> getUserInfo(@PathVariable String userId) {
        log.info("사용자 정보 조회 요청 - userId: {}", userId);
        
        UserResponse userResponse = userServiceClient.getUserById(userId);
        
        if (userResponse != null) {
            log.info("사용자 정보 조회 성공 - userId: {}, username: {}", 
                    userId, userResponse.getUsername());
            return ResponseEntity.ok(userResponse);
        } else {
            log.warn("사용자 정보를 찾을 수 없음 - userId: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }
}
