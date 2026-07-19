CREATE TABLE expenses
(
    id            UUID PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,
    merchant      VARCHAR(255) NOT NULL,
    amount        NUMERIC(12, 2) NOT NULL,
    currency      VARCHAR(10) NOT NULL,
    category      VARCHAR(100),
    purchase_date TIMESTAMP NOT NULL,
    qr_url        TEXT,
    receipt_image TEXT,
    created_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_expenses_user_id
    ON expenses (user_id);

CREATE INDEX idx_expenses_purchase_date
    ON expenses (purchase_date DESC);

CREATE INDEX idx_expenses_user_purchase_date
    ON expenses (user_id, purchase_date DESC);