-- Local test seed for warehouse filter / picker regression.
-- Safe to re-run: clears warehouse business tables only (not users/menus).

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM material_io_record;
DELETE FROM safety_stock;
DELETE FROM material_ledger;
DELETE FROM warehouse_bom_image;
DELETE FROM warehouse_bom;
DELETE FROM warehouse_bin;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO warehouse_bin (bin_code, row_no, col_no, level_no, remark) VALUES
('1-1-1', '1', 1, 1, 'Zone-A'),
('1-1-3', '1', 1, 3, 'Zone-A'),
('1-2-1', '1', 2, 1, 'Zone-B'),
('1-3-1', '1', 3, 1, 'Zone-C');

INSERT INTO warehouse_bom (category, generic_name, brand, name, remark) VALUES
('五金类', '轴承座', 'F206/UC206', 'UC206', 'seed'),
('五金类', '轴承座', 'ASAHI/轴承座', 'UCPA204', 'seed'),
('五金类', '轴承', '内径9外17厚5', '689ZZ', 'seed'),
('气路配件', 'C式快速接头', 'C式快速接头', 'PP-20', NULL),
('气路配件', 'C式快速接头', 'C式快速接头', 'PP-30', NULL),
('气路配件', 'C式快速接头', 'C式快速接头', 'SP-20', NULL),
('气路配件', 'C式快速接头', 'C式快速接头', 'SP-30', NULL),
('气路配件', '电磁阀', 'SMC', 'SY3120', NULL),
('气路配件', '电磁阀', 'SMC', 'SY3320', NULL),
('耗材', '生料带', '冰禹', '生料带', NULL),
('耗材', '密封圈', '三环', 'O型密封圈', NULL);

INSERT INTO material_ledger (category, generic_name, brand, name, model, bin_location, stock_quantity, unit_price, remark) VALUES
('五金类', '轴承座', 'F206/UC206', 'UC206', 'UC206-A', '1-1-1', 45, NULL, 'seed'),
('五金类', '轴承座', 'ASAHI/轴承座', 'UCPA204', 'UCPA204-A', '1-1-1', 32, NULL, 'seed'),
('五金类', '轴承', '内径9外17厚5', '689ZZ', '689ZZ-A', '1-2-1', 120, NULL, 'seed'),
('气路配件', 'C式快速接头', 'C式快速接头', 'PP-20', 'PP-20', '1-1-3', 30, NULL, 'seed'),
('气路配件', 'C式快速接头', 'C式快速接头', 'PP-30', 'PP-30', '1-1-3', 30, NULL, 'seed'),
('气路配件', 'C式快速接头', 'C式快速接头', 'SP-20', 'SP-20', '1-1-3', 30, NULL, 'seed'),
('气路配件', 'C式快速接头', 'C式快速接头', 'SP-30', 'SP-30', '1-1-3', 30, NULL, 'seed'),
('气路配件', '电磁阀', 'SMC', 'SY3120', 'SY3120', '1-3-1', 12, 320.00, 'seed'),
('气路配件', '电磁阀', 'SMC', 'SY3320', 'SY3320', '1-3-1', 8, 380.00, 'seed'),
('耗材', '生料带', '冰禹', '生料带', '18*20', '1-2-1', 40, NULL, 'seed'),
('耗材', '密封圈', '三环', 'O型密封圈', 'OR-10', '1-2-1', 500, 0.50, 'seed'),
('气路配件', '气管接头', '', 'PC型/直通', 'PC8-02', '1-1-3', 22, NULL, 'seed');

INSERT INTO safety_stock (material_ledger_id, safety_quantity, warning_enabled)
SELECT id,
       CASE
           WHEN name IN ('PP-20', 'PP-30', 'SP-20', 'SP-30') THEN 50
           WHEN brand = 'SMC' THEN 15
           WHEN category = '五金类' THEN 80
           ELSE 30
       END,
       1
FROM material_ledger;
