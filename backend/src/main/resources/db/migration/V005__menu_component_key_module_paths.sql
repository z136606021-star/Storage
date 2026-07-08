-- Store frontend route components as menu-managed module paths instead of registry aliases.
ALTER TABLE sys_menu MODIFY component_key VARCHAR(128) NULL COMMENT '前端组件模块路径';

UPDATE sys_menu SET component_key = 'views/platform/ShellPlaceholderView.vue' WHERE component_key = 'ShellPlaceholder';
UPDATE sys_menu SET component_key = 'views/warehouse/MaterialLedgerView.vue' WHERE component_key = 'MaterialLedger';
UPDATE sys_menu SET component_key = 'views/warehouse/MaterialIoView.vue' WHERE component_key = 'MaterialIo';
UPDATE sys_menu SET component_key = 'views/warehouse/SafetyStockView.vue' WHERE component_key = 'SafetyStock';
UPDATE sys_menu SET component_key = 'views/warehouse/InventoryStatsView.vue' WHERE component_key = 'InventoryStats';
UPDATE sys_menu SET component_key = 'views/warehouse/config/BinManageView.vue' WHERE component_key = 'BinManage';
UPDATE sys_menu SET component_key = 'views/warehouse/config/BomManageView.vue' WHERE component_key = 'BomManage';
UPDATE sys_menu SET component_key = 'views/system/SystemManageLayout.vue' WHERE component_key = 'SystemManageLayout';
UPDATE sys_menu SET component_key = 'views/system/UserManageView.vue' WHERE component_key = 'UserManage';
UPDATE sys_menu SET component_key = 'components/system/RoleManagePanel.vue' WHERE component_key = 'RoleManagePanel';
UPDATE sys_menu SET component_key = 'components/system/MenuManagePanel.vue' WHERE component_key = 'MenuManagePanel';
UPDATE sys_menu SET component_key = 'views/system/CustomerManageView.vue' WHERE component_key = 'CustomerManage';
