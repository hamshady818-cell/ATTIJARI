-- Retire les anciennes contraintes sensibles à la casse
ALTER TABLE folders DROP CONSTRAINT IF EXISTS uq_folders_name_parent;
ALTER TABLE documents DROP CONSTRAINT IF EXISTS uq_documents_name_folder;

-- Ajoute des index uniques insensibles à la casse
CREATE UNIQUE INDEX uq_folders_name_parent_ci
    ON folders (LOWER(name), parent_folder_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_documents_title_folder_ci
    ON documents (LOWER(title), folder_id)
    WHERE deleted_at IS NULL;