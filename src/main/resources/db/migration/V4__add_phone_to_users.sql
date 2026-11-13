ALTER TABLE users
    ADD COLUMN phone VARCHAR(20) NULL AFTER email;

CREATE UNIQUE INDEX uk_users_phone
    ON users (phone);

CREATE INDEX idx_users_phone
    ON users (phone);
