package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.BulkActionResultDto;
import com.awb.ged.application.port.in.document.BulkDocumentActionUseCase;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.event.DocumentDeletedEvent;
import com.awb.ged.domain.document.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BulkDocumentActionService implements BulkDocumentActionUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final FolderRepositoryPort folderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;
    private final DocumentAccessValidator documentAccessValidator;
    private final DocumentLockGuard documentLockGuard;

    @Autowired
    public BulkDocumentActionService(DocumentRepositoryPort documentRepositoryPort,
                                     FolderRepositoryPort folderRepositoryPort,
                                     EventPublisherPort eventPublisherPort,
                                     DocumentAccessValidator documentAccessValidator,
                                     DocumentLockGuard documentLockGuard) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderRepositoryPort = folderRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.documentAccessValidator = documentAccessValidator;
        this.documentLockGuard = documentLockGuard;
    }

    public BulkDocumentActionService(DocumentRepositoryPort documentRepositoryPort,
                                     FolderRepositoryPort folderRepositoryPort) {
        this(documentRepositoryPort, folderRepositoryPort, null, null, null);
    }

    @Override
    public BulkActionResultDto bulkDelete(List<UUID> documentIds, UUID performedBy) {
        Instant now = Instant.now();
        int processedCount = 0;
        List<UUID> skippedIds = new ArrayList<>();
        List<String> skippedNames = new ArrayList<>();

        for (UUID id : documentIds) {
            Optional<Document> maybeDoc = documentRepositoryPort.findById(id);
            if (maybeDoc.isEmpty()) continue;
            Document doc = maybeDoc.get();

            if (documentLockGuard != null) {
                try {
                    documentLockGuard.assertNotLockedByOther(id, performedBy);
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.DOCUMENT_LOCKED) {
                        skippedIds.add(id);
                        skippedNames.add(doc.getName());
                        continue;
                    }
                    throw e;
                }
            }

            if (documentAccessValidator != null) {
                documentAccessValidator.validateAccess(doc, performedBy, "DELETE");
            }
            Document deleted = doc.toBuilder()
                    .deletedAt(now)
                    .deletedBy(performedBy)
                    .status(Document.DocumentStatus.TRASHED)
                    .updatedAt(now)
                    .build();
            documentRepositoryPort.save(deleted);

            if (eventPublisherPort != null) {
                eventPublisherPort.publish(new DocumentDeletedEvent(
                        deleted.getId(),
                        deleted.getName(),
                        deleted.getFolderId(),
                        deleted.getDeletedBy()
                ));
            }
            processedCount++;
        }

        return BulkActionResultDto.builder()
                .processedCount(processedCount)
                .skippedIds(skippedIds)
                .skippedNames(skippedNames)
                .build();
    }

    @Override
    public BulkActionResultDto bulkMove(List<UUID> documentIds, UUID targetFolderId, boolean moveToRoot, UUID performedBy) {
        UUID destination = moveToRoot ? null : targetFolderId;

        // Validate destination folder exists (unless moving to root)
        if (destination != null) {
            folderRepositoryPort.findById(destination)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.FOLDER_NOT_FOUND,
                            "Target folder with ID " + destination + " was not found."
                    ));
        }

        Instant now = Instant.now();
        int processedCount = 0;
        List<UUID> skippedIds = new ArrayList<>();
        List<String> skippedNames = new ArrayList<>();

        for (UUID id : documentIds) {
            Optional<Document> maybeDoc = documentRepositoryPort.findById(id);
            if (maybeDoc.isEmpty()) continue;
            Document doc = maybeDoc.get();

            if (documentLockGuard != null) {
                try {
                    documentLockGuard.assertNotLockedByOther(id, performedBy);
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.DOCUMENT_LOCKED) {
                        skippedIds.add(id);
                        skippedNames.add(doc.getName());
                        continue;
                    }
                    throw e;
                }
            }

            if (documentAccessValidator != null) {
                documentAccessValidator.validateAccess(doc, performedBy, "WRITE");
            }
            Document moved = doc.toBuilder()
                    .folderId(destination)
                    .updatedAt(now)
                    .build();
            documentRepositoryPort.save(moved);
            processedCount++;
        }

        return BulkActionResultDto.builder()
                .processedCount(processedCount)
                .skippedIds(skippedIds)
                .skippedNames(skippedNames)
                .build();
    }

    @Override
    public BulkActionResultDto bulkTag(List<UUID> documentIds, List<String> tagNames, UUID performedBy) {
        int processedCount = 0;
        List<UUID> skippedIds = new ArrayList<>();
        List<String> skippedNames = new ArrayList<>();

        for (UUID id : documentIds) {
            Optional<Document> maybeDoc = documentRepositoryPort.findById(id);
            if (maybeDoc.isEmpty()) continue;
            Document doc = maybeDoc.get();

            if (documentLockGuard != null) {
                try {
                    documentLockGuard.assertNotLockedByOther(id, performedBy);
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.DOCUMENT_LOCKED) {
                        skippedIds.add(id);
                        skippedNames.add(doc.getName());
                        continue;
                    }
                    throw e;
                }
            }

            if (documentAccessValidator != null) {
                documentAccessValidator.validateAccess(doc, performedBy, "WRITE");
            }
            for (String tagName : tagNames) {
                if (tagName != null && !tagName.isBlank()) {
                    documentRepositoryPort.addTagToDocument(id, tagName.trim());
                }
            }
            processedCount++;
        }

        return BulkActionResultDto.builder()
                .processedCount(processedCount)
                .skippedIds(skippedIds)
                .skippedNames(skippedNames)
                .build();
    }
}
