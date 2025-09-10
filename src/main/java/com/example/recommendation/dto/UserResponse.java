package com.example.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String userId;
    private Boolean isAdmin;
    private String username;
    private String nickname;
    // password는 보안상 응답에 포함하지 않음
}
