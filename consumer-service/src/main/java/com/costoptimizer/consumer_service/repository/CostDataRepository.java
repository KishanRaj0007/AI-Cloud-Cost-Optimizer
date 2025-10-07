package com.costoptimizer.consumer_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.costoptimizer.consumer_service.model.CostData;

public interface CostDataRepository extends MongoRepository<CostData, String> {
    // By extending MongoRepository<CostData, String>, this interface automatically 
    // gets methods like save(), findAll(), findById(), etc., for our CostData object..
}
