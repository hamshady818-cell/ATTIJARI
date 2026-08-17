-- =========================================================================
-- GED-AWB SQL Migration: V10__add_active_to_categories.sql
-- Description: Adds is_active boolean column to categories table.
-- Target: PostgreSQL 15+
-- =========================================================================

ALTER TABLE categories
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
