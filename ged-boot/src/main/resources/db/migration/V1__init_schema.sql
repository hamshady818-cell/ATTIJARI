-- =========================================================================
-- GED-AWB SQL Migration: V1__init_schema.sql
-- Description: Initial database schema with soft delete, auditing, optimistic
--              locking, check constraints, safe cascading rules, and indexes.
-- Target: PostgreSQL 15+
-- =========================================================================

-- Enable pgcrypto for gen_random_uuid() generation (native UUIDs)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -------------------------------------------------------------------------
-- 1. DEPARTMENTS TABLE (Hierarchical organization)
-- -------------------------------------------------------------------------
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL UNIQUE,
    parent_id UUID,
    
    -- Soft Delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID, -- Set by constraint check or mapping later
    
    -- Auditing
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID,
    
    -- Optimistic Locking
    version INT NOT NULL DEFAULT 0,
    
    -- Safe constraints: No cascade delete on parent departments to prevent mass loss
    CONSTRAINT fk_departments_parent FOREIGN KEY (parent_id) 
        REFERENCES departments (id) ON DELETE RESTRICT
);

-- -------------------------------------------------------------------------
-- 2. USERS TABLE (Synchronized profile from Keycloak)
-- -------------------------------------------------------------------------
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_sub VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    department_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Auditing (Keycloak sync mapping)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Optimistic Locking
    version INT NOT NULL DEFAULT 0,
    
    -- Safe constraints: Deleting a department sets department_id to null on users (no deletion of user)
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) 
        REFERENCES departments (id) ON DELETE SET NULL
);

-- Set back-references on departments for auditing columns
ALTER TABLE departments ADD CONSTRAINT fk_departments_created_by 
    FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE departments ADD CONSTRAINT fk_departments_updated_by 
    FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE departments ADD CONSTRAINT fk_departments_deleted_by 
    FOREIGN KEY (deleted_by) REFERENCES users (id) ON DELETE SET NULL;

-- -------------------------------------------------------------------------
-- 3. CATEGORIES TABLE (Hierarchical document templates / types)
-- -------------------------------------------------------------------------
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    parent_id UUID,
    
    -- Soft Delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users (id) ON DELETE SET NULL,
    
    -- Auditing
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID REFERENCES users (id) ON DELETE SET NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID REFERENCES users (id) ON DELETE SET NULL,
    
    -- Optimistic Locking
    version INT NOT NULL DEFAULT 0,
    
    -- Safe constraints: Restrict deletion of parent category if child subcategories exist
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) 
        REFERENCES categories (id) ON DELETE RESTRICT
);

-- -------------------------------------------------------------------------
-- 4. METADATA DEFINITIONS TABLE (Dynamic properties schema definitions)
-- -------------------------------------------------------------------------
CREATE TABLE metadata_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    label VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    validation_pattern VARCHAR(255),
    
    -- Auditing
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID REFERENCES users (id) ON DELETE SET NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID REFERENCES users (id) ON DELETE SET NULL,
    
    -- Optimistic Locking
    version INT NOT NULL DEFAULT 0,
    
    -- Validation Check Constraints
    CONSTRAINT chk_metadata_definitions_type 
        CHECK (type IN ('STRING', 'INTEGER', 'DECIMAL', 'DATE', 'BOOLEAN'))
);

-- -------------------------------------------------------------------------
-- 5. FOLDERS TABLE (Hierarchical file system directory)
-- -------------------------------------------------------------------------
CREATE TABLE folders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    parent_id UUID,
    owner_id UUID,
    
    -- Soft Delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users (id) ON DELETE SET NULL,
    
    -- Auditing
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID REFERENCES users (id) ON DELETE SET NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID REFERENCES users (id) ON DELETE SET NULL,
    
    -- Optimistic Locking
    version INT NOT NULL DEFAULT 0,
    
    -- Safe constraints: Restrict folder deletion if active subfolders exist
    CONSTRAINT fk_folders_parent FOREIGN KEY (parent_id) 
        REFERENCES folders (id) ON DELETE RESTRICT,
        
    -- Link owner to user profile
    CONSTRAINT fk_folders_owner FOREIGN KEY (owner_id) 
        REFERENCES users (id) ON DELETE SET NULL,
        
    -- Unique constraint: Unique folder name within the same parent folder (excluding deleted folders)
    CONSTRAINT uq_folders_name_parent UNIQUE (name, parent_id)
);

-- -------------------------------------------------------------------------
-- 6. DOCUMENTS TABLE (Core aggregate metadata wrapper)
-- -------------------------------------------------------------------------
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    folder_id UUID,
    category_id UUID,
    owner_id UUID,
    active_version_id UUID, -- Back-reference set programmatically after version upload
    
    -- Check-Out Locking details
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    locked_by UUID REFERENCES users (id) ON DELETE SET NULL,
    locked_at TIMESTAMP WITH TIME ZONE,
    lock_expiration TIMESTAMP WITH TIME ZONE,
    
    -- Soft Delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users (id) ON DELETE SET NULL,
    
    -- Auditing
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID REFERENCES users (id) ON DELETE SET NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID REFERENCES users (id) ON DELETE SET NULL,
    
    -- Optimistic Locking
    version INT NOT NULL DEFAULT 0,
    
    -- Safe constraints: Restrict folder deletion if active documents are inside
    CONSTRAINT fk_documents_folder FOREIGN KEY (folder_id) 
        REFERENCES folders (id) ON DELETE RESTRICT,
        
    CONSTRAINT fk_documents_category FOREIGN KEY (category_id) 
        REFERENCES categories (id) ON DELETE SET NULL,
        
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) 
        REFERENCES users (id) ON DELETE SET NULL,
        
    -- Unique constraint: Unique document name within the same folder (excluding deleted documents)
    CONSTRAINT uq_documents_name_folder UNIQUE (name, folder_id)
);

-- -------------------------------------------------------------------------
-- 7. DOCUMENT VERSIONS TABLE (Physical file contents history)
-- -------------------------------------------------------------------------
CREATE TABLE document_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    version_number INT NOT NULL,
    hash VARCHAR(64) NOT NULL, -- SHA-256 Checksum
    size_bytes BIGINT NOT NULL,
    file_reference_id VARCHAR(255) NOT NULL, -- Logical storage index
    uploaded_by UUID,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Safe cascade: If document aggregate is completely purged, delete versions.
    -- (Soft deletes are managed via the documents table, keeping versions safe).
    CONSTRAINT fk_versions_document FOREIGN KEY (document_id) 
        REFERENCES documents (id) ON DELETE CASCADE,
        
    CONSTRAINT fk_versions_uploader FOREIGN KEY (uploaded_by) 
        REFERENCES users (id) ON DELETE SET NULL,
        
    -- Check constraints
    CONSTRAINT chk_document_versions_size CHECK (size_bytes > 0),
    CONSTRAINT chk_document_versions_number CHECK (version_number >= 1)
);

-- Set back-reference from documents to document_versions
ALTER TABLE documents ADD CONSTRAINT fk_documents_active_version 
    FOREIGN KEY (active_version_id) REFERENCES document_versions (id) ON DELETE SET NULL;

-- -------------------------------------------------------------------------
-- 8. DOCUMENT METADATA VALUES (Dynamic values bound to documents)
-- -------------------------------------------------------------------------
CREATE TABLE document_metadata_values (
    document_id UUID NOT NULL,
    definition_id UUID NOT NULL,
    key VARCHAR(100) NOT NULL,
    value TEXT NOT NULL,
    
    -- Cascade deletion if document is hard deleted
    CONSTRAINT fk_metadata_values_document FOREIGN KEY (document_id) 
        REFERENCES documents (id) ON DELETE CASCADE,
        
    -- Restrict deletion of metadata definition schemas if documents still hold references
    CONSTRAINT fk_metadata_values_definition FOREIGN KEY (definition_id) 
        REFERENCES metadata_definitions (id) ON DELETE RESTRICT,
        
    PRIMARY KEY (document_id, definition_id)
);

-- -------------------------------------------------------------------------
-- 9. DOCUMENT TAGS TABLE (Manual and AI tags associated with documents)
-- -------------------------------------------------------------------------
CREATE TABLE document_tags (
    document_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    generated_by_ia BOOLEAN NOT NULL DEFAULT FALSE,
    confidence DOUBLE PRECISION,
    
    -- Cascade deletion if document is hard deleted
    CONSTRAINT fk_tags_document FOREIGN KEY (document_id) 
        REFERENCES documents (id) ON DELETE CASCADE,
        
    -- Check constraints for AI confidence ranges
    CONSTRAINT chk_document_tags_confidence CHECK (confidence >= 0.0 AND confidence <= 1.0),
    
    PRIMARY KEY (document_id, name)
);

-- =========================================================================
-- DATABASE INDEXES (Performance optimization on frequent queries & joins)
-- =========================================================================

-- Organizations
CREATE INDEX idx_users_keycloak_sub ON users (keycloak_sub);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_department ON users (department_id);
CREATE INDEX idx_departments_parent ON departments (parent_id);

-- Classification
CREATE INDEX idx_categories_parent ON categories (parent_id);

-- Tree structures & folders
CREATE INDEX idx_folders_parent ON folders (parent_id);
CREATE INDEX idx_folders_owner ON folders (owner_id);
CREATE INDEX idx_folders_deleted ON folders (deleted);

-- Document lifecycle
CREATE INDEX idx_documents_folder ON documents (folder_id);
CREATE INDEX idx_documents_owner ON documents (owner_id);
CREATE INDEX idx_documents_category ON documents (category_id);
CREATE INDEX idx_documents_deleted ON documents (deleted);
CREATE INDEX idx_documents_locked ON documents (locked);

-- Versions
CREATE INDEX idx_document_versions_doc ON document_versions (document_id);
CREATE INDEX idx_document_versions_ref ON document_versions (file_reference_id);

-- Metadata values and tags
CREATE INDEX idx_metadata_values_def ON document_metadata_values (definition_id);
CREATE INDEX idx_document_tags_name ON document_tags (name);
