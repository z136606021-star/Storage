CREATE TABLE sys_exception_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(16) NOT NULL COMMENT 'BACKEND or FRONTEND',
    level VARCHAR(16) NOT NULL DEFAULT 'ERROR' COMMENT '日志级别',
    occurred_at DATETIME NOT NULL COMMENT '异常发生时间',
    error_code VARCHAR(64) NULL COMMENT '错误码',
    request_id VARCHAR(64) NULL COMMENT '请求追踪 ID',
    http_status INT NULL COMMENT 'HTTP 状态码',
    http_method VARCHAR(16) NULL COMMENT 'HTTP 方法',
    request_path VARCHAR(512) NULL COMMENT '请求路径（已脱敏）',
    exception_class VARCHAR(256) NULL COMMENT '异常类型',
    summary VARCHAR(512) NOT NULL COMMENT '异常摘要',
    stack_trace TEXT NULL COMMENT '堆栈信息（截断）',
    frontend_route VARCHAR(512) NULL COMMENT '前端路由',
    browser_info VARCHAR(512) NULL COMMENT '浏览器信息',
    operator_id BIGINT NULL COMMENT '操作人 ID',
    operator_username VARCHAR(64) NULL COMMENT '操作人用户名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_exception_log_occurred_at (occurred_at),
    INDEX idx_exception_log_source (source),
    INDEX idx_exception_log_request_id (request_id),
    INDEX idx_exception_log_http_status (http_status),
    INDEX idx_exception_log_exception_class (exception_class),
    CONSTRAINT fk_exception_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_menu (parent_id, menu_type, name, permission, path, component_key, icon, visible, sort_order)
SELECT p.id, 'SUB', '异常日志', 'system:exception-log:read', '/system/exception-logs', 'views/system/ExceptionLogView.vue', NULL, 1, 50
FROM sys_menu p
WHERE p.name = '系统管理'
  AND p.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:exception-log:read');

INSERT INTO sys_menu (parent_id, menu_type, name, permission, path, component_key, icon, visible, sort_order)
SELECT pg.id, 'BUTTON', '异常日志清理', 'system:exception-log:write', NULL, NULL, NULL, 0, 10
FROM sys_menu pg
WHERE pg.permission = 'system:exception-log:read'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:exception-log:write');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.permission IN ('system:exception-log:read', 'system:exception-log:write')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );
