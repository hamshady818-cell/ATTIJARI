-- =========================================================================
-- GED-AWB SQL Migration: V4__features_extension.sql
-- Description: Indexes for multi-filter search, document status tracking,
--              and checkout lock queries.
-- Target: PostgreSQL 15+
-- =========================================================================

-- Performance index for full-text / title search
CREATE INDEX IF NOT EXISTS idx_documents_title_lower ON documents (LOWER(title));

-- Performance index for search by status & created_at
CREATE INDEX IF NOT EXISTS idx_documents_status_created ON documents (document_status, created_at DESC);

-- Partial index for active checkouts (checked_in_at IS NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_checkout_doc
    ON document_checkouts (document_id)
    WHERE checked_in_at IS NULL;
