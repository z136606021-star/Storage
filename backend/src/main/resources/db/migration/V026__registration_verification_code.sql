CREATE TABLE IF NOT EXISTS registration_verification_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(128) NOT NULL COMMENT 'normalized lowercase email',
    code_hash VARCHAR(64) NOT NULL COMMENT '验证码 SHA-256 哈希',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    used_at DATETIME NULL COMMENT '使用时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_registration_verification_email (email),
    INDEX idx_registration_verification_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
