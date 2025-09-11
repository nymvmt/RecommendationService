package com.example.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public record AssignItemsResponse(
        int people,
        int itemCount,              // 입력 아이템 총 개수
        int assignedCount,          // 실제 배정된 개수(필터 후)
        BigDecimal totalAssigned,   // 배정된 금액 합
        List<AssignmentCard> cards  // 카드별 결과
) {}
