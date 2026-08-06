package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentVersionResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.UploadNewVersionUseCase;
import com.awb.ged.application.dto.document.UploadNewVersionCommand;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.event.DocumentUploadedEvent;
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
public class UploadNewVersionService implements UploadNewVersionUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final StoragePort storagePort;
    private final DocumentMapper documentMapper;
    private final EventPublisherPort eventPublisherPort;

    @Autowired
    public UploadNewVersionService(DocumentRepositoryPort documentRepositoryPort,
                                   StoragePort storagePort,
                                   DocumentMapper documentMapper,
                                   EventPublisherPort eventPublisherPort) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.storagePort = storagePort;
        this.documentMapper = documentMapper;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public DocumentVersionResponseDto uploadNewVersion(UploadNewVersionCommand command) {
        // 1. Verify document exists
        Document document = documentRepositoryPort.findById(command.getDocumentId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + command.getDocumentId() + " was not found."
                ));

        // 2. Verify document is not locked by another user
        if (document.isCurrentlyLocked(Instant.now())) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_LOCKED,
                    "Document is currently checked out — upload a new version after it is checked in."
            );
        }

        // 3. Determine next version number
        int nextVersion = documentRepositoryPort.countVersionsByDocumentId(command.getDocumentId()) + 1;

        // 4. Compute SHA-256 checksum
        byte[] content = command.getFileContent() != null ? command.getFileContent() : new byte[0];
        String checksumHex = calculateSha256(content);

        // 5. Store file in storage
        UUID versionId = UUID.randomUUID();
        String storagePath = "documents/" + command.getDocumentId() + "/v" + nextVersion;
        FileReferenceId fileRef = storagePort.store(storagePath, content, command.getMimeType());

        // 6. Build new DocumentVersion — avec le mimeType réel du fichier
        String effectiveMimeType = (command.getMimeType() != null && !command.getMimeType().isBlank())
                ? command.getMimeType()
                : "application/octet-stream";

        Instant now = Instant.now();
        DocumentVersion newVersion = DocumentVersion.builder()
                .id(versionId)
                .documentId(command.getDocumentId())
                .versionNumber(nextVersion)
                .hash(checksumHex)
                .sizeBytes(content.length)
                .mimeType(effectiveMimeType)
                .fileReferenceId(fileRef)
                .uploadedBy(command.getUploadedBy())
                .uploadedAt(now)
                .build();

        // 7. Save the new version
        DocumentVersion saved = documentRepositoryPort.saveVersion(newVersion);

        // 8. Update document's active version pointer and updatedAt
        Document updated = document.toBuilder()
                .activeVersionId(versionId)
                .mimeType(command.getMimeType())
                .updatedAt(now)
                .build();
        documentRepositoryPort.save(updated);

        // 9. Publish event
        if (eventPublisherPort != null) {
            eventPublisherPort.publish(new DocumentUploadedEvent(
                    command.getDocumentId(),
                    versionId,
                    checksumHex,
                    content.length,
                    command.getUploadedBy()
            ));
        }

        return documentMapper.toVersionResponseDto(saved);
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
