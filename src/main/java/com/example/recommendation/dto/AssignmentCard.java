package com.example.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public record AssignmentCard(
        int card,
        Participant participant,   // ← 추가
        List<ReceiptItem> items,
        BigDecimal subtotal,
        int count
) {}
