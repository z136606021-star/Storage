UPDATE material_ledger
SET brand = ''
WHERE brand IS NULL;

CREATE TEMPORARY TABLE material_ledger_duplicate_guard (
    must_be_empty INT NOT NULL,
    CONSTRAINT chk_no_material_ledger_duplicates CHECK (must_be_empty <> 1)
);

INSERT INTO material_ledger_duplicate_guard (must_be_empty)
SELECT 1
FROM material_ledger
GROUP BY category, generic_name, brand, name, model, bin_location
HAVING COUNT(*) > 1
LIMIT 1;

DROP TEMPORARY TABLE material_ledger_duplicate_guard;

SET @brand_nullable := (
    SELECT IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'material_ledger'
      AND COLUMN_NAME = 'brand'
);

SET @sql := IF(
    @brand_nullable = 'YES',
    'ALTER TABLE material_ledger MODIFY brand VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''品牌''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_count := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'material_ledger'
      AND INDEX_NAME = 'uk_material_ledger_natural_key'
);

SET @sql := IF(
    @index_count = 0,
    'ALTER TABLE material_ledger ADD CONSTRAINT uk_material_ledger_natural_key UNIQUE (category, generic_name, brand, name, model, bin_location)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
