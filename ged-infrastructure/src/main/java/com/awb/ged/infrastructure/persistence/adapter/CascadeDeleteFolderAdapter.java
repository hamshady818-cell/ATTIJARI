package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.CascadeDeleteFolderPort;
import com.awb.ged.infrastructure.persistence.repository.DocumentJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.FolderJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Infrastructure adapter for cascade folder deletion using bulk JPQL queries.
 * Avoids Hibernate persistence-context conflicts (DuplicateKeyException) by
 * never loading individual entities — only IDs are fetched and bulk UPDATE statements
 * are issued directly against the database.
 */
@Component
@Transactional
public class CascadeDeleteFolderAdapter implements CascadeDeleteFolderPort {

    private final FolderJpaRepository folderJpaRepository;
    private final DocumentJpaRepository documentJpaRepository;

    public CascadeDeleteFolderAdapter(FolderJpaRepository folderJpaRepository,
                                      DocumentJpaRepository documentJpaRepository) {
        this.folderJpaRepository = folderJpaRepository;
        this.documentJpaRepository = documentJpaRepository;
    }

    @Override
    public List<UUID> collectDescendantFolderIds(UUID rootFolderId) {
        List<UUID> result = new ArrayList<>();
        collectRecursive(rootFolderId, result);
        return result;
    }

    private void collectRecursive(UUID parentId, List<UUID> accumulator) {
        List<UUID> childIds = folderJpaRepository.findActiveChildIds(parentId);
        for (UUID childId : childIds) {
            // Depth-first: recurse into grand-children first so we soft-delete leaves before parents
            collectRecursive(childId, accumulator);
            accumulator.add(childId);
        }
    }

    @Override
    public List<UUID> bulkSoftDeleteDocumentsInFolders(List<UUID> folderIds, Instant deletedAt) {
        if (folderIds.isEmpty()) return List.of();

        List<UUID> allDocIds = new ArrayList<>();
        for (UUID folderId : folderIds) {
            allDocIds.addAll(documentJpaRepository.findActiveIdsByFolderId(folderId));
        }

        if (!allDocIds.isEmpty()) {
            documentJpaRepository.bulkSoftDelete(allDocIds, deletedAt);
        }
        return allDocIds;
    }

    @Override
    public void bulkSoftDeleteFolders(List<UUID> folderIds, Instant deletedAt) {
        if (folderIds.isEmpty()) return;
        folderJpaRepository.bulkSoftDelete(folderIds, deletedAt);
    }
}
