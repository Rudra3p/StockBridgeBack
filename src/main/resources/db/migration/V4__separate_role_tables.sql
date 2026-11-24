-- V4__separate_role_tables.sql
-- Create distinct tables for Partner Sellers and Wholesale Buyers, and clean existing test data

-- 1. Clean up legacy non-admin test data from database as requested
DELETE FROM app_user WHERE email NOT IN ('admin@stockbridge.local', 'rudra090207@gmail.com');
DELETE FROM organisation WHERE gst_number NOT IN ('ADMIN0000000001', 'RUDRA0000000001');

-- 2. Create distinct table for Partners / Sellers
CREATE TABLE IF NOT EXISTS partner_sellers (
    id UUID PRIMARY KEY,
    organisation_id UUID NOT NULL REFERENCES organisation(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_partner_sellers_email ON partner_sellers(email);
CREATE INDEX IF NOT EXISTS idx_partner_sellers_org ON partner_sellers(organisation_id);

-- 3. Create distinct table for Wholesale Buyers
CREATE TABLE IF NOT EXISTS wholesale_buyers (
    id UUID PRIMARY KEY,
    organisation_id UUID NOT NULL REFERENCES organisation(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wholesale_buyers_email ON wholesale_buyers(email);
CREATE INDEX IF NOT EXISTS idx_wholesale_buyers_org ON wholesale_buyers(organisation_id);
