CREATE TABLE friendships (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_low_id BIGINT NOT NULL,
    user_high_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_friendships_pair UNIQUE (user_low_id, user_high_id),
    CONSTRAINT fk_friendships_low FOREIGN KEY (user_low_id) REFERENCES users(id),
    CONSTRAINT fk_friendships_high FOREIGN KEY (user_high_id) REFERENCES users(id),
    CONSTRAINT fk_friendships_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT chk_friendships_order CHECK (user_low_id < user_high_id),
    CONSTRAINT chk_friendships_status CHECK (status IN ('PENDING', 'ACCEPTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
