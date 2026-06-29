CREATE TABLE IF NOT EXISTS warehouse_bin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bin_code VARCHAR(32) NOT NULL COMMENT 'Bin位编号（排-列-层）',
    row_no INT NOT NULL COMMENT '排',
    col_no INT NOT NULL COMMENT '列',
    level_no INT NOT NULL COMMENT '层',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bin_code (bin_code),
    UNIQUE KEY uk_row_col_level (row_no, col_no, level_no),
    INDEX idx_bin_code (bin_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO warehouse_bin (bin_code, row_no, col_no, level_no, remark)
SELECT DISTINCT
    bin_location,
    CAST(SUBSTRING_INDEX(bin_location, '-', 1) AS UNSIGNED),
    CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(bin_location, '-', 2), '-', -1) AS UNSIGNED),
    CAST(SUBSTRING_INDEX(bin_location, '-', -1) AS UNSIGNED),
    NULL
FROM material_ledger
WHERE bin_location REGEXP '^[0-9]+-[0-9]+-[0-9]+$';
