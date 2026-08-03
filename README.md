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
---
## Performance & Benchmarks

The system was benchmarked locally to measure throughput and identify potential bottlenecks (such as database I/O and local resource limits).

### Benchmark Results

| Records | Configuration | Time (ms) | Duration |
| :--- | :--- | :--- | :--- |
| 10,000 | 1 thread / 3 partitions | 129,095 ms | ~2 min 09 sec |
| 10,000 | 3 thread / 3 partitions | 127,590 ms | ~2 min 07 sec |
| 10,000 | 6 thread / 6 partitions | 149,408 ms | ~2 min 29 sec |
| 50,000 | 1 thread / 3 partitions | 675,994 ms | ~11 min 15 sec |
| 50,000 | 3 thread / 3 partitions | 654,510 ms | ~10 min 54 sec |
| 50,000 | 6 thread / 6 partitions | 663,876 ms | ~11 min 03 sec |

### Key Takeaways & Optimizations

* **Bottlenecks:** Localhost execution and PostgreSQL write operations act as the primary bottlenecks.
* **Impact of Batch Saving:** Implementing Hibernate batch insertions:
`datasource.hikari.maximum-pool-size: 50`, `reWriteBatchedInserts=true`, `jpa.hibernate.jdbc.batch_size=500`
* **Drastically improved performance:**
    * **100,000 records** (3 threads / 3 partitions) completed in **73,316 ms (~1 min 13 sec).
    * **1,000,000 records** (3 threads / 3 partitions) completed in **646,923 ms (~10 min 47 sec).
