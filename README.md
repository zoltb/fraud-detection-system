# fraud-detection-system

![Status](https://img.shields.io/badge/status-under--construction-yellow?style=for-the-badge)

> [!IMPORTANT]
> This project is currently **under construction**. Features are subject to change, and documentation might be incomplete.

## Overview

This project is a real-time fraud detection system built with an event-driven architecture.

It simulates financial transaction processing and detects suspicious behavior (fraud patterns) using streaming data, distributed services, and in-memory time-window analysis.

The system is designed to run locally using Docker Compose and demonstrates backend system design, messaging, and data processing concepts.

---

## Architecture

The system follows an event-driven pipeline:

Client -> Spring Boot API -> Apache Kafka (KRaft mode) -> Fraud Detection Service -> PostgreSQL (persistent storage) -> OpenSearch (analytics & search)

Additionally:

- Redis is used for sliding window-based fraud detection
- OpenSearch Dashboards is used for visualization

---

## Tech Stack

- Java 11+
- Spring Boot
- Apache Kafka (KRaft mode, no ZooKeeper)
- PostgreSQL
- Redis
- OpenSearch + OpenSearch Dashboards
- Docker Compose

---

## Key Features

### 1. Real-time transaction processing
Transactions are sent through a REST API and processed asynchronously using Kafka.

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
- OpenSearch stores fraud events for search and analysis

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


python data-generator/generate_transactions.py

The generated data can be sent to the system via REST API or Kafka producer.

## How to Run

1. Clone repository

```
git clone <repo-url>
cd fraud-detection-system

