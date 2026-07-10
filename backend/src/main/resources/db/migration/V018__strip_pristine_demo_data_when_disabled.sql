-- Remove baseline demo business data on fresh databases when LOAD_DEMO_DATA=false.
-- Placeholder ${loadDemoData} is injected by Flyway from application env (default true for dev).
-- When loadDemoData=true, this migration is a no-op.
-- When loadDemoData=false, cleanup runs only if the database still matches the historical pristine seed snapshot.

SET @strip_pristine_demo := IF(
    '${loadDemoData}' = 'true',
    0,
    IF(
        (SELECT COUNT(*) FROM material_io_record) = 0
        AND (SELECT COUNT(*) FROM experience_record) = 0
        AND (SELECT COUNT(*) FROM experience_attachment) = 0
        AND (SELECT COUNT(*) FROM experience_project_link) = 0
        AND (SELECT COUNT(*) FROM material_ledger) = 15
        AND (SELECT COUNT(*) FROM material_ledger WHERE model IN (
            'SL8-01', 'SL4-02', 'SPA-6', 'SPP-6', 'PC8-02', 'SY3120', 'SY3320',
            '18*20', '12*15', 'OR-10', 'OR-12', 'HM-46', 'SL6-01', 'SPA-8', '25*30'
        )) = 15
        AND (SELECT COUNT(*) FROM warehouse_bin) = 6
        AND (SELECT COUNT(*) FROM warehouse_bin WHERE bin_code IN (
            '1-1-1', '1-1-4', '1-2-1', '1-2-2', '1-2-3', '1-3-1'
        )) = 6
        AND (SELECT COUNT(*) FROM warehouse_bom) = 9
        AND (SELECT COUNT(*) FROM safety_stock) = 3
        AND (SELECT COUNT(*) FROM design_guide) = 9
        AND (SELECT COUNT(*) FROM design_product_type) = 10
        AND (SELECT COUNT(*) FROM design_product_type WHERE type_code IN (
            'A01', 'A02', 'A03', 'A04', 'A05', 'A06', 'A07', 'A08', 'A09', 'A10'
        )) = 10
        AND (SELECT COUNT(*) FROM design_stage) = 5
        AND (SELECT COUNT(*) FROM experience_type) = 3
        AND (SELECT COUNT(*) FROM experience_type WHERE name IN (
            '设计经验', '制造问题', '客户需求变更'
        )) = 3,
        1,
        0
    )
);

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM safety_stock', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM material_io_record', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM material_ledger', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM warehouse_bom_image', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM warehouse_bom', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM warehouse_bin', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM design_guide', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM design_product_type', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM design_stage', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@strip_pristine_demo = 1, 'DELETE FROM experience_type', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
