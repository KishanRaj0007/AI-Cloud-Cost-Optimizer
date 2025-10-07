package com.costoptimizer.recommendation_service.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.costoptimizer.recommendation_service.db.InstanceKnowledgeBase;
import com.costoptimizer.recommendation_service.model.CostData;
import com.costoptimizer.recommendation_service.model.InstanceType;
import com.costoptimizer.recommendation_service.model.Recommendation;
import com.costoptimizer.recommendation_service.repository.CostDataRepository;

@Service
public class RecommendationService {
    @Autowired
    private CostDataRepository costDataRepository;

    @Autowired
    private InstanceKnowledgeBase knowledgeBase;

    public List<Recommendation> generateRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();

        // 1. Fetch all data and group by resourceId
        Map<String, List<CostData>> costsByResource = costDataRepository.findAll().stream()
                .filter(cost -> cost.getResourceId() != null)
                .collect(Collectors.groupingBy(CostData::getResourceId));

        // 2. Analyze each resource's history
        for (Map.Entry<String, List<CostData>> entry : costsByResource.entrySet()) {
            String resourceId = entry.getKey();
            List<CostData> history = entry.getValue();
            if (history.isEmpty()) continue;

            // 3. Profile the workload by averaging its performance metrics
            String currentInstanceTypeName = history.get(0).getInstanceType();
            if (currentInstanceTypeName == null) {
                System.out.println("Skipping resource " + resourceId + " because its instance type is missing.");
                continue; // Move to the next resource in the loop
            }
            double avgCpu = history.stream()
                .filter(c -> c.getCpuUtilization() != null) // <-- Add this line to ignore old records
                .mapToDouble(CostData::getCpuUtilization)
                .average()
                .orElse(0.0);

            double avgMem = history.stream()
                .filter(c -> c.getMemoryUtilization() != null) // <-- Add this line too
                .mapToDouble(CostData::getMemoryUtilization)
                .average()
                .orElse(0.0);

            InstanceType currentInstance = knowledgeBase.getInstance(currentInstanceTypeName);
            if (currentInstance == null) continue; // Skip if we don't know about this instance type

            // 4. The Matching Algorithm
            if (avgCpu > 70 && !currentInstance.family().equals("Compute Optimized")) {
                // Find a cheaper, better-fitting Compute Optimized instance
                knowledgeBase.getAllInstances().stream()
                        .filter(candidate -> candidate.family().equals("Compute Optimized"))
                        .filter(candidate -> candidate.costPerHour() < currentInstance.costPerHour())
                        .min(Comparator.comparingDouble(InstanceType::costPerHour))
                        .ifPresent(bestFit -> {
                            double savings = (currentInstance.costPerHour() - bestFit.costPerHour()) * 24 * 30;
                            recommendations.add(new Recommendation(resourceId, currentInstance.name(), bestFit.name(), "Workload is CPU-bound.", savings));
                        });
            } else if (avgMem > 70 && !currentInstance.family().equals("Memory Optimized")) {
                // Find a cheaper, better-fitting Memory Optimized instance
                knowledgeBase.getAllInstances().stream()
                        .filter(candidate -> candidate.family().equals("Memory Optimized"))
                        .filter(candidate -> candidate.memoryGib() >= currentInstance.memoryGib())
                        .filter(candidate -> candidate.costPerHour() < currentInstance.costPerHour())
                        .min(Comparator.comparingDouble(InstanceType::costPerHour))
                        .ifPresent(bestFit -> {
                            double savings = (currentInstance.costPerHour() - bestFit.costPerHour()) * 24 * 30;
                            recommendations.add(new Recommendation(resourceId, currentInstance.name(), bestFit.name(), "Workload is memory-bound.", savings));
                        });
            }
        }
        return recommendations;
    }
}
