package com.costoptimizer.consumer_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.costoptimizer.consumer_service.model.CloudData;
import com.costoptimizer.consumer_service.repository.CloudDataRepository;

@Service
public class CloudDataConsumer {
    @Autowired
    private CloudDataRepository repository;

    @KafkaListener(topics = "${topic.name}", groupId = "cloud-data-group")
    public void listen(CloudData data) {
        repository.save(data);
        // We'll log every 100th message to avoid spamming the console
        String id = data.getId();
        if(id != null && id.length() >= 2) {
            int hexValue = Integer.parseInt(id.substring(0, 2), 16);
            if (hexValue % 100 == 0) {
                System.out.println("Received and saved data for VM: " + data.getVmType());
            }
        }
        else{
            System.out.println("Id is not valid");
        }
    }
}
