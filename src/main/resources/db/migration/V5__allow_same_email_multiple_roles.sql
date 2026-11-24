-- V5__allow_same_email_multiple_roles.sql
-- Allow a single email to be used across multiple roles (BUYER, SELLER, ADMIN)
-- Clean legacy test data as requested

-- 1. Clean legacy non-admin test data from database
DELETE FROM partner_sellers WHERE email NOT IN ('admin@stockbridge.local', 'rudra090207@gmail.com');
DELETE FROM wholesale_buyers WHERE email NOT IN ('admin@stockbridge.local', 'rudra090207@gmail.com');
DELETE FROM app_user WHERE email NOT IN ('admin@stockbridge.local', 'rudra090207@gmail.com');
DELETE FROM organisation WHERE gst_number NOT IN ('ADMIN0000000001', 'RUDRA0000000001');

-- 2. Drop unique constraint on email in app_user
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS app_user_email_key;
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS uk_user_email;

-- 3. Add composite unique constraint on (email, role) so one email can have separate accounts for BUYER, SELLER, and ADMIN
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_app_user_email_role'
    ) THEN
        ALTER TABLE app_user ADD CONSTRAINT uk_app_user_email_role UNIQUE (email, role);
    END IF;
END $$;
