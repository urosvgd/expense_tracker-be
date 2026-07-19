CREATE TABLE categories
(
    id         UUID PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    icon       VARCHAR(50),
    color_hex  VARCHAR(7),
    sort_order INTEGER      NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_categories_type
        CHECK (type IN ('EXPENSE', 'ITEM')),

    CONSTRAINT uk_categories_code_type
        UNIQUE (code, type)
);

CREATE INDEX idx_categories_type_active_order
    ON categories (type, active, sort_order);