ALTER TABLE metadata_definitions DROP CONSTRAINT chk_metadata_definitions_type;

ALTER TABLE metadata_definitions ADD CONSTRAINT chk_metadata_definitions_type
    CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'SELECT', 'MULTI_SELECT', 'URL'));
