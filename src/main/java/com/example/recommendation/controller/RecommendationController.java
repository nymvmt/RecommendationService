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
        log.info("테스트 엔드포인트 호출됨");
        return ResponseEntity.ok("recommendation-service is working!");
    }
    
    /**
     * 모든 사용자 목록 조회 (user-service의 GET /users 호출)
     * GET /recommendations/users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        log.info("모든 사용자 목록 조회 요청");
        
        try {
            // user-service의 GET /users API 호출
            Object usersResponse = userServiceClient.getAllUsers();
            
            log.info("사용자 목록 조회 성공");
            return ResponseEntity.ok(usersResponse);
        } catch (Exception e) {
            log.error("사용자 목록 조회 실패", e);
            return ResponseEntity.status(500).body("사용자 목록 조회에 실패했습니다: " + e.getMessage());
        }
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
