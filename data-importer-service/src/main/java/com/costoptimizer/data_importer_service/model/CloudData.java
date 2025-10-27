package com.costoptimizer.data_importer_service.model;

import java.time.Instant;

import lombok.Data;

@Data
public class CloudData {
    private String id;

    private Instant timestamp;
    private Double cpuUsage; // from cpu_usage
    private Double memoryUsage; // from memory_usage
    private Double netIo; // from net_io
    private Double diskIo; // from disk_io

    // --- Instance Configuration ---
    private String cloudProvider; // from cloud_provider
    private String region;
    private String vmType; // from vm_type
    private Integer vCPU; // from vCPU
    private Double ramGb; // from RAM_GB
    private Double pricePerHour; // from price_per_hour

    // --- Workload & Performance ---
    private String target; // e.g., scale_up
    private Double latencyMs; // from latency_ms
    private Double throughput;
    private Double cost; 
    private Double utilization;
}
