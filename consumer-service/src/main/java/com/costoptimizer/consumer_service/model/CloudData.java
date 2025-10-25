package com.costoptimizer.consumer_service.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "cloud_data") // Use a consistent collection name
public class CloudData {

    @Id
    private String id;

    // --- Core Metrics ---
    private Instant timestamp;
    private Double cpuUsage; // from cpu_usage
    private Double memoryUsage; // from memory_usage
    private Double netIo; // from net_io
    private Double diskIo; // from disk_io

    // --- Instance Configuration ---
    private String cloudProvider; // from cloud_provider
    private String region;
    private String vmType; // from vm_type
    private Integer vCPU; // from vCPU (Note: Integer)
    private Double ramGb; // from RAM_GB (Correct camelCase)
    private Double pricePerHour; // from price_per_hour (Correct camelCase)

    // --- Workload & Performance ---
    private String target; // e.g., scale_up
    private Double latencyMs; // from latency_ms
    private Double throughput;
    private Double cost; // This is likely calculated, let's keep it
    private Double utilization;

    // We can probably ignore these Kaggle columns for now:
    // power_consumption, num_executed_instructions, execution_time,
    // energy_efficiency, task_type, task_priority, task_status
}
