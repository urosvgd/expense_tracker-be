CREATE TABLE budgets
(
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(255)  NOT NULL,
    year        INTEGER       NOT NULL,
    month       INTEGER       NOT NULL,
    total_limit NUMERIC(12, 2),
    currency    VARCHAR(10)   NOT NULL DEFAULT 'RSD',
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,

    CONSTRAINT chk_budgets_month
        CHECK (month BETWEEN 1 AND 12),

    CONSTRAINT chk_budgets_total_limit
        CHECK (total_limit IS NULL OR total_limit >= 0),

    CONSTRAINT uq_budgets_user_year_month
        UNIQUE (user_id, year, month)
);

CREATE INDEX idx_budgets_user_id
    ON budgets (user_id);

CREATE INDEX idx_budgets_user_year_month
    ON budgets (user_id, year, month);


CREATE TABLE category_budgets
(
    id           UUID PRIMARY KEY,
    budget_id    UUID           NOT NULL,
    category_id  UUID           NOT NULL,
    amount_limit NUMERIC(12, 2) NOT NULL,
    created_at   TIMESTAMP      NOT NULL,
    updated_at   TIMESTAMP      NOT NULL,

    CONSTRAINT fk_category_budgets_budget
        FOREIGN KEY (budget_id)
            REFERENCES budgets (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_category_budgets_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id),

    CONSTRAINT chk_category_budgets_amount_limit
        CHECK (amount_limit >= 0),

    CONSTRAINT uq_category_budgets_budget_category
        UNIQUE (budget_id, category_id)
);

CREATE INDEX idx_category_budgets_budget_id
    ON category_budgets (budget_id);

CREATE INDEX idx_category_budgets_category_id
    ON category_budgets (category_id);