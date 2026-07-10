-- Flatten system-management menus under catalog 200 for independent sidebar entries.

UPDATE sys_menu
SET parent_id = 200,
    path = '/system/users',
    component_key = 'views/system/UserManageView.vue',
    visible = 1,
    sort_order = 10
WHERE id = 201;

UPDATE sys_menu
SET parent_id = 200,
    path = '/system/roles',
    component_key = 'components/system/RoleManagePanel.vue',
    visible = 1,
    sort_order = 20
WHERE id = 202;

UPDATE sys_menu
SET parent_id = 200,
    path = '/system/menus',
    component_key = 'components/system/MenuManagePanel.vue',
    visible = 1,
    sort_order = 30
WHERE id = 203;

UPDATE sys_menu
SET parent_id = 200,
    path = '/system/customers',
    component_key = 'views/system/CustomerManageView.vue',
    visible = 1,
    sort_order = 40
WHERE id = 204;
