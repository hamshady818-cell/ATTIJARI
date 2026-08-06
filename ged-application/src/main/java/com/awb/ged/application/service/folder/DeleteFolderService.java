package com.awb.ged.application.service.folder;

import com.awb.ged.application.port.in.folder.DeleteFolderUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.CascadeDeleteFolderPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.event.DocumentDeletedEvent;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.folder.event.FolderDeletedEvent;
import com.awb.ged.domain.folder.model.Folder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeleteFolderService implements DeleteFolderUseCase {

    private final FolderRepositoryPort folderRepositoryPort;
    private final DocumentRepositoryPort documentRepositoryPort;
    private final CascadeDeleteFolderPort cascadeDeleteFolderPort;
    private final EventPublisherPort eventPublisherPort;

    @Autowired
    public DeleteFolderService(FolderRepositoryPort folderRepositoryPort,
                               DocumentRepositoryPort documentRepositoryPort,
                               CascadeDeleteFolderPort cascadeDeleteFolderPort,
                               EventPublisherPort eventPublisherPort) {
        this.folderRepositoryPort = folderRepositoryPort;
        this.documentRepositoryPort = documentRepositoryPort;
        this.cascadeDeleteFolderPort = cascadeDeleteFolderPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public void deleteFolder(UUID folderId, UUID deletedByUserId, boolean cascade) {
        // 1. Find the target folder (fails fast if not found)
        Folder folder = folderRepositoryPort.findById(folderId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.FOLDER_NOT_FOUND,
                        "Folder with ID " + folderId + " was not found."
                ));

        if (!cascade) {
            // 2a. Strict mode — block if folder is not empty
            List<Folder> activeSubfolders = folderRepositoryPort.findByParentId(folderId);
            if (!activeSubfolders.isEmpty()) {
                throw new ConflictException(
                        ErrorCode.FOLDER_NOT_EMPTY,
                        "Cannot delete folder with ID " + folderId + " because it contains active subfolders."
                );
            }
            List<Document> activeDocuments = documentRepositoryPort.findByFolderId(folderId);
            if (!activeDocuments.isEmpty()) {
                throw new ConflictException(
                        ErrorCode.FOLDER_NOT_EMPTY,
                        "Cannot delete folder with ID " + folderId + " because it contains active documents."
                );
            }
        } else {
            // 2b. Cascade mode — use bulk JPQL to avoid Hibernate persistence-context conflicts.
            //     Collect all descendant folder IDs (leaves first), then soft-delete their documents
            //     and the folders themselves in bulk. Finally soft-delete the root folder.
            Instant now = Instant.now();

            // Collect descendant IDs (not including folderId itself)
            List<UUID> descendantIds = cascadeDeleteFolderPort.collectDescendantFolderIds(folderId);

            // All folder IDs to process for document bulk-delete: descendants + root
            List<UUID> allFolderIds = new ArrayList<>(descendantIds);
            allFolderIds.add(folderId);

            // Bulk soft-delete documents inside all these folders
            List<UUID> deletedDocIds = cascadeDeleteFolderPort.bulkSoftDeleteDocumentsInFolders(allFolderIds, now);

            // Publish DocumentDeletedEvent for each document so TrashEventListener registers them
            if (eventPublisherPort != null) {
                for (UUID docId : deletedDocIds) {
                    // We publish with folderId=null since the original folder will also be deleted
                    eventPublisherPort.publish(new DocumentDeletedEvent(docId, "document", null, deletedByUserId));
                }
            }

            // Bulk soft-delete all descendant folders (leaves first — already ordered that way)
            if (!descendantIds.isEmpty()) {
                cascadeDeleteFolderPort.bulkSoftDeleteFolders(descendantIds, now);
                // Publish FolderDeletedEvent for each descendant folder
                if (eventPublisherPort != null) {
                    for (UUID subId : descendantIds) {
                        eventPublisherPort.publish(new FolderDeletedEvent(subId, "subfolder", folderId, deletedByUserId));
                    }
                }
            }
        }

        // 3. Soft-delete the root folder itself (via domain port — single object, no conflict)
        folder.setDeletedAt(Instant.now());
        folder.setDeletedBy(deletedByUserId);
        folderRepositoryPort.save(folder);

        // 4. Publish domain event for the root folder
        if (eventPublisherPort != null) {
            eventPublisherPort.publish(new FolderDeletedEvent(
                    folder.getId(),
                    folder.getName(),
                    folder.getParentId(),
                    folder.getDeletedBy()
            ));
        }
    }
}
