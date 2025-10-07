package com.costoptimizer.consumer_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.costoptimizer.consumer_service.model.CostData;
import com.costoptimizer.consumer_service.repository.CostDataRepository;

@Service
public class BillingDataConsumer {
    @Autowired
    private CostDataRepository repository;

    @KafkaListener(topics = "${topic.name}", groupId = "cost-optimizer-group")
    public void listen(CostData data) {

        repository.save(data);

        System.out.println("Received and saved data: " + data.getResourceId());
    }
}
