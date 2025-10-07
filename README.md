*AI-Powered Cloud Cost Optimizer*
Overview
This project is a backend system designed to help companies reduce their cloud spending on platforms like AWS, GCP, or Azure. It moves beyond simple cost reporting by using a microservice architecture and machine learning to proactively forecast future costs, detect spending anomalies in real-time, and recommend specific, actionable changes to optimize resource allocation.

The entire system is built on an event-driven architecture using Apache Kafka to stream billing and performance data between independent, specialized services.

The Problem and Our Solution
The Problem
Companies regularly overspend by 20-40% on cloud resources due to overprovisioning, idle resources, and inefficient scaling. Current solutions like AWS Cost Explorer are reactive; they provide dashboards to show you what you have already spent, but they don't help you prevent future overspending.

Our Solution
This project provides a proactive, AI-driven solution. It acts as an intelligent financial advisor for cloud infrastructure by providing three key capabilities that traditional dashboards lack:

Forecasting: It uses time-series ML models like ARIMA to predict future spending, allowing for better budget planning.

Real-Time Anomaly Detection: It uses models like Isolation Forest to monitor the live stream of cost data and immediately alert on unusual spikes that deviate from normal patterns.

Intelligent Recommendations: It analyzes the performance profile of resources (CPU, Memory) to provide "right-sizing" recommendations, suggesting cheaper instance types that better match the workload. This provides direct, actionable advice to reduce costs.

Features
Real-Time Data Ingestion: A scalable data pipeline built with Spring Boot and Apache Kafka to ingest and process billing data.

AI-Powered Cost Forecasting: An API endpoint that provides an N-day forecast of future cloud costs based on historical trends.

Real-Time Anomaly Detection: A streaming service that monitors costs as they happen and prints alerts to the console when an anomaly is detected.

Intelligent Right-Sizing Recommendations: An API endpoint that returns a list of specific EC2 instances that are overprovisioned, along with a recommendation for a more cost-effective instance type and the estimated monthly savings.

Tech Stack
Backend: Java 17, Spring Boot 3

Messaging/Streaming: Apache Kafka

Database: MongoDB

Machine Learning Libraries:

Forecasting: com.github.signaflo:timeseries (ARIMA Model)

Anomaly Detection: io.github.haifengl:smile-core (Isolation Forest)

Containerization: Docker, Docker Compose

Build Tool: Apache Maven

System Architecture
The system is composed of four independent microservices that communicate through a central Kafka messaging topic and use MongoDB for persistent storage.

(Upload your LLD image to your GitHub repo and update the path below)
![System LLD](./path/to/your/lld_diagram_image.png)

Project Demo
Anomaly Detection in Action
The system successfully identifies an anomalous cost spike in the real-time data stream.

(Upload your anomaly screenshot and update the path below)
![Anomaly Detected](./path/to/your/anomaly_screenshot.png)

Right-Sizing Recommendations
The recommendation engine analyzes workload profiles and suggests more cost-effective instance types.

(Upload your recommendation screenshot and update the path below)
![Right-Sizing Recommendation](./path/to/your/recommendation_screenshot.png)

Getting Started
Follow these instructions to get the project running on your local machine.

Prerequisites
Java 17 JDK

Apache Maven

Docker Desktop

Running the Project
The project contains four microservices that must be run in the correct order.

Clone the repository:

git clone <your-repo-url>

Start the infrastructure:
Navigate to the root directory of the project (where docker-compose.yml is located) and run:

docker-compose up -d

This will start the Kafka, Zookeeper, and MongoDB containers.

Start the data pipeline:
Open two separate terminals.

In the first terminal, navigate to the producer-service folder and run: mvn spring-boot:run

In the second terminal, navigate to the consumer-service folder and run: mvn spring-boot:run

Let these run for at least 1-2 minutes to generate sufficient historical data.

Start the analytical services:
You can now start the other services in any order in new terminals.

Navigate to forecasting-service and run: mvn spring-boot:run

Navigate to anomaly-detection-service and run: mvn spring-boot:run

Navigate to recommendation-service and run: mvn spring-boot:run

Test the API Endpoints:

Get Forecast: http://localhost:8082/api/forecast?days=7

Get Recommendations: http://localhost:8084/api/recommendations

View Anomaly Alerts: Watch the console output of the anomaly-detection-service.
