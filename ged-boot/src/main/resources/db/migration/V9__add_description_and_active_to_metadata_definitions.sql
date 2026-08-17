ALTER TABLE metadata_definitions
    ADD COLUMN description VARCHAR(500);

ALTER TABLE metadata_definitions
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
