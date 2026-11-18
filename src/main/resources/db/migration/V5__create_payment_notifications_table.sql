CREATE TABLE payment_notifications (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    charge_id UUID NOT NULL,
    notification_type VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_payment_notifications_event_id
        UNIQUE (event_id)
);

CREATE INDEX idx_payment_notifications_charge_id
    ON payment_notifications (charge_id);