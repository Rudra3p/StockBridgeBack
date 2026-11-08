-- Seed initial Admin Organisation
INSERT INTO organisation (
    id,
    legal_name,
    org_type,
    gst_number,
    address_line,
    city,
    state,
    pin_code,
    verification_status,
    created_at,
    updated_at
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'StockBridge Platform Admin Org',
    'OTHER',
    'ADMIN0000000001',
    'StockBridge HQ, Tech Park',
    'Bengaluru',
    'Karnataka',
    '560001',
    'APPROVED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- Seed initial Admin User (email: admin@stockbridge.local, default password: admin123)
INSERT INTO app_user (
    id,
    organisation_id,
    full_name,
    email,
    phone,
    password_hash,
    role,
    enabled,
    created_at
) VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'StockBridge Administrator',
    'admin@stockbridge.local',
    '+919876543210',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
