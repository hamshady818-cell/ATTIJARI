package com.awb.ged.application.service.document;

import com.awb.ged.application.port.in.document.DeleteDocumentUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.event.DocumentDeletedEvent;
import com.awb.ged.domain.document.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class DeleteDocumentService implements DeleteDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    @Autowired
    public DeleteDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 EventPublisherPort eventPublisherPort) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public void deleteDocument(UUID documentId, UUID deletedByUserId) {
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        document.setDeletedAt(Instant.now());
        document.setDeletedBy(deletedByUserId);
        document.setStatus(Document.DocumentStatus.TRASHED);

        documentRepositoryPort.save(document);

        if (eventPublisherPort != null) {
            eventPublisherPort.publish(new DocumentDeletedEvent(
                    document.getId(),
                    document.getName(),
                    document.getFolderId(),
                    document.getDeletedBy()
            ));
        }
    }
}