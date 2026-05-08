DROP TABLE IF EXISTS transactions;
CREATE TABLE transactions
(
    id         SERIAL PRIMARY KEY,
    user_id    INT NOT NULL,
    fraud_type VARCHAR(255)
);