-- Remove the hidden project read placeholder that can leak into navigation.

DELETE FROM sys_role_menu WHERE menu_id = 23;
DELETE FROM sys_menu WHERE id = 23;
