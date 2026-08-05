CREATE TABLE IF NOT EXISTS notifications (
    id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NULL,
    user_id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(1024) NULL,
    link VARCHAR(255) NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notifications_user (user_id, created_at)
);
