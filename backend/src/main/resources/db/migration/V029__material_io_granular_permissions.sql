-- Split material IO actions into independent button permissions while retaining the legacy write node.
UPDATE sys_menu
SET name = '旧版写权限（兼容）', visible = 0, menu_type = 'BUTTON'
WHERE permission = 'warehouse:material-io:write';

INSERT INTO sys_menu (parent_id, menu_type, name, permission, path, component_key, icon, visible, sort_order)
SELECT page.id, 'BUTTON', permission_row.name, permission_row.permission, NULL, NULL, NULL, 0, permission_row.sort_order
FROM sys_menu page
JOIN (
    SELECT '新增出入库' AS name, 'warehouse:material-io:create' AS permission, 10 AS sort_order
    UNION ALL SELECT '编辑出入库', 'warehouse:material-io:update', 20
    UNION ALL SELECT '删除出入库', 'warehouse:material-io:delete', 30
    UNION ALL SELECT '全部删除出入库', 'warehouse:material-io:delete-all', 40
    UNION ALL SELECT '导入出入库', 'warehouse:material-io:import', 50
    UNION ALL SELECT '导出出入库', 'warehouse:material-io:export', 60
) permission_row
WHERE page.permission = 'warehouse:material-io:read'
  AND NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.permission = permission_row.permission);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT legacy_roles.role_id, action_menu.id
FROM (
    SELECT DISTINCT rm.role_id
    FROM sys_role_menu rm
    JOIN sys_menu legacy_menu ON legacy_menu.id = rm.menu_id
    WHERE legacy_menu.permission = 'warehouse:material-io:write'
) legacy_roles
JOIN sys_menu action_menu ON action_menu.permission IN (
    'warehouse:material-io:create',
    'warehouse:material-io:update',
    'warehouse:material-io:delete',
    'warehouse:material-io:delete-all',
    'warehouse:material-io:import'
)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.role_id = legacy_roles.role_id AND existing.menu_id = action_menu.id
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT read_roles.role_id, export_menu.id
FROM (
    SELECT DISTINCT rm.role_id
    FROM sys_role_menu rm
    JOIN sys_menu read_menu ON read_menu.id = rm.menu_id
    WHERE read_menu.permission = 'warehouse:material-io:read'
) read_roles
JOIN sys_menu export_menu ON export_menu.permission = 'warehouse:material-io:export'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.role_id = read_roles.role_id AND existing.menu_id = export_menu.id
);
