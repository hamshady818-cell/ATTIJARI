-- V6__add_category_security_class.sql
-- ─────────────────────────────────────────────────────────────────────────────
-- Adds the security_class column to the categories table.
--
-- Purpose:
--   This column enables Keycloak-based document type access control at the
--   category level. The value (e.g., 'FINANCE') must match the suffix of a
--   Keycloak client role on ged-boot: DOC_TYPE_FINANCE.
--
-- When NULL: no document type restriction is enforced for this category.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS security_class VARCHAR(50);

-- Optional: index for fast lookups by security_class during access checks
CREATE INDEX IF NOT EXISTS idx_categories_security_class
    ON categories (security_class);
