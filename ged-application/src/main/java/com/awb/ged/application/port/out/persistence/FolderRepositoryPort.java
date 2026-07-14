package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.folder.model.Folder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h1>FolderRepositoryPort</h1>
 * <p>
 * Output Port interface representing persistence capabilities for the {@link Folder} domain entity.
 * Implemented by persistence adapters in the infrastructure layer.
 * </p>
 */
public interface FolderRepositoryPort {

    /**
     * Persists or updates a folder.
     *
     * @param folder the folder to save
     * @return the saved folder
     */
    Folder save(Folder folder);

    /**
     * Resolves a folder by ID.
     *
     * @param id the folder UUID
     * @return an {@link Optional} containing the folder, or empty
     */
    Optional<Folder> findById(UUID id);

    /**
     * Finds folders located under a specific parent folder.
     *
     * @param parentId the parent folder UUID, or null to find root folders
     * @return list of child folders
     */
    List<Folder> findByParentId(UUID parentId);

    /**
     * Lists all folders.
     *
     * @return list of all folders
     */
    List<Folder> findAll();

    /**
     * Deletes a folder.
     *
     * @param id the folder UUID
     */
    void delete(UUID id);
}
