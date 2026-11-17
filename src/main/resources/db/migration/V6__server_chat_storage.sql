ALTER TABLE conversations ADD COLUMN external_id VARCHAR(255) NULL;
UPDATE conversations SET external_id=CONCAT('db-', id);
CREATE UNIQUE INDEX uk_conversations_external ON conversations (external_id);
ALTER TABLE conversations ADD COLUMN metadata LONGTEXT NULL;
ALTER TABLE conversation_members ADD COLUMN preferences LONGTEXT NULL;
ALTER TABLE messages ADD COLUMN external_id VARCHAR(64) NULL;
UPDATE messages SET external_id=CONCAT('db-msg-', id);
CREATE UNIQUE INDEX uk_messages_external ON messages (external_id);
ALTER TABLE messages ADD COLUMN metadata LONGTEXT NULL;
CREATE TABLE user_preferences (
 user_id BIGINT NOT NULL PRIMARY KEY,
 settings LONGTEXT NOT NULL,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE user_notifications (
 user_id BIGINT NOT NULL,
 notification_id VARCHAR(255) NOT NULL,
 payload LONGTEXT NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY (user_id, notification_id),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE desktop_deliveries (
 user_id BIGINT NOT NULL,
 session_key VARCHAR(64) NOT NULL,
 message_key VARCHAR(255) NOT NULL,
 claimed_at BIGINT NOT NULL,
 delivered BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY (user_id, session_key, message_key),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE legacy_chat_imports (
 user_id BIGINT NOT NULL,
 source_id VARCHAR(64) NOT NULL,
 payload LONGTEXT NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY (user_id, source_id),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
