package com.costoptimizer.recommendation_service.db;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.costoptimizer.recommendation_service.model.InstanceType;

import jakarta.annotation.PostConstruct;

@Component
public class InstanceKnowledgeBase {
    private final Map<String, InstanceType> instanceTypes = new ConcurrentHashMap<>();

    // This method runs once when the application starts up
    @PostConstruct
    private void init() {
        // General Purpose
        instanceTypes.put("t3.large", new InstanceType("t3.large", 2, 8.0, 0.0832, "General Purpose"));

        // Compute Optimized
        instanceTypes.put("c5.large", new InstanceType("c5.large", 2, 4.0, 0.085, "Compute Optimized"));
        instanceTypes.put("c5.xlarge", new InstanceType("c5.xlarge", 4, 8.0, 0.17, "Compute Optimized"));

        // Memory Optimized
        instanceTypes.put("r5.large", new InstanceType("r5.large", 2, 16.0, 0.126, "Memory Optimized"));
    }

    public InstanceType getInstance(String name) {
        return instanceTypes.get(name);
    }
    
    public Collection<InstanceType> getAllInstances() {
        return instanceTypes.values();
    }
}
