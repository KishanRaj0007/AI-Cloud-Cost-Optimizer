package com.costoptimizer.consumer_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.costoptimizer.consumer_service.model.CloudData;

@Repository
public interface CloudDataRepository extends MongoRepository<CloudData, String> {

}
