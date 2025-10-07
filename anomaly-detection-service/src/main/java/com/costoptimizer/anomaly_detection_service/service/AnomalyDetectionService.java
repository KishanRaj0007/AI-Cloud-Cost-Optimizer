package com.costoptimizer.anomaly_detection_service.service;

import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.costoptimizer.anomaly_detection_service.model.CostData;
import com.costoptimizer.anomaly_detection_service.repository.CostDataRepository;

import jakarta.annotation.PostConstruct;
import smile.anomaly.IsolationForest;

@Service
public class AnomalyDetectionService {
    @Autowired
    private CostDataRepository repository;

    private IsolationForest model;
    private boolean isModelTrained = false;

    /**
     * This method runs automatically once, right after the service starts up.
     * It's the perfect place to train our model on historical data.
     */
    @PostConstruct
    public void trainModel() {
        System.out.println("Attempting to train anomaly detection model...");
        List<CostData> trainingData = repository.findAll();

        if (trainingData.size() < 50) { // Need a decent sample size for training
            System.out.println("Not enough historical data to train model. Need at least 50 records. Current count: " + trainingData.size());
            return;
        }

        // --- Step 1: Feature Engineering ---
        // We need to convert our CostData object into a numerical array (double[])
        // that the ML model can understand. These are our "features".
        double[][] features = trainingData.stream()
                .map(this::createFeatures)
                .toArray(double[][]::new);

        // --- Step 2: Train the Isolation Forest model ---
        model = IsolationForest.fit(features);
        isModelTrained = true;
        System.out.println("✅ Anomaly detection model trained successfully with " + trainingData.size() + " records.");
    }

    /**
     * This method listens to the Kafka topic in real-time.
     */
    @KafkaListener(topics = "${topic.name}", groupId = "anomaly-detection-group")
    public void listenForCosts(CostData data) {
        if (!isModelTrained) {
            // Don't try to predict if the model hasn't been trained yet.
            return;
        }

        // --- Step 3: Predict on live data ---
        // Convert the incoming data into the same feature format.
        double[] liveFeatures = createFeatures(data);

        // The model.predict() method returns 1 for an outlier (anomaly) and 0 for an inlier (normal).
        double score = model.score(liveFeatures);
        if (score > 0.75) { // You can tune this threshold based on your data
            System.out.printf("🚨 ANOMALY DETECTED 🚨: Service=%s, Cost=%.2f, Resource=%s, Region=%s, Score=%.3f%n",
                    data.getServiceName(), data.getCost(), data.getResourceId(), data.getRegion(), score);
        }
    }

    /**
     * A helper method to convert a CostData object into a numerical feature vector.
     * @param data The cost data record.
     * @return A double array of features.
     */
    private double[] createFeatures(CostData data) {
        return new double[]{
                data.getCost(),
                // We use hashCode() as a simple way to convert string categories to numbers.
                data.getServiceName().hashCode(),
                data.getRegion().hashCode(),
                // The hour of the day can be an important feature (e.g., costs at 3 AM might be unusual).
                data.getTimestamp().atZone(ZoneOffset.UTC).getHour()
        };
    }
}
