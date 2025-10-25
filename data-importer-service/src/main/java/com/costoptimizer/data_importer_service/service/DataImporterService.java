package com.costoptimizer.data_importer_service.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.costoptimizer.data_importer_service.model.CloudData;

@Service
public class DataImporterService implements CommandLineRunner {

    @Value("${topic.name}")
    private String topicName;

    @Autowired
    private KafkaTemplate<String, CloudData> kafkaTemplate;

    // The exact timestamp format from your dataset
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting data import from multi_cloud_dataset.csv...");

        String csvFile = "multi_cloud_dataset.csv"; // Correct filename
        String line = "";
        String csvSplitBy = ",";
        int lineCount = 0;
        int skippedLines = 0;

        // The timestamp format is "yyyy-MM-dd HH:mm:ss"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            // Read the header line to get column indexes
            String headerLine = br.readLine();
            if (headerLine == null) {
                System.err.println("Error: CSV file is empty or header is missing.");
                return;
            }
            Map<String, Integer> headers = new HashMap<>();
            String[] headerParts = headerLine.split(csvSplitBy);
            for (int i = 0; i < headerParts.length; i++) {
                // Store header names without leading/trailing spaces
                headers.put(headerParts[i].trim().toLowerCase(), i); // Use lowercase for easier lookup
            }

            // Verify essential headers exist
            List<String> essentialHeaders = List.of("timestamp", "cpu_usage", "memory_usage", "net_io", "disk_io",
                                                    "cloud_provider", "region", "vm_type", "vcpu", "ram_gb",
                                                    "price_per_hour", "target", "latency_ms", "throughput", "cost", "utilization");
            for (String essential : essentialHeaders) {
                if (!headers.containsKey(essential)) {
                    System.err.println("Error: Essential header '" + essential + "' not found in CSV.");
                    return;
                }
            }


            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    skippedLines++;
                    continue;
                }

                String[] data = line.split(csvSplitBy, -1); // Use -1 limit to keep trailing empty strings

                // Basic validation: Check if row has expected number of columns
                if (data.length != headerParts.length) {
                System.err.println("Warning: Skipping malformed line (expected " + headerParts.length + " columns, got " + data.length + "): " + line);
                skippedLines++;
                continue;
                }

                try {
                    CloudData cloudData = new CloudData();
                    cloudData.setId(UUID.randomUUID().toString());

                    // Parse using header map (case-insensitive)
                    LocalDateTime ldt = LocalDateTime.parse(data[headers.get("timestamp")], formatter);
                    cloudData.setTimestamp(ldt.toInstant(ZoneOffset.UTC));

                    cloudData.setCpuUsage(parseDouble(data[headers.get("cpu_usage")]));
                    cloudData.setMemoryUsage(parseDouble(data[headers.get("memory_usage")]));
                    cloudData.setNetIo(parseDouble(data[headers.get("net_io")]));
                    cloudData.setDiskIo(parseDouble(data[headers.get("disk_io")]));
                    cloudData.setCloudProvider(data[headers.get("cloud_provider")]);
                    cloudData.setRegion(data[headers.get("region")]);
                    cloudData.setVmType(data[headers.get("vm_type")]);
                    cloudData.setVCPU(parseInteger(data[headers.get("vcpu")]));
                    cloudData.setRamGb(parseDouble(data[headers.get("ram_gb")])); // Correct field name
                    cloudData.setPricePerHour(parseDouble(data[headers.get("price_per_hour")])); // Correct field name
                    cloudData.setTarget(data[headers.get("target")]);
                    cloudData.setLatencyMs(parseDouble(data[headers.get("latency_ms")]));
                    cloudData.setThroughput(parseDouble(data[headers.get("throughput")]));
                    cloudData.setCost(parseDouble(data[headers.get("cost")]));
                    cloudData.setUtilization(parseDouble(data[headers.get("utilization")]));

                    // Send to Kafka
                    kafkaTemplate.send(topicName, cloudData);
                    lineCount++;

                } catch (NumberFormatException | DateTimeParseException e) {
                System.err.println("Warning: Skipping line due to parsing error (" + e.getMessage() + "): " + line);
                skippedLines++;
                } catch (Exception e) {
                System.err.println("Warning: Skipping line due to unexpected error (" + e.getMessage() + "): " + line);
                skippedLines++;
                e.printStackTrace(); // Print stack trace for unexpected errors
                }
                if (lineCount >= 1000) { // Keep the limit for now
                    System.out.println("Reached import limit of 1000 records.");
                    break;
                }
            }
            System.out.println("✅ Successfully imported and published " + lineCount + " records to Kafka.");
            if (skippedLines > 0) {
                System.out.println("⚠️ Skipped " + skippedLines + " lines due to formatting or parsing issues.");
            }

        } catch (Exception e) {
            System.err.println("Error while reading multi_cloud_dataset.csv: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper methods to handle potential empty strings before parsing
    private Double parseDouble(String value) {
        return (value == null || value.trim().isEmpty()) ? null : Double.parseDouble(value.trim());
    }

    private Integer parseInteger(String value) {
        return (value == null || value.trim().isEmpty()) ? null : Integer.parseInt(value.trim());
    }
}
