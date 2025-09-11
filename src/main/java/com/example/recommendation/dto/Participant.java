package com.example.recommendation.dto;

public record Participant(
        String id,         // 선택: 사용자 식별자(없으면 null)
        String nickname    // 표시용 닉네임(필수 권장)
) {}
