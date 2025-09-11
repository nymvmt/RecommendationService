package com.example.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReceiptOcrResponse(
        String merchant,
        BigDecimal total,
        List<ReceiptItem> items
) {}
