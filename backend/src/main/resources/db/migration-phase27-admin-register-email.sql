-- 第二十七期：admin 种子邮箱 + 注册可选邮箱支持（忘记密码闭环）
UPDATE sys_user
SET email = 'admin@example.com'
WHERE username = 'admin' AND (email IS NULL OR TRIM(email) = '');
