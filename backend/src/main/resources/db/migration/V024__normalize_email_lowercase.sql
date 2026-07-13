-- Normalize existing user and customer emails to trimmed lowercase.
UPDATE sys_user
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL;

UPDATE sys_customer
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL;
