package com.awb.ged.infrastructure.audit;

import com.awb.ged.application.port.out.audit.AuditLogPort;
import com.awb.ged.domain.document.event.DocumentArchivedEvent;
import com.awb.ged.domain.document.event.DocumentCreatedEvent;
import com.awb.ged.domain.document.event.DocumentUploadedEvent;
import com.awb.ged.domain.document.event.DocumentViewedEvent;
import com.awb.ged.domain.folder.event.FolderCreatedEvent;
import com.awb.ged.domain.folder.event.FolderViewedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class AuditEventListener {

    private final AuditLogPort auditLogPort;

    public AuditEventListener(AuditLogPort auditLogPort) {
        this.auditLogPort = auditLogPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        Map<String, Object> metadata = Map.of(
                "versionId", event.getVersionId() != null ? event.getVersionId().toString() : "",
                "hash", event.getHash() != null ? event.getHash() : "",
                "sizeBytes", event.getSizeBytes()
        );

        auditLogPort.record(
                "DOCUMENT_UPLOAD",
                "DOCUMENT",
                event.getDocumentId(),
                "Document File Upload",
                event.getUploadedBy(),
                metadata
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDocumentCreated(DocumentCreatedEvent event) {
        auditLogPort.record(
                "DOCUMENT_CREATE",
                "DOCUMENT",
                event.getDocumentId(),
                event.getName(),
                event.getOwnerId(),
                Map.of("folderId", event.getFolderId() != null ? event.getFolderId().toString() : "ROOT")
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDocumentViewed(DocumentViewedEvent event) {
        auditLogPort.record(
                "DOCUMENT_VIEW",
                "DOCUMENT",
                event.getDocumentId(),
                event.getDocumentName(),
                event.getViewedBy(),
                Map.of()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleFolderCreated(FolderCreatedEvent event) {
        auditLogPort.record(
                "FOLDER_CREATE",
                "FOLDER",
                event.getFolderId(),
                event.getName(),
                event.getOwnerId(),
                Map.of("parentId", event.getParentId() != null ? event.getParentId().toString() : "ROOT")
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleFolderViewed(FolderViewedEvent event) {
        auditLogPort.record(
                "FOLDER_VIEW",
                "FOLDER",
                event.getFolderId(),
                event.getFolderName(),
                event.getViewedBy(),
                Map.of()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDocumentArchived(DocumentArchivedEvent event) {
        auditLogPort.record(
                "DOCUMENT_ARCHIVE",
                "DOCUMENT",
                event.getDocumentId(),
                "Archived Document",
                event.getArchivedBy(),
                Map.of()
        );
    }
}
