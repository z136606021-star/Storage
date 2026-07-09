CREATE TABLE experience_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '经验类型名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_experience_type_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE experience_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_id BIGINT NOT NULL COMMENT '经验类型 ID',
    description TEXT NOT NULL COMMENT '描述',
    impact TEXT NULL COMMENT '影响',
    suggestion TEXT NULL COMMENT '建议',
    action_plan TEXT NULL COMMENT '行动方案',
    recorder_user_id BIGINT NULL COMMENT '记录人用户 ID',
    recorder_name VARCHAR(64) NOT NULL COMMENT '记录人',
    recorded_at DATETIME NOT NULL COMMENT '记录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_experience_record_type (type_id),
    INDEX idx_experience_record_recorder (recorder_name),
    INDEX idx_experience_record_recorded_at (recorded_at),
    CONSTRAINT fk_experience_record_type FOREIGN KEY (type_id) REFERENCES experience_type (id),
    CONSTRAINT fk_experience_record_user FOREIGN KEY (recorder_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE experience_project_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL COMMENT '经验记录 ID',
    project_name VARCHAR(128) NOT NULL COMMENT '关联项目名称/编号',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    INDEX idx_experience_project_record (record_id),
    CONSTRAINT fk_experience_project_record FOREIGN KEY (record_id) REFERENCES experience_record (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE experience_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL COMMENT '经验记录 ID',
    file_id BIGINT NOT NULL COMMENT '文件 ID',
    object_key VARCHAR(255) NOT NULL COMMENT 'MinIO 对象键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    content_type VARCHAR(128) NULL COMMENT 'MIME 类型',
    size_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    INDEX idx_experience_attachment_record (record_id),
    CONSTRAINT fk_experience_attachment_record FOREIGN KEY (record_id) REFERENCES experience_record (id) ON DELETE CASCADE,
    CONSTRAINT fk_experience_attachment_file FOREIGN KEY (file_id) REFERENCES sys_file (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO experience_type (name, status, sort_order)
SELECT '设计经验', 1, 10 WHERE NOT EXISTS (SELECT 1 FROM experience_type WHERE name = '设计经验');
INSERT INTO experience_type (name, status, sort_order)
SELECT '制造问题', 1, 20 WHERE NOT EXISTS (SELECT 1 FROM experience_type WHERE name = '制造问题');
INSERT INTO experience_type (name, status, sort_order)
SELECT '客户需求变更', 1, 30 WHERE NOT EXISTS (SELECT 1 FROM experience_type WHERE name = '客户需求变更');

UPDATE sys_menu
SET component_key = 'views/experience/ExperienceLibraryView.vue'
WHERE permission = 'platform:experience:read';

INSERT INTO sys_menu (parent_id, menu_type, name, permission, path, component_key, icon, visible, sort_order)
SELECT NULL, 'MENU', '经验库写', 'platform:experience:write', NULL, NULL, NULL, 0, 258
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'platform:experience:write');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.permission = 'platform:experience:write'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );
