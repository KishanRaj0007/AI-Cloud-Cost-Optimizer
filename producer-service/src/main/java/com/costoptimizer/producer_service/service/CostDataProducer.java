package com.costoptimizer.producer_service.service;

import com.costoptimizer.producer_service.model.CostData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@EnableScheduling
public class CostDataProducer {

    // Enum to represent different workload types
    private enum WorkloadProfile { NORMAL, CPU_BOUND, MEMORY_BOUND }

    @Value("${topic.name}")
    private String topicName;

    @Autowired
    private KafkaTemplate<String, CostData> kafkaTemplate;

    private final Random random = new Random();
    private final List<String> services = List.of("AmazonEC2"); // Focusing on EC2 for right-sizing
    private final List<String> regions = List.of("us-east-1", "us-west-2", "eu-central-1");
    private final List<String> instanceTypes = List.of("t3.large", "m5.xlarge", "c5.2xlarge", "r5.large");
    private final List<WorkloadProfile> profiles = List.of(WorkloadProfile.NORMAL, WorkloadProfile.CPU_BOUND, WorkloadProfile.MEMORY_BOUND);

    @Scheduled(fixedRate = 2000)
    public void generateAndSendCostData() {
        // Generate base data
        double cost = 0.5 + (10.0 - 0.5) * random.nextDouble();
        int daysAgo = random.nextInt(30);
        Instant historicalTimestamp = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        String instanceType = instanceTypes.get(random.nextInt(instanceTypes.size()));

        // --- NEW: Generate Performance Metrics based on a random profile ---
        WorkloadProfile profile = profiles.get(random.nextInt(profiles.size()));
        Double cpu = 0.0;
        Double memory = 0.0;

        switch (profile) {
            case CPU_BOUND:
                cpu = 70.0 + (25.0 * random.nextDouble()); // 70-95%
                memory = 20.0 + (20.0 * random.nextDouble()); // 20-40%
                break;
            case MEMORY_BOUND:
                cpu = 10.0 + (20.0 * random.nextDouble()); // 10-30%
                memory = 75.0 + (20.0 * random.nextDouble()); // 75-95%
                break;
            case NORMAL:
            default:
                cpu = 30.0 + (30.0 * random.nextDouble()); // 30-60%
                memory = 40.0 + (30.0 * random.nextDouble()); // 40-70%
                break;
        }

        CostData data = new CostData();
        data.setRecordId(UUID.randomUUID().toString());
        data.setAccountId("123456789012");
        data.setServiceName("AmazonEC2");
        data.setRegion(regions.get(random.nextInt(regions.size())));
        data.setResourceId("i-" + UUID.randomUUID().toString().substring(0, 17));
        data.setUsageType("BoxUsage:" + instanceType);
        data.setCost(Math.round(cost * 100.0) / 100.0);
        data.setCurrency("USD");
        data.setTimestamp(historicalTimestamp);
        // Set new fields
        data.setInstanceType(instanceType);
        data.setCpuUtilization(Math.round(cpu * 100.0) / 100.0);
        data.setMemoryUtilization(Math.round(memory * 100.0) / 100.0);


        kafkaTemplate.send(topicName, data);
        System.out.println("Sent " + profile + " data for instance type " + instanceType);
    }
}