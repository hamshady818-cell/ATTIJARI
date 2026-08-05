package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UpdateDocumentCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.UpdateDocumentUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UpdateDocumentService implements UpdateDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final FolderRepositoryPort folderRepositoryPort;
    private final DocumentMapper documentMapper;

    @Autowired
    public UpdateDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 FolderRepositoryPort folderRepositoryPort,
                                 DocumentMapper documentMapper) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderRepositoryPort = folderRepositoryPort;
        this.documentMapper = documentMapper;
    }

    @Override
    public DocumentResponseDto updateDocument(UUID documentId, UpdateDocumentCommand command, UUID currentUserId) {
        // 1. Find document
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        // 2. Validate not locked
        if (document.isCurrentlyLocked(Instant.now())) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_LOCKED,
                    "Cannot rename or move a locked document. Check it in first."
            );
        }

        String targetName = command.getNewName() != null && !command.getNewName().isBlank()
                ? command.getNewName().trim()
                : document.getName();

        UUID targetFolderId = command.isMoveToRoot()
                ? null
                : (command.getNewFolderId() != null ? command.getNewFolderId() : document.getFolderId());

        // 3. If moving to another folder, verify folder exists
        if (targetFolderId != null && !targetFolderId.equals(document.getFolderId())) {
            folderRepositoryPort.findById(targetFolderId)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.FOLDER_NOT_FOUND,
                            "Target folder with ID " + targetFolderId + " was not found."
                    ));
        }

        // 4. Check name uniqueness in target folder (excluding current document)
        if (!targetName.equalsIgnoreCase(document.getName()) || !java.util.Objects.equals(targetFolderId, document.getFolderId())) {
            List<Document> siblings = documentRepositoryPort.findByFolderId(targetFolderId);
            boolean duplicate = siblings.stream()
                    .filter(d -> !d.getId().equals(documentId))
                    .anyMatch(d -> d.getName().equalsIgnoreCase(targetName));
            if (duplicate) {
                throw new ConflictException(
                        ErrorCode.DOCUMENT_ALREADY_EXISTS,
                        "A document named '" + targetName + "' already exists in the target folder."
                );
            }
        }

        // 5. Build updated document
        Document updated = document.toBuilder()
                .name(targetName)
                .description(command.getDescription() != null ? command.getDescription() : document.getDescription())
                .folderId(targetFolderId)
                .updatedAt(Instant.now())
                .build();

        Document saved = documentRepositoryPort.save(updated);
        return documentMapper.toResponseDto(saved);
    }
}
