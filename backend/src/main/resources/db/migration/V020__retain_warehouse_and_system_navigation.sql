-- Retain warehouse and system management as the only visible top-level navigation modules.
-- Other modules are hidden (not deleted) for future development.

UPDATE sys_menu
SET parent_id = NULL,
    icon = 'InboxOutlined',
    visible = 1,
    sort_order = 10
WHERE id = 110;

UPDATE sys_menu
SET visible = 0
WHERE id IN (
    10,
    20, 21, 22, 23,
    100,
    120, 121, 122, 123,
    150,
    160, 161, 162, 163,
    170,
    180, 181, 182, 183
);

UPDATE sys_menu
SET visible = 1,
    sort_order = 20
WHERE id = 200;
