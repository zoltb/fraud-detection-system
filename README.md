# fraud-detection-system

[![codecov](https://codecov.io/gh/zoltb/fraud-detection-system/graph/badge.svg?token=IJTUTCE94L)](https://codecov.io/gh/zoltb/fraud-detection-system)

## Overview

This project is a real-time fraud detection system built with an event-driven architecture.

It simulates financial transaction processing and detects suspicious behavior (fraud patterns) using streaming data, distributed services, and in-memory time-window analysis.

The system is designed to run locally using Docker Compose and demonstrates backend system design, messaging, and data processing concepts.

---

## Architecture

The system follows an event-driven pipeline:

Client -> Spring Boot API -> Apache Kafka (KRaft mode) -> Fraud Detection Service -> PostgreSQL (persistent storage)

Additionally:

- Redis is used for sliding window-based fraud detection

---

## Tech Stack

- Java 21
- Spring Boot
- Apache Kafka (KRaft mode, no ZooKeeper)
- PostgreSQL
- Redis
- Docker Compose

---

## Key Features

### 1. Real-time transaction processing
Transactions are sent through a REST API using Kafka.

### 2. Event-driven architecture
Microservices communicate via Kafka topics instead of direct HTTP calls.

### 3. Fraud detection logic
Fraud detection is based on:

- Transaction amount thresholds
- Transaction frequency (sliding time window)
- Basic rule-based scoring

### 4. Sliding window detection
Redis is used to track user activity within a time window (e.g. last 60 seconds) to detect high-frequency transactions.

### 5. Data persistence & analytics
- PostgreSQL stores transaction data

---

## Test Data Generation

The system includes a synthetic data generator to simulate realistic financial transactions.

This allows end-to-end testing of the fraud detection pipeline without requiring real user data.

The generator can create:

- Normal transactions
- High-frequency transaction bursts
- High-value suspicious transactions
- Randomized user behavior patterns

Example usage:

The generated data can be sent to the system via REST API or Kafka producer.

## How to Run

## 0. Prerequisites:
   Docker Desktop must be running on your machine.

## 1. Clone repository

````
git clone <repo-url>
cd fraud-detection-system
````
## 2. Build and launch the application using Docker Compose:
2/a) Build and launch
````     
docker compose up -d --build
````
     Note: Once the application is running, it will generate a report.log file in the root directory
     (default transaction count: 100,000).
2/b) Build and launch tests using Docker Compose:
- Prerequisites: build and launch the test environment and run the test suite
- Check running containers and stop the run:
````
docker ps -a
docker stop <CONTAINER_ID> OR docker compose kill OR docker compose down -v --remove-orphans
````
- Troubleshooting:
If the container still there sometimes only Docker Compose restart helps
- To run test suite:
````
docker compose -f compose-test.yaml up -d --build
./mvnw clean test
````
## 3. Open and run in IDE
- Set env variables to: KAFKA_HOST=localhost;KAFKA_PORT=29092
````
docker compose up -d
````
- In case of test (see 2/b) -> Check running containers and stop the run)
````
docker compose -f compose-test.yaml up -d 
````
- It will generate a report.log file in the root directory
- In application.yaml you can set (eg.: transaction count, amount-limit, count-limit)