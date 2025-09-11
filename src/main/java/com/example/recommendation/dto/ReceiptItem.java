package com.example.recommendation.dto;

import java.math.BigDecimal;

public record ReceiptItem(
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {}
