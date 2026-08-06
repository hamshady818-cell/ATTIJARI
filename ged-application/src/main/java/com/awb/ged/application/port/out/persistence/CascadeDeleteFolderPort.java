package com.awb.ged.application.port.out.persistence;

import java.util.List;
import java.util.UUID;

/**
 * Output port for cascade (force) folder deletion operations.
 * Implementations use bulk JPQL updates that bypass Hibernate's first-level
 * cache to avoid DuplicateKeyException when the same entity is already managed.
 */
public interface CascadeDeleteFolderPort {

    /**
     * Collects all active descendant folder IDs under the given root folder
     * (recursively, depth-first), NOT including the root folder itself.
     *
     * @param rootFolderId the folder whose subtree to collect
     * @return list of descendant folder IDs (may be empty)
     */
    List<UUID> collectDescendantFolderIds(UUID rootFolderId);

    /**
     * Bulk soft-deletes all active documents that belong to any of the given folder IDs.
     * Returns the list of document IDs that were soft-deleted (so events can be published).
     *
     * @param folderIds   folders whose documents should be soft-deleted
     * @param deletedAt   timestamp to stamp on deleted_at
     * @return IDs of documents that were soft-deleted
     */
    List<UUID> bulkSoftDeleteDocumentsInFolders(List<UUID> folderIds, java.time.Instant deletedAt);

    /**
     * Bulk soft-deletes a list of folder IDs directly.
     *
     * @param folderIds IDs to soft-delete
     * @param deletedAt timestamp to stamp on deleted_at
     */
    void bulkSoftDeleteFolders(List<UUID> folderIds, java.time.Instant deletedAt);
}
