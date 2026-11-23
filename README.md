# fraud-detection-system

[![codecov](https://codecov.io/gh/zoltb/fraud-detection-system/graph/badge.svg?token=IJTUTCE94L)](https://codecov.io/gh/zoltb/fraud-detection-system)

## Overview

A fraud detection pet project built with Spring Boot, Kafka, Redis and PostgreSQL.

The application generates synthetic card transactions, sends them through Kafka,
checks each one against simple fraud rules and stores the result in PostgreSQL.
The rules count each user's transactions in Redis within a fixed time window.

Two fraud rules are implemented (thresholds are configurable in `application.yaml`):

- **Velocity:** too many transactions from the same user in a short time.
- **Card testing:** many small-amount transactions from the same user in a short time –
  a typical pattern when someone checks whether a stolen card still works.

Everything runs locally with Docker Compose.

---

## Architecture

It is a **single Spring Boot service**. Inside it, the producer and the consumer side are decoupled by Kafka:

```
Transaction generator ──► Kafka topic "transactions" ──► Consumer ──► Fraud detectors ──► PostgreSQL
  (mock REST endpoint)      (3 partitions, key = userId)   (batch)       (counters in Redis)
```

1. The generator calls the application's own mock endpoint (`GET /api/mock/transactions`) to get a random
   transaction, adds an ID and a timestamp, and sends it to Kafka.
2. The message key is the `userId`, so all transactions of one user go to the same partition and are
   processed in order.
3. The consumer reads records in batches (3 listener threads), runs the fraud detectors and saves
   the whole batch with one `saveAll` call; Hibernate sends the inserts to PostgreSQL in JDBC batches.
   The offset is committed manually, only after the save succeeded.
4. When the application shuts down, it writes a summary report (`report.log`).

The service is not split into microservices, but the Kafka topic is a natural boundary: the transaction
generator and the fraud detection could become two separate services, each with its own deployment
and scaling.

---

## Tech Stack

- Java 21, Spring Boot 3
- Apache Kafka (KRaft mode, no ZooKeeper)
- Redis (window counters)
- PostgreSQL, Spring Data JPA / Hibernate
- Docker Compose
- JUnit 5, GitHub Actions, Codecov

---

## Fraud detection

Each detector keeps a counter per user in Redis:

- `INCR` increases the counter for the user.
- When the counter is 1 (first transaction in the window), `EXPIRE` sets its lifetime
  (the window length is configurable per rule).
- When the key expires, counting starts again from zero.

This is a **fixed window counter**: the window starts at the user's first transaction, not at the current
moment. It is cheap (one counter per user), but a burst that crosses the window boundary can be missed.
A sliding window (Redis sorted set) is on the roadmap.

The window is measured in **processing time** (when the consumer handles the message),
not in event time (`createdAt` of the transaction).

---

## Test data generation

Currently the generator creates random transactions:

- user IDs: 100–800, merchant IDs: 100–110
- amount: 50–10 000
- count: 100 000 by default (`fraud.generator.transaction-count`)

The data is uniformly random, so it is not realistic: with ~700 users and no delay between messages,
most users exceed the velocity limit. More realistic parameters are planned.

---

## How to run

### Prerequisites
- Docker Desktop running
- JDK 21 (only for running from the IDE or running tests)

### 1. Clone

```
git clone https://github.com/zoltb/fraud-detection-system.git
cd fraud-detection-system
```

### 2. Run everything in Docker

```
docker compose up -d --build
```

The application generates the transactions, processes them and then **stops by itself**.
The report is written to `report.log` in the project root.

Stop and clean up:
```
docker compose down
```

### 3. Run from the IDE

Start only the infrastructure (not the application container):
```
docker compose up -d postgres redis kafka
```

Run `FraudDetectionServiceApplication` with this environment variable:
```
KAFKA_PORT=29092
```
Optional, for a quick run: `FRAUD_GENERATOR_TRANSACTIONCOUNT=1000`

### 4. Run the tests

The tests use a separate Docker Compose project (`test-env`, different ports), so it can run next to the
dev environment.

```
docker compose -f compose-test.yaml up -d
./mvnw clean test          # Windows with PowerShell: .\mvnw.cmd clean test
docker compose -f compose-test.yaml down
```

### Troubleshooting
- **`password authentication failed for user "user"`** – another PostgreSQL (for example a local Windows
  service) is listening on port 5432. Stop it, or check with `netstat -ano | findstr :5432`.
- **Kafka connection errors from the IDE** – check that `KAFKA_PORT=29092` is set (port 9092 is for
  containers inside the Docker network).

---

## Performance

Earlier benchmark results were removed: they were measured while the Hibernate batch settings were placed
under the wrong configuration key (`spring.jpa.hibernate.*`), so JDBC batching was not active.
The application also stopped 5 seconds after the producer finished, so not all messages were processed.

After moving the settings to `spring.jpa.properties.hibernate.*`, Hibernate statistics show the inserts going
out in JDBC batches, and all 100 000 messages are processed. New measurements will follow.

One finding from the old measurements is still valid: more threads and partitions did not make the
processing faster, because the bottleneck was writing to the database row by row.

---

## Known limitations

- **Shutdown does not wait for the consumer.** The application stops a fixed 5 seconds after the producer
  finished. If the consumer falls behind, the rest of the messages stay unprocessed in Kafka.
- **Fixed window, processing time.** See [Fraud detection](#fraud-detection).
- **`INCR` and `EXPIRE` are two separate Redis commands.** If the application stops between them, the key
  has no expiry and the counter is never reset.
- **At-least-once processing.** If saving fails, the batch is delivered again, and the Redis counters are
  increased a second time for the same transactions.
- **Startup clears all data.** On startup the application runs `FLUSHALL` on Redis and truncates the
  `transactions` table. This is fine for a local demo, but not for a shared environment.
- **No dead-letter topic.** A message that cannot be processed is retried, there is no separate error path.

---

## Roadmap

- Shutdown after all messages are processed (instead of a fixed wait)
- Sliding window with Redis sorted sets (`ZADD`, `ZREMRANGEBYSCORE`, `ZCARD`)
- Idempotent processing (unique `transaction_id`, safe Redis side effects on redelivery)
- `POST /api/transactions` endpoint for sending transactions from outside
- More realistic test data
- Dead-letter topic
