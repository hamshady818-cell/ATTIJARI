package com.awb.ged.application.service.trash;

import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.in.trash.RestoreFromTrashUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.TrashRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.folder.model.Folder;
import com.awb.ged.domain.trash.model.TrashItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RestoreFromTrashService implements RestoreFromTrashUseCase {

    private final TrashRepositoryPort trashRepositoryPort;
    private final DocumentRepositoryPort documentRepositoryPort;
    private final FolderRepositoryPort folderRepositoryPort;
    private final DocumentAccessValidator documentAccessValidator;

    @Autowired
    public RestoreFromTrashService(TrashRepositoryPort trashRepositoryPort,
                                  DocumentRepositoryPort documentRepositoryPort,
                                  FolderRepositoryPort folderRepositoryPort,
                                  DocumentAccessValidator documentAccessValidator) {
        this.trashRepositoryPort = trashRepositoryPort;
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderRepositoryPort = folderRepositoryPort;
        this.documentAccessValidator = documentAccessValidator;
    }

    public RestoreFromTrashService(TrashRepositoryPort trashRepositoryPort,
                                  DocumentRepositoryPort documentRepositoryPort,
                                  FolderRepositoryPort folderRepositoryPort) {
        this(trashRepositoryPort, documentRepositoryPort, folderRepositoryPort, null);
    }

    @Override
    public void restoreFromTrash(UUID trashId, UUID userId) {
        // 1. Retrieve trash item
        TrashItem item = trashRepositoryPort.findById(trashId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Trash item with ID " + trashId + " was not found."
                ));

        // 2. Perform restoration depending on type
        String type = item.getEntityType().toUpperCase();
        if ("DOCUMENT".equals(type)) {
            Document doc = documentRepositoryPort.findByIdIncludingDeleted(item.getEntityId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.DOCUMENT_NOT_FOUND,
                            "Document with ID " + item.getEntityId() + " was not found."
                    ));

            if (documentAccessValidator != null) {
                documentAccessValidator.validateAccess(doc, userId, "WRITE");
            }
            doc.setDeletedAt(null);
            doc.setDeletedBy(null);
            doc.setStatus(Document.DocumentStatus.DRAFT);
            documentRepositoryPort.save(doc);
        } else if ("FOLDER".equals(type)) {
            Folder folder = folderRepositoryPort.findByIdIncludingDeleted(item.getEntityId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.FOLDER_NOT_FOUND,
                            "Folder with ID " + item.getEntityId() + " was not found."
                    ));
            folder.setDeletedAt(null);
            folder.setDeletedBy(null);
            folderRepositoryPort.save(folder);
        } else {
            throw new IllegalArgumentException("Unsupported trash entity type: " + item.getEntityType());
        }

        // 3. Remove item from trash (or mark as purged/restored. Since it's restored, we delete the trash record)
        trashRepositoryPort.delete(trashId);
    }
}
