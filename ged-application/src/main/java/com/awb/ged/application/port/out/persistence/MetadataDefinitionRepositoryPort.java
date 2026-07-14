package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.metadata.model.MetadataDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h1>MetadataDefinitionRepositoryPort</h1>
 * <p>
 * Output Port interface representing persistence capabilities for the {@link MetadataDefinition} domain entity.
 * Implemented by persistence adapters in the infrastructure layer.
 * </p>
 */
public interface MetadataDefinitionRepositoryPort {

    /**
     * Persists or updates a metadata definition.
     *
     * @param definition the definition to save
     * @return the saved definition
     */
    MetadataDefinition save(MetadataDefinition definition);

    /**
     * Resolves a metadata definition by ID.
     *
     * @param id the definition UUID
     * @return an {@link Optional} containing the definition, or empty
     */
    Optional<MetadataDefinition> findById(UUID id);

    /**
     * Resolves a metadata definition by its unique key/name.
     *
     * @param name the unique code name (e.g. "invoice_amount")
     * @return an {@link Optional} containing the definition, or empty
     */
    Optional<MetadataDefinition> findByName(String name);

    /**
     * Lists all metadata definitions.
     *
     * @return list of all definitions
     */
    List<MetadataDefinition> findAll();

    /**
     * Deletes a metadata definition.
     *
     * @param id the definition UUID
     */
    void delete(UUID id);
}
