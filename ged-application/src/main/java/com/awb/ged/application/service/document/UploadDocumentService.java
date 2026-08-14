package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UploadDocumentCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.UploadDocumentUseCase;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
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
    private final DocumentAccessValidator documentAccessValidator;

    @Autowired
    public UploadDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 FolderRepositoryPort folderRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 StoragePort storagePort,
                                 DocumentMapper documentMapper,
                                 EventPublisherPort eventPublisherPort,
                                 DocumentAccessValidator documentAccessValidator) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderRepositoryPort = folderRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.storagePort = storagePort;
        this.documentMapper = documentMapper;
        this.eventPublisherPort = eventPublisherPort;
        this.documentAccessValidator = documentAccessValidator;
    }

    public UploadDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 FolderRepositoryPort folderRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 StoragePort storagePort,
                                 DocumentMapper documentMapper) {
        this(documentRepositoryPort, folderRepositoryPort, userRepositoryPort, storagePort, documentMapper, null, null);
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

        // 2b. Validate authorization for target category/department before uploading
        if (documentAccessValidator != null && command.getCategoryId() != null) {
            Document transientDoc = Document.builder()
                    .folderId(command.getFolderId())
                    .categoryId(command.getCategoryId())
                    .build();
            documentAccessValidator.validateAccess(transientDoc, command.getOwnerId(), "WRITE");
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

        // 6. Build DocumentVersion (v1) — avec le mimeType réel du fichier
        Instant now = Instant.now();
        String effectiveMimeType = (command.getMimeType() != null && !command.getMimeType().isBlank())
                ? command.getMimeType()
                : "application/octet-stream";

        DocumentVersion v1 = DocumentVersion.builder()
                .id(versionId)
                .documentId(documentId)
                .versionNumber(1)
                .hash(checksumHex)
                .sizeBytes(content.length)
                .mimeType(effectiveMimeType)
                .fileReferenceId(fileRef)
                .uploadedBy(command.getOwnerId())
                .uploadedAt(now)
                .build();

        // 7. Build Document Aggregate Root (initial save without activeVersionId to satisfy @NotNull on DocumentVersionJpaEntity)
        Document document = Document.builder()
                .id(documentId)
                .name(command.getName().trim())
                .folderId(command.getFolderId())
                .categoryId(command.getCategoryId())
                .ownerId(command.getOwnerId())
                .mimeType(effectiveMimeType)
                .activeVersionId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 8. 3-Step Persistence Flow for Circular FK (Document <-> DocumentVersion):
        // Step 1: Save Document record first (so DocumentJpaEntity exists in DB for foreign key constraint)
        documentRepositoryPort.save(document);

        // Step 2: Save DocumentVersion v1 (finds the saved DocumentJpaEntity, satisfying @NotNull validation)
        documentRepositoryPort.saveVersion(v1);

        // Step 3: Link activeVersionId on Document and save final state
        Document documentWithActiveVersion = document.toBuilder()
                .activeVersionId(versionId)
                .build();
        Document savedDoc = documentRepositoryPort.save(documentWithActiveVersion);

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
