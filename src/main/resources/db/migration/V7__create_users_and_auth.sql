CREATE TABLE users
(
    id             UUID PRIMARY KEY,
    google_subject VARCHAR(255) UNIQUE,
    email          VARCHAR(320) NOT NULL UNIQUE,
    password_hash  VARCHAR(255),
    display_name   VARCHAR(255),
    picture_url    TEXT,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,

    CONSTRAINT chk_users_login_method
        CHECK (
            google_subject IS NOT NULL
                OR password_hash IS NOT NULL
            )
);