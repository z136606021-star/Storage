-- Optimize warehouse menu order and flatten system-management menus for independent sidebar entries.

UPDATE sys_menu SET sort_order = 5 WHERE id = 117;
UPDATE sys_menu SET sort_order = 10 WHERE id = 111;
UPDATE sys_menu SET sort_order = 20 WHERE id = 112;
UPDATE sys_menu SET sort_order = 30 WHERE id = 113;

UPDATE sys_menu
SET parent_id = 200,
    path = '/system/roles',
    visible = 1,
    sort_order = 20
WHERE id = 202;

UPDATE sys_menu
SET parent_id = 200,
    path = '/system/menus',
    visible = 1,
    sort_order = 30
WHERE id = 203;

UPDATE sys_menu
SET component_key = 'views/system/UserManageView.vue'
WHERE id = 201;

UPDATE sys_menu SET sort_order = 40 WHERE id = 204;
