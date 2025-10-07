package com.costoptimizer.forecasting_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.costoptimizer.forecasting_service.model.CostData;

public interface CostDataRepository extends MongoRepository<CostData, String> {

}
