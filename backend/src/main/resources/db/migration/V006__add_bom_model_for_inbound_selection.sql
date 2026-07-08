ALTER TABLE warehouse_bom ADD COLUMN model VARCHAR(64) NULL COMMENT '型号' AFTER name;

UPDATE warehouse_bom b
JOIN (
    SELECT
        category,
        generic_name,
        COALESCE(brand, '') AS brand_key,
        name,
        MIN(model) AS model
    FROM material_ledger
    WHERE model IS NOT NULL AND model <> ''
    GROUP BY category, generic_name, COALESCE(brand, ''), name
) l
    ON b.category = l.category
    AND b.generic_name = l.generic_name
    AND COALESCE(b.brand, '') = l.brand_key
    AND b.name = l.name
SET b.model = l.model
WHERE b.model IS NULL OR b.model = '';

UPDATE warehouse_bom SET model = name WHERE model IS NULL OR model = '';
