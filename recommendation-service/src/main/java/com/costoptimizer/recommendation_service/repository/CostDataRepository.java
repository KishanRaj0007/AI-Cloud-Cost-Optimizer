package com.costoptimizer.recommendation_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.costoptimizer.recommendation_service.model.CostData;

public interface CostDataRepository extends MongoRepository<CostData, String> {

}
