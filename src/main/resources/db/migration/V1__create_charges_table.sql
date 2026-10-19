CREATE TABLE charges (
    id UUID PRIMARY KEY,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_charges_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_charges_currency_brl
        CHECK (currency = 'BRL'),

    CONSTRAINT chk_charges_status
        CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX idx_charges_created_at
    ON charges (created_at DESC);