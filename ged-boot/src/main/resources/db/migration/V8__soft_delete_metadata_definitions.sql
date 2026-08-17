ALTER TABLE metadata_definitions ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE metadata_definitions ADD COLUMN deleted_by UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_metadata_definitions_deleted_at ON metadata_definitions (deleted_at);

-- La contrainte UNIQUE globale sur field_name empêcherait de recréer une définition
-- avec le même nom qu'une définition soft-deleted. On la remplace par un index
-- unique partiel qui ne s'applique qu'aux définitions actives.
ALTER TABLE metadata_definitions DROP CONSTRAINT IF EXISTS metadata_definitions_field_name_key;
ALTER TABLE metadata_definitions DROP CONSTRAINT IF EXISTS metadata_definitions_name_key;
CREATE UNIQUE INDEX uq_metadata_definitions_field_name_active ON metadata_definitions (field_name) WHERE deleted_at IS NULL;

-- Sécurité supplémentaire : repasser la FK de document_metadata en RESTRICT.
-- Avec le soft delete en place, on ne fait plus jamais de vrai DELETE en usage
-- normal, donc CASCADE ne devrait plus jamais se déclencher — mais on garde
-- un filet de sécurité si un DELETE SQL direct est fait par erreur.
ALTER TABLE document_metadata DROP CONSTRAINT IF EXISTS document_metadata_metadata_definition_id_fkey;
ALTER TABLE document_metadata DROP CONSTRAINT IF EXISTS fk_doc_metadata_definition;
ALTER TABLE document_metadata ADD CONSTRAINT fk_doc_metadata_definition_restrict FOREIGN KEY (metadata_definition_id) REFERENCES metadata_definitions(id) ON DELETE RESTRICT;
