CREATE TABLE processed_events (
    id           VARCHAR(64) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);
