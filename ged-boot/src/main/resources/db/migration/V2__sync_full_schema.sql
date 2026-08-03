-- =========================================================================
-- GED-AWB SQL Migration: V2__sync_full_schema.sql
-- Description: Synchronizes initial V1 baseline schema with full enterprise
--              JPA entity model (Roles, ACLs, EAV, OCR, RAG, Audit, Trash,
--              Archive, Favorites, Notifications).
-- Target: PostgreSQL 15+
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. USERS ALIGNMENT
-- -------------------------------------------------------------------------
ALTER TABLE users RENAME COLUMN keycloak_sub TO keycloak_id;
ALTER TABLE users RENAME COLUMN active TO is_active;

ALTER TABLE users ADD COLUMN username VARCHAR(150);
ALTER TABLE users ADD COLUMN job_title VARCHAR(150);
ALTER TABLE users ADD COLUMN avatar_url TEXT;
ALTER TABLE users ADD COLUMN locale VARCHAR(10) DEFAULT 'fr';
ALTER TABLE users ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN last_sync_at TIMESTAMP WITH TIME ZONE;

-- -------------------------------------------------------------------------
-- 2. ROLES & GROUPS
-- -------------------------------------------------------------------------
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    parent_group_id UUID REFERENCES groups(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_groups (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

-- -------------------------------------------------------------------------
-- 3. FOLDERS ALIGNMENT & PERMISSIONS
-- -------------------------------------------------------------------------
ALTER TABLE folders RENAME COLUMN parent_id TO parent_folder_id;
ALTER TABLE folders ADD COLUMN description VARCHAR(500);
ALTER TABLE folders ADD COLUMN path TEXT NOT NULL DEFAULT 'root';
ALTER TABLE folders ADD COLUMN color VARCHAR(7);
ALTER TABLE folders ADD COLUMN icon VARCHAR(50);
ALTER TABLE folders ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE folder_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folder_id UUID NOT NULL REFERENCES folders(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    can_read BOOLEAN NOT NULL DEFAULT FALSE,
    can_write BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    can_manage BOOLEAN NOT NULL DEFAULT FALSE,
    is_inherited BOOLEAN NOT NULL DEFAULT FALSE,
    granted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_folder_perm_principal CHECK (
        (user_id IS NOT NULL AND group_id IS NULL) OR
        (user_id IS NULL AND group_id IS NOT NULL)
    )
);

-- -------------------------------------------------------------------------
-- 4. CATEGORIES ALIGNMENT
-- -------------------------------------------------------------------------
ALTER TABLE categories RENAME COLUMN parent_id TO parent_category_id;
ALTER TABLE categories ADD COLUMN description VARCHAR(500);
ALTER TABLE categories ADD COLUMN path TEXT NOT NULL DEFAULT 'root';
ALTER TABLE categories ADD COLUMN color VARCHAR(7);
ALTER TABLE categories ADD COLUMN icon VARCHAR(50);

-- -------------------------------------------------------------------------
-- 5. DOCUMENTS ALIGNMENT
-- -------------------------------------------------------------------------
ALTER TABLE documents RENAME COLUMN name TO title;
ALTER TABLE documents RENAME COLUMN active_version_id TO current_version_id;

ALTER TABLE documents ADD COLUMN description TEXT;
ALTER TABLE documents ADD COLUMN document_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE documents ADD COLUMN mime_type VARCHAR(100);
ALTER TABLE documents ADD COLUMN file_extension VARCHAR(20);
ALTER TABLE documents ADD COLUMN language VARCHAR(10);
ALTER TABLE documents ADD COLUMN is_confidential BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE documents ADD COLUMN retention_until DATE;

CREATE TABLE document_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    can_read BOOLEAN NOT NULL DEFAULT FALSE,
    can_write BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    can_share BOOLEAN NOT NULL DEFAULT FALSE,
    granted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_doc_perm_principal CHECK (
        (user_id IS NOT NULL AND group_id IS NULL) OR
        (user_id IS NULL AND group_id IS NOT NULL)
    )
);

-- -------------------------------------------------------------------------
-- 6. DOCUMENT VERSIONS ALIGNMENT
-- -------------------------------------------------------------------------
ALTER TABLE document_versions RENAME COLUMN hash TO checksum_sha256;
ALTER TABLE document_versions RENAME COLUMN size_bytes TO file_size_bytes;
ALTER TABLE document_versions RENAME COLUMN uploaded_by TO created_by;
ALTER TABLE document_versions RENAME COLUMN uploaded_at TO created_at;

ALTER TABLE document_versions ADD COLUMN version_label VARCHAR(50);
ALTER TABLE document_versions ADD COLUMN storage_bucket VARCHAR(100) NOT NULL DEFAULT 'ged-documents';
ALTER TABLE document_versions ADD COLUMN storage_path VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE document_versions ADD COLUMN mime_type VARCHAR(100) NOT NULL DEFAULT 'application/octet-stream';
ALTER TABLE document_versions ADD COLUMN change_summary TEXT;
ALTER TABLE document_versions ADD COLUMN is_major_version BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE document_versions ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- -------------------------------------------------------------------------
-- 7. DOCUMENT CHECKOUTS
-- -------------------------------------------------------------------------
CREATE TABLE document_checkouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    checked_out_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    checked_out_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checkout_comment TEXT,
    expected_return_at TIMESTAMP WITH TIME ZONE,
    checked_in_at TIMESTAMP WITH TIME ZONE,
    new_version_id UUID REFERENCES document_versions(id) ON DELETE SET NULL,
    forced_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------------------
-- 8. TAGS ALIGNMENT
-- -------------------------------------------------------------------------
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(7),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS document_tags;
CREATE TABLE document_tags (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, tag_id)
);

CREATE TABLE document_categories (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, category_id)
);

-- -------------------------------------------------------------------------
-- 9. METADATA DEFINITIONS & VALUES ALIGNMENT
-- -------------------------------------------------------------------------
ALTER TABLE metadata_definitions RENAME COLUMN name TO field_name;
ALTER TABLE metadata_definitions RENAME COLUMN label TO display_label;
ALTER TABLE metadata_definitions RENAME COLUMN type TO field_type;
ALTER TABLE metadata_definitions RENAME COLUMN required TO is_required;
ALTER TABLE metadata_definitions RENAME COLUMN validation_pattern TO validation_regex;

ALTER TABLE metadata_definitions ADD COLUMN is_searchable BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE metadata_definitions ADD COLUMN is_filterable BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE metadata_definitions ADD COLUMN default_value TEXT;
ALTER TABLE metadata_definitions ADD COLUMN allowed_values JSONB;
ALTER TABLE metadata_definitions ADD COLUMN display_order INT NOT NULL DEFAULT 0;
ALTER TABLE metadata_definitions ADD COLUMN category_id UUID REFERENCES categories(id) ON DELETE SET NULL;

DROP TABLE IF EXISTS document_metadata_values;
CREATE TABLE document_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    metadata_definition_id UUID NOT NULL REFERENCES metadata_definitions(id) ON DELETE CASCADE,
    value_text TEXT,
    value_json JSONB,
    updated_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_doc_metadata_doc_def UNIQUE (document_id, metadata_definition_id)
);

-- -------------------------------------------------------------------------
-- 10. AI MODULE TABLES
-- -------------------------------------------------------------------------
CREATE TABLE ai_ocr_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL UNIQUE REFERENCES document_versions(id) ON DELETE CASCADE,
    extracted_text TEXT,
    page_count INT,
    confidence_score NUMERIC(5, 4),
    ocr_engine VARCHAR(50) NOT NULL DEFAULT 'TESSERACT',
    language_detected VARCHAR(10),
    page_data JSONB,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    processing_time_ms INT,
    error_message TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_document_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL UNIQUE REFERENCES document_versions(id) ON DELETE CASCADE,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50),
    summary TEXT,
    suggested_categories JSONB,
    extracted_entities JSONB,
    key_phrases JSONB,
    sentiment VARCHAR(20),
    risk_score NUMERIC(5, 4),
    risk_flags JSONB,
    suggested_tags JSONB,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL REFERENCES document_versions(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_start_char INT,
    chunk_end_char INT,
    embedding_model VARCHAR(100) NOT NULL,
    vector_store_id VARCHAR(255),
    vector_collection VARCHAR(100),
    token_count INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_embedding_version_chunk UNIQUE (version_id, chunk_index)
);

CREATE TABLE ai_chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255),
    scope_type VARCHAR(30) NOT NULL,
    scope_entity_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    retrieved_chunks JSONB,
    source_documents JSONB,
    token_count INT,
    latency_ms INT,
    feedback VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------------------
-- 11. GOVERNANCE, AUDIT & NOTIFICATIONS
-- -------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    entity_name VARCHAR(500),
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    session_id VARCHAR(255),
    correlation_id UUID,
    metadata JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_favorites_user_entity UNIQUE (user_id, entity_type, entity_id)
);

CREATE TABLE trash (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    original_folder_id UUID REFERENCES folders(id) ON DELETE SET NULL,
    deleted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    deleted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auto_purge_at TIMESTAMP WITH TIME ZONE NOT NULL,
    purged_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE archives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL UNIQUE REFERENCES documents(id) ON DELETE CASCADE,
    archive_reason TEXT,
    archived_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    archived_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_policy VARCHAR(100),
    storage_tier VARCHAR(30) NOT NULL DEFAULT 'COLD',
    restore_requested_at TIMESTAMP WITH TIME ZONE,
    restored_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE search_index_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL UNIQUE REFERENCES document_versions(id) ON DELETE CASCADE,
    index_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    index_engine VARCHAR(50),
    indexed_at TIMESTAMP WITH TIME ZONE,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    read_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notif_sub_user_event_entity_channel UNIQUE (user_id, event_type, entity_type, entity_id, channel)
);

-- -------------------------------------------------------------------------
-- 12. NEW INDEXES
-- -------------------------------------------------------------------------
CREATE INDEX idx_user_roles_user ON user_roles (user_id);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);
CREATE INDEX idx_user_groups_user ON user_groups (user_id);
CREATE INDEX idx_user_groups_group ON user_groups (group_id);
CREATE INDEX idx_folder_perm_folder ON folder_permissions (folder_id);
CREATE INDEX idx_doc_perm_document ON document_permissions (document_id);
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_occurred ON audit_logs (occurred_at DESC);
