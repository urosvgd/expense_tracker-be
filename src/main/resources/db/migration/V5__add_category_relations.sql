ALTER TABLE expenses
    ADD COLUMN category_id UUID;

ALTER TABLE receipt_items
    ADD COLUMN category_id UUID;

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id);

ALTER TABLE receipt_items
    ADD CONSTRAINT fk_receipt_items_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id);

CREATE INDEX idx_expenses_category_id
    ON expenses (category_id);

CREATE INDEX idx_receipt_items_category_id
    ON receipt_items (category_id);