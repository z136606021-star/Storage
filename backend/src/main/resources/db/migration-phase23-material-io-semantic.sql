ALTER TABLE material_io_record
    ADD COLUMN purpose VARCHAR(32) NULL COMMENT '用途码' AFTER remark;

CREATE INDEX idx_material_io_purpose ON material_io_record (purpose);
