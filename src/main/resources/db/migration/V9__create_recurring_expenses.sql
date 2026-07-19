CREATE TABLE recurring_expenses (
                                    id UUID PRIMARY KEY,

                                    user_id VARCHAR(255) NOT NULL,

                                    name VARCHAR(150) NOT NULL,
                                    merchant VARCHAR(150),

                                    amount NUMERIC(12, 2) NOT NULL,
                                    currency VARCHAR(3) NOT NULL DEFAULT 'RSD',

                                    category_id UUID,

                                    frequency VARCHAR(30) NOT NULL,

                                    next_due_date DATE NOT NULL,
                                    end_date DATE,

                                    active BOOLEAN NOT NULL DEFAULT TRUE,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_recurring_expense_category
                                        FOREIGN KEY (category_id)
                                            REFERENCES categories(id),

                                    CONSTRAINT chk_recurring_expense_amount
                                        CHECK (amount > 0),

                                    CONSTRAINT chk_recurring_expense_dates
                                        CHECK (
                                            end_date IS NULL
                                                OR end_date >= next_due_date
                                            ),

                                    CONSTRAINT chk_recurring_expense_frequency
                                        CHECK (
                                            frequency IN (
                                                          'WEEKLY',
                                                          'MONTHLY',
                                                          'QUARTERLY',
                                                          'YEARLY'
                                                )
                                            )
);

CREATE INDEX idx_recurring_expenses_user
    ON recurring_expenses(user_id);

CREATE INDEX idx_recurring_expenses_user_active
    ON recurring_expenses(user_id, active);

CREATE INDEX idx_recurring_expenses_user_due_date
    ON recurring_expenses(user_id, next_due_date);

CREATE INDEX idx_recurring_expenses_upcoming
    ON recurring_expenses(user_id, active, next_due_date);