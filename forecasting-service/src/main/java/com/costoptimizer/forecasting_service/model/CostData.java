package com.costoptimizer.forecasting_service.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "billing_records")
public class CostData {
    @Id
    private String recordId;
    private String accountId;
    private String serviceName;
    private String region;
    private String resourceId;
    private String usageType;
    private double cost;
    private String currency;
    private Instant timestamp;
    private String instanceType; // e.g., "t3.large"
    private Double cpuUtilization; // e.g., 85.5 (as a percentage)
    private Double memoryUtilization; // e.g., 40.2 (as a percentage)
}
