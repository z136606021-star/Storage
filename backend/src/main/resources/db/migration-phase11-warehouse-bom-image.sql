-- MySQL 8 不支持 ADD COLUMN IF NOT EXISTS；依赖 spring.sql.init.continue-on-error 幂等重跑
ALTER TABLE warehouse_bom
    ADD COLUMN image_object_key VARCHAR(512) NULL COMMENT 'MinIO 对象键';