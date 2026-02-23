-- Ajasta Database Schema
-- PostgreSQL tables for Resources and Bookings

-- Create Keycloak database
CREATE DATABASE keycloak;

-- Resources table
CREATE TABLE IF NOT EXISTS resources (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    type TEXT NOT NULL,
    location TEXT,
    image_url TEXT,
    price_per_slot DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    units_count INTEGER NOT NULL DEFAULT 1,
    open_time TEXT,
    close_time TEXT,
    rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    owner_id TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    unavailable_weekdays TEXT,
    unavailable_dates TEXT,
    daily_unavailable_ranges TEXT,
    lock TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT
);

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    resource_id TEXT NOT NULL REFERENCES resources(id),
    user_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    slots TEXT NOT NULL DEFAULT '[]',
    lock TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_resources_type ON resources(type);
CREATE INDEX IF NOT EXISTS idx_resources_owner_id ON resources(owner_id);
CREATE INDEX IF NOT EXISTS idx_resources_location ON resources(location);
CREATE INDEX IF NOT EXISTS idx_bookings_resource_id ON bookings(resource_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
