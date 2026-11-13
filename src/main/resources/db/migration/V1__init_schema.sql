
-- =====================================================
-- V1: INITIAL DATABASE SCHEMA
-- Project: Realtime Chat
-- Database: chat_realtime
-- =====================================================

-- 1. USERS
-- Luu tai khoan nguoi dung duoc xac thuc bang Google OIDC

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    google_sub VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    avatar_url VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_login_at DATETIME(6),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_google_sub UNIQUE (google_sub),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 2. CONVERSATIONS
-- PRIVATE: chat 1-1
-- GROUP: chat nhom

CREATE TABLE conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(150),
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_conversations PRIMARY KEY (id),

    CONSTRAINT fk_conversations_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT chk_conversations_type
        CHECK (type IN ('PRIVATE', 'GROUP')),

    INDEX idx_conversations_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3. CONVERSATION MEMBERS
-- Luu thanh vien cua tung cuoc tro chuyen
-- OWNER: chu nhom
-- MEMBER: thanh vien

CREATE TABLE conversation_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    last_read_message_id BIGINT,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_conversation_members PRIMARY KEY (id),

    CONSTRAINT uk_conversation_user
        UNIQUE (conversation_id, user_id),

    CONSTRAINT fk_members_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_members_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_member_role
        CHECK (role IN ('OWNER', 'MEMBER')),

    INDEX idx_members_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4. MESSAGES
-- Luu tin nhan ca nhan va tin nhan nhom

CREATE TABLE messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    client_message_id CHAR(36),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_messages PRIMARY KEY (id),

    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id)
        REFERENCES users(id),

    INDEX idx_messages_conversation_id_id
        (conversation_id, id),

    INDEX idx_messages_sender_id (sender_id),

    CONSTRAINT uk_sender_client_message
        UNIQUE (sender_id, client_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 5. LAST READ MESSAGE
-- Lien ket trang thai doc cua thanh vien voi tin nhan

ALTER TABLE conversation_members
ADD CONSTRAINT fk_members_last_read_message
FOREIGN KEY (last_read_message_id)
REFERENCES messages(id)
ON DELETE SET NULL;
