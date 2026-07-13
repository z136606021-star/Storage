CREATE TABLE IF NOT EXISTS email_verification_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    purpose VARCHAR(32) NOT NULL COMMENT '用途码',
    code_hash VARCHAR(64) NOT NULL COMMENT '验证码 SHA-256 哈希',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    used_at DATETIME NULL COMMENT '使用时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_verification_user_purpose (user_id, purpose),
    INDEX idx_email_verification_expires_at (expires_at),
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
