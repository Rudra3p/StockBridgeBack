-- V6__create_admin_users_table.sql
-- Create distinct table for Admin Users (completing strict 3-table separation: wholesale_buyers, partner_sellers, admin_users)

-- 1. Create dedicated admin_users table
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_users_email ON admin_users(email);

-- 2. Populate admin_users from existing admin records if present in app_user
INSERT INTO admin_users (id, full_name, email, phone, password_hash, enabled, created_at)
SELECT id, full_name, email, phone, password_hash, enabled, created_at
FROM app_user
WHERE role = 'ADMIN'
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;

-- 3. Ensure dedicated master administrators are seeded into admin_users table
INSERT INTO admin_users (id, full_name, email, phone, password_hash, enabled, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'StockBridge Platform Admin',
    'admin@stockbridge.local',
    '+919876543210',
    '$2a$10$liyVwCsm6TUQxeu0yLtFqe40peB.DnLG4mZSucFOx.L6iNWjSl15O',
    true,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;

INSERT INTO admin_users (id, full_name, email, phone, password_hash, enabled, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'Rudra Admin',
    'rudra090207@gmail.com',
    '7016047658',
    '$2a$10$liyVwCsm6TUQxeu0yLtFqe40peB.DnLG4mZSucFOx.L6iNWjSl15O',
    true,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;
