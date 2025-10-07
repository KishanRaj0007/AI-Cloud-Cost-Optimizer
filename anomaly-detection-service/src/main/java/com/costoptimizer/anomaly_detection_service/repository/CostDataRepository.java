package com.costoptimizer.anomaly_detection_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.costoptimizer.anomaly_detection_service.model.CostData;

public interface CostDataRepository extends MongoRepository<CostData, String> {
    
}
