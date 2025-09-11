package com.example.recommendation.dto;

import jakarta.validation.constraints.NotBlank;

public record ReceiptOcrRequest(@NotBlank String imageUrl) {}
