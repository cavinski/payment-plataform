ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_error TEXT,
    ADD COLUMN failed_at TIMESTAMPTZ;

DROP INDEX idx_outbox_events_pending;

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (next_attempt_at, created_at)
    WHERE processed_at IS NULL
      AND failed_at IS NULL;
      