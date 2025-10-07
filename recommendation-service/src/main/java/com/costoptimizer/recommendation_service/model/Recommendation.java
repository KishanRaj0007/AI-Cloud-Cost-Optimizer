package com.costoptimizer.recommendation_service.model;

public record Recommendation(
    String resourceId,
    String currentInstanceType,
    String recommendedInstanceType,
    String reasoning,
    double estimatedMonthlySavings
) {}
