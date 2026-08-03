package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UploadDocumentCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.UploadDocumentUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentVersion;
import com.awb.ged.domain.document.model.FileReferenceId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UploadDocumentService implements UploadDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final FolderRepositoryPort folderRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final StoragePort storagePort;
    private final DocumentMapper documentMapper;
    private final EventPublisherPort eventPublisherPort;

    @Autowired
    public UploadDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 FolderRepositoryPort folderRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 StoragePort storagePort,
                                 DocumentMapper documentMapper,
                                 EventPublisherPort eventPublisherPort) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderRepositoryPort = folderRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.storagePort = storagePort;
        this.documentMapper = documentMapper;
        this.eventPublisherPort = eventPublisherPort;
    }

    public UploadDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 FolderRepositoryPort folderRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 StoragePort storagePort,
                                 DocumentMapper documentMapper) {
        this(documentRepositoryPort, folderRepositoryPort, userRepositoryPort, storagePort, documentMapper, null);
    }

    @Override
    public DocumentResponseDto uploadDocument(UploadDocumentCommand command) {
        // 1. Verify folder existence if specified
        if (command.getFolderId() != null) {
            folderRepositoryPort.findById(command.getFolderId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.FOLDER_NOT_FOUND,
                            "Folder with ID " + command.getFolderId() + " was not found."
                    ));
        }

        // 2. Verify owner existence if specified
        if (command.getOwnerId() != null) {
            userRepositoryPort.findById(command.getOwnerId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.USER_NOT_FOUND,
                            "User with ID " + command.getOwnerId() + " was not found."
                    ));
        }

        // 3. Check name uniqueness in target folder
        List<Document> existingDocs = documentRepositoryPort.findByFolderId(command.getFolderId());
        boolean duplicateName = existingDocs.stream()
                .anyMatch(d -> d.getName().equalsIgnoreCase(command.getName().trim()));

        if (duplicateName) {
            throw new ConflictException(
                    ErrorCode.DOCUMENT_ALREADY_EXISTS,
                    "A document named '" + command.getName().trim() + "' already exists in this folder."
            );
        }

        // 4. Calculate SHA-256 hash of file content
        byte[] content = command.getFileContent() != null ? command.getFileContent() : new byte[0];
        String checksumHex = calculateSha256(content);

        // 5. Store binary in storage port
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        String storagePath = "documents/" + documentId + "/v1";
        FileReferenceId fileRef = storagePort.store(storagePath, content, command.getMimeType());

        // 6. Build DocumentVersion (v1)
        Instant now = Instant.now();
        DocumentVersion v1 = DocumentVersion.builder()
                .id(versionId)
                .documentId(documentId)
                .versionNumber(1)
                .hash(checksumHex)
                .sizeBytes(content.length)
                .fileReferenceId(fileRef)
                .uploadedBy(command.getOwnerId())
                .uploadedAt(now)
                .build();

        // 7. Build Document Aggregate Root
        Document document = Document.builder()
                .id(documentId)
                .name(command.getName().trim())
                .folderId(command.getFolderId())
                .categoryId(command.getCategoryId())
                .ownerId(command.getOwnerId())
                .activeVersionId(versionId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 8. Save document and version
        Document savedDoc = documentRepositoryPort.save(document);
        documentRepositoryPort.saveVersion(v1);

        // 9. Publish Domain Event
        if (eventPublisherPort != null) {
            eventPublisherPort.publish(new com.awb.ged.domain.document.event.DocumentUploadedEvent(
                    documentId,
                    versionId,
                    checksumHex,
                    content.length,
                    command.getOwnerId()
            ));
        }

        // 10. Map to DTO and return
        return documentMapper.toResponseDto(savedDoc);
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not supported", e);
        }
    }
}
