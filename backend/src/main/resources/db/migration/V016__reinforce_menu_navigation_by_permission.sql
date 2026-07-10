-- Reinforce system-management navigation by permission instead of fixed menu ids.



UPDATE sys_menu

SET parent_id = 200,

    path = '/system/users',

    component_key = 'views/system/UserManageView.vue',

    visible = 1,

    sort_order = 10

WHERE permission = 'system:user:read';



UPDATE sys_menu

SET parent_id = 200,

    path = '/system/roles',

    component_key = 'components/system/RoleManagePanel.vue',

    visible = 1,

    sort_order = 20

WHERE permission = 'system:role:read';



UPDATE sys_menu

SET parent_id = 200,

    path = '/system/menus',

    component_key = 'components/system/MenuManagePanel.vue',

    visible = 1,

    sort_order = 30

WHERE permission = 'system:menu:read';



UPDATE sys_menu

SET parent_id = 200,

    path = '/system/customers',

    component_key = 'views/system/CustomerManageView.vue',

    visible = 1,

    sort_order = 40

WHERE permission = 'system:customer:read';


