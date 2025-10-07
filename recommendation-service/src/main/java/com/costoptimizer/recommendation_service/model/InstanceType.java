package com.costoptimizer.recommendation_service.model;

public record InstanceType(
    String name,      // e.g., "m5.large"
    int vcpus,
    double memoryGib,
    double costPerHour,
    String family     // e.g., "General Purpose", "Memory Optimized"
) {}
