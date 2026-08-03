package com.awb.ged.infrastructure.audit;

import com.awb.ged.application.port.out.audit.AuditLogPort;
import com.awb.ged.domain.document.event.DocumentUploadedEvent;
import com.awb.ged.domain.document.event.DocumentViewedEvent;
import com.awb.ged.domain.folder.event.FolderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditLogPort auditLogPort;

    private AuditEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new AuditEventListener(auditLogPort);
    }

    @Test
    @DisplayName("handleDocumentUploaded - Should invoke AuditLogPort.record with DOCUMENT_UPLOAD")
    void handleDocumentUploaded_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentUploadedEvent event = new DocumentUploadedEvent(
                docId, versionId, "hash123", 2048L, userId
        );

        // When
        eventListener.handleDocumentUploaded(event);

        // Then
        verify(auditLogPort).record(
                eq("DOCUMENT_UPLOAD"),
                eq("DOCUMENT"),
                eq(docId),
                eq("Document File Upload"),
                eq(userId),
                org.mockito.ArgumentMatchers.anyMap()
        );
    }

    @Test
    @DisplayName("handleDocumentViewed - Should invoke AuditLogPort.record with DOCUMENT_VIEW")
    void handleDocumentViewed_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentViewedEvent event = new DocumentViewedEvent(
                docId, "Contract.pdf", userId
        );

        // When
        eventListener.handleDocumentViewed(event);

        // Then
        verify(auditLogPort).record(
                eq("DOCUMENT_VIEW"),
                eq("DOCUMENT"),
                eq(docId),
                eq("Contract.pdf"),
                eq(userId),
                org.mockito.ArgumentMatchers.anyMap()
        );
    }

    @Test
    @DisplayName("handleFolderCreated - Should invoke AuditLogPort.record with FOLDER_CREATE")
    void handleFolderCreated_Success() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        FolderCreatedEvent event = new FolderCreatedEvent(
                folderId, "Archived Reports", null, userId
        );

        // When
        eventListener.handleFolderCreated(event);

        // Then
        verify(auditLogPort).record(
                eq("FOLDER_CREATE"),
                eq("FOLDER"),
                eq(folderId),
                eq("Archived Reports"),
                eq(userId),
                org.mockito.ArgumentMatchers.anyMap()
        );
    }
}
