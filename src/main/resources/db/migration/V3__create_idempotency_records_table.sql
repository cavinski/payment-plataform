CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT uk_idempotency_records_key
        UNIQUE (idempotency_key),

    CONSTRAINT chk_idempotency_response_status
        CHECK (
            response_status IS NULL
            OR response_status BETWEEN 100 AND 599
        )
);