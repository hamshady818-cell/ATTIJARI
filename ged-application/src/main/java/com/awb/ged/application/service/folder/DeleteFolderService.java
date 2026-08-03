package com.awb.ged.application.service.folder;

import com.awb.ged.application.port.in.folder.DeleteFolderUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.folder.event.FolderDeletedEvent;
import com.awb.ged.domain.folder.model.Folder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeleteFolderService implements DeleteFolderUseCase {

    private final FolderRepositoryPort folderRepositoryPort;
    private final DocumentRepositoryPort documentRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    @Autowired
    public DeleteFolderService(FolderRepositoryPort folderRepositoryPort,
                                 DocumentRepositoryPort documentRepositoryPort,
                                 EventPublisherPort eventPublisherPort) {
        this.folderRepositoryPort = folderRepositoryPort;
        this.documentRepositoryPort = documentRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public void deleteFolder(UUID folderId, UUID deletedByUserId) {
        // 1. Find folder
        Folder folder = folderRepositoryPort.findById(folderId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.FOLDER_NOT_FOUND,
                        "Folder with ID " + folderId + " was not found."
                ));

        // 2. Verify folder has no active subfolders
        List<Folder> activeSubfolders = folderRepositoryPort.findByParentId(folderId);
        if (!activeSubfolders.isEmpty()) {
            throw new ConflictException(
                    ErrorCode.FOLDER_NOT_EMPTY,
                    "Cannot delete folder with ID " + folderId + " because it contains active subfolders."
            );
        }

        // 3. Verify folder has no active documents
        List<Document> activeDocuments = documentRepositoryPort.findByFolderId(folderId);
        if (!activeDocuments.isEmpty()) {
            throw new ConflictException(
                    ErrorCode.FOLDER_NOT_EMPTY,
                    "Cannot delete folder with ID " + folderId + " because it contains active documents."
            );
        }

        // 4. Soft-delete the folder
        folder.setDeletedAt(Instant.now());
        folder.setDeletedBy(deletedByUserId);

        folderRepositoryPort.save(folder);

        // 5. Publish Domain Event
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
