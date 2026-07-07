-- Nested system-management routes for dynamic registration and JWT revocation blacklist.

CREATE TABLE IF NOT EXISTS jwt_revoked_token (
    jti VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'JWT ID',
    expires_at TIMESTAMP NOT NULL COMMENT 'Token expiration for cleanup',
    revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Revocation time'
);

UPDATE sys_menu SET parent_id = 201, path = 'roles', visible = 0 WHERE id = 202;
UPDATE sys_menu SET parent_id = 201, path = 'menus', visible = 0 WHERE id = 203;
