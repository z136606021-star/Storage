-- 第十二期：修复第十一期 image_object_key 迁移因 IF NOT EXISTS 语法未生效的存量库
ALTER TABLE warehouse_bom
    ADD COLUMN image_object_key VARCHAR(512) NULL COMMENT 'MinIO 对象键';
