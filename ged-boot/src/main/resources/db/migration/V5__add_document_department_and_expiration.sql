-- =========================================================================
-- V5__add_document_department_and_expiration.sql
-- Add department_id and expiration_date columns to documents table
-- =========================================================================

-- 1. Add department_id column with FK to departments
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS department_id UUID REFERENCES departments (id) ON DELETE SET NULL;

-- 2. Add expiration_date column
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS expiration_date DATE;

-- 3. Indexes for fast lookup/filtering
CREATE INDEX IF NOT EXISTS idx_documents_department_id ON documents (department_id);
CREATE INDEX IF NOT EXISTS idx_documents_expiration_date ON documents (expiration_date);
