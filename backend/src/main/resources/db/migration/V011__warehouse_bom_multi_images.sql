CREATE TABLE IF NOT EXISTS warehouse_bom_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT NOT NULL COMMENT '物料清单 ID',
    object_key VARCHAR(512) NOT NULL COMMENT 'MinIO 对象键',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_warehouse_bom_image_bom_id (bom_id),
    CONSTRAINT fk_warehouse_bom_image_bom FOREIGN KEY (bom_id) REFERENCES warehouse_bom (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO warehouse_bom_image (bom_id, object_key, sort_order)
SELECT wb.id, wb.image_object_key, 0
FROM warehouse_bom wb
WHERE wb.image_object_key IS NOT NULL
  AND wb.image_object_key <> ''
  AND NOT EXISTS (
      SELECT 1 FROM warehouse_bom_image wbi WHERE wbi.bom_id = wb.id
  );

SET @remark_len := (
    SELECT CHARACTER_MAXIMUM_LENGTH
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bom'
      AND COLUMN_NAME = 'remark'
);

SET @sql := IF(
    @remark_len IS NOT NULL AND @remark_len < 999,
    'ALTER TABLE warehouse_bom MODIFY remark VARCHAR(999) NULL COMMENT ''备注''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
