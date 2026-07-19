CREATE TABLE receipt_items
(
    id          UUID PRIMARY KEY,
    expense_id  UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    quantity    NUMERIC(12, 3) NOT NULL,
    unit_price  NUMERIC(12, 2) NOT NULL,
    total_price NUMERIC(12, 2) NOT NULL,
    category    VARCHAR(100),

    CONSTRAINT fk_receipt_items_expense
        FOREIGN KEY (expense_id)
            REFERENCES expenses (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_receipt_items_expense_id
    ON receipt_items (expense_id);