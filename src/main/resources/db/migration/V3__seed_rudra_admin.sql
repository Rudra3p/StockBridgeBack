-- Seed or update primary Admin User (email: rudra090207@gmail.com, phone: 7016047658, password: 1234567890)
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
    'a0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'Rudra Admin',
    'rudra090207@gmail.com',
    '7016047658',
    '$2a$10$liyVwCsm6TUQxeu0yLtFqe40peB.DnLG4mZSucFOx.L6iNWjSl15O',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO UPDATE SET
    phone = '7016047658',
    password_hash = '$2a$10$liyVwCsm6TUQxeu0yLtFqe40peB.DnLG4mZSucFOx.L6iNWjSl15O',
    role = 'ADMIN',
    enabled = true;
