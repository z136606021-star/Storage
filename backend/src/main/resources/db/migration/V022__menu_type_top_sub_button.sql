-- Normalize menu types to TOP / SUB / BUTTON and reparent action permissions under their pages.
-- Restore personal center as the first visible top-level module.

UPDATE sys_menu SET menu_type = 'TOP' WHERE menu_type = 'CATALOG' AND parent_id IS NULL;

UPDATE sys_menu SET menu_type = 'SUB' WHERE menu_type = 'CATALOG' AND parent_id IS NOT NULL;

UPDATE sys_menu SET menu_type = 'SUB' WHERE menu_type = 'MENU' AND path IS NOT NULL AND path <> '';

UPDATE sys_menu SET menu_type = 'BUTTON', visible = 0 WHERE menu_type = 'MENU' AND (path IS NULL OR path = '');

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'warehouse:material-ledger:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'warehouse:material-ledger:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'warehouse:material-io:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'warehouse:material-io:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'warehouse:safety-stock:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'warehouse:safety-stock:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'warehouse:bin:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'warehouse:bin:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'warehouse:bom:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission IN ('warehouse:bom:write', 'platform:file:upload');

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'system:user:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'system:user:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'system:role:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'system:role:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'system:menu:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'system:menu:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'system:customer:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'system:customer:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'platform:experience:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'platform:experience:write';

UPDATE sys_menu m
INNER JOIN sys_menu p ON p.permission = 'platform:design:read'
SET m.parent_id = p.id, m.menu_type = 'BUTTON', m.visible = 0
WHERE m.permission = 'platform:design:write';

UPDATE sys_menu
SET visible = 1,
    sort_order = 1,
    menu_type = 'TOP',
    icon = 'HomeOutlined',
    path = '/platform/personal',
    component_key = 'views/platform/PersonalCenterView.vue'
WHERE permission = 'platform:personal:read';

UPDATE sys_menu SET sort_order = 10 WHERE id = 110;

UPDATE sys_menu SET sort_order = 20 WHERE id = 200;
