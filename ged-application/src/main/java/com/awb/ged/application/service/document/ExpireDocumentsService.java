package com.awb.ged.application.service.document;

import com.awb.ged.application.port.in.document.ExpireDocumentsUseCase;
import com.awb.ged.application.port.in.document.UpdateDocumentStatusUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.domain.document.event.DocumentExpiredEvent;
import com.awb.ged.domain.document.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * <h1>ExpireDocumentsService</h1>
 * <p>
 * Application service that archives every {@code PUBLISHED} or {@code DRAFT} document
 * whose {@code expirationDate} is strictly before today.
 * </p>
 *
 * <p><strong>Design decisions:</strong></p>
 * <ul>
 *   <li>Delegates the status transition to {@link UpdateDocumentStatusUseCase} to avoid
 *       duplicating the transition-validation logic (DRAFT/PUBLISHED → ARCHIVED).</li>
 *   <li>Publishes a {@link DocumentExpiredEvent} per document so that listeners
 *       (audit, notifications) can react independently.</li>
 *   <li>No {@code @Scheduled} annotation — scheduling is configured in {@code ged-boot}
 *       to keep this service free of infrastructure concerns.</li>
 * </ul>
 */
@Service
@Transactional
public class ExpireDocumentsService implements ExpireDocumentsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpireDocumentsService.class);

    private final DocumentRepositoryPort      documentRepositoryPort;
    private final UpdateDocumentStatusUseCase updateDocumentStatusUseCase;
    private final EventPublisherPort          eventPublisherPort;

    @Autowired
    public ExpireDocumentsService(DocumentRepositoryPort      documentRepositoryPort,
                                  UpdateDocumentStatusUseCase updateDocumentStatusUseCase,
                                  EventPublisherPort          eventPublisherPort) {
        this.documentRepositoryPort      = documentRepositoryPort;
        this.updateDocumentStatusUseCase = updateDocumentStatusUseCase;
        this.eventPublisherPort          = eventPublisherPort;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For each expired document:</p>
     * <ol>
     *   <li>Calls {@code updateDocumentStatusUseCase.updateStatus(id, "ARCHIVED", null)}
     *       — {@code currentUserId} is {@code null} because this is a system action
     *       (no human actor).</li>
     *   <li>Publishes a {@link DocumentExpiredEvent} via {@link EventPublisherPort}.</li>
     * </ol>
     */
    @Override
    public int expireOverdueDocuments() {
        LocalDate today = LocalDate.now();

        List<Document> expired = documentRepositoryPort.findExpiredActiveDocuments(today);

        for (Document document : expired) {
            // Delegate transition logic (validates DRAFT/PUBLISHED → ARCHIVED)
            // currentUserId = null → system-initiated action, no human actor
            updateDocumentStatusUseCase.updateStatus(document.getId(), "ARCHIVED", null);

            // Notify listeners: audit trail, notifications, etc.
            eventPublisherPort.publish(new DocumentExpiredEvent(
                    document.getId(),
                    document.getName(),
                    document.getOwnerId(),
                    document.getExpirationDate()
            ));
        }

        int count = expired.size();
        log.info("[ExpireDocumentsService] {} document(s) archived due to expiration (reference date: {}).",
                count, today);

        return count;
    }
}
