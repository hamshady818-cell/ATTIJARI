package com.awb.ged.application.port.out.persistence;

import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.metadata.model.MetadataDefinition;

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
     * Resolves an active metadata definition by ID.
     *
     * @param id the definition UUID
     * @return an {@link Optional} containing the definition, or empty
     */
    Optional<MetadataDefinition> findById(UUID id);

    /**
     * Resolves an active metadata definition by its unique key/name.
     *
     * @param name the unique code name (e.g. "invoice_amount")
     * @return an {@link Optional} containing the definition, or empty
     */
    Optional<MetadataDefinition> findByName(String name);

    /**
     * Lists active metadata definitions with pagination.
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated response of active metadata definitions
     */
    PageResponse<MetadataDefinition> findAllActive(int page, int size);

    /**
     * Lists soft-deleted metadata definitions with pagination.
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated response of deleted metadata definitions
     */
    PageResponse<MetadataDefinition> findAllDeleted(int page, int size);

    /**
     * Marks a metadata definition as soft-deleted.
     *
     * @param id the definition UUID
     * @param deletedByUserId the ID of the user performing the deletion
     */
    void softDelete(UUID id, UUID deletedByUserId);

    /**
     * Restores a previously soft-deleted metadata definition.
     *
     * @param id the definition UUID
     * @return the restored definition
     */
    MetadataDefinition restore(UUID id);
}
