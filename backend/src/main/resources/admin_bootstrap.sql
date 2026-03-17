-- ============================================================
-- ADMIN BOOTSTRAP MIGRATION SCRIPT
-- Enterprise Expense Compliance & Audit System
-- Run ONCE after initial database init.
-- This script creates the first ADMIN user securely.
-- ============================================================

-- Password below is BCrypt hash of: Admin@Aegis2024!
-- CHANGE THIS PASSWORD IMMEDIATELY after first login.
-- Generated with strength 12: $2a$12$...

SET @admin_password = '$2a$12$LKkx3jFkRH5FZzPr9Wvr5O7tmIEL93U9rCkDk0C8oBK4HhVYiZLyW';

-- 1. Insert ADMIN user (will skip if username already exists)
INSERT INTO users (username, email, password, active, deleted_at, department_id)
SELECT 'sysadmin', 'admin@aegis-enterprise.internal', @admin_password, 1, NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'sysadmin'
);

-- 2. Ensure ROLE_ADMIN exists in roles table
INSERT INTO roles (name)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN'
);

-- 3. Assign ROLE_ADMIN to sysadmin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'sysadmin'
  AND r.name = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- ============================================================
-- LOGIN CREDENTIALS (CHANGE IMMEDIATELY)
-- Username : sysadmin
-- Password : Admin@Aegis2024!
-- ============================================================
