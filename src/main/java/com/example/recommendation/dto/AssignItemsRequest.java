package com.example.recommendation.dto;

import java.util.List;

public record AssignItemsRequest(
        int people,
        Long seed,
        List<ReceiptItem> items,
        List<Participant> participants  // ← 추가
) {}
