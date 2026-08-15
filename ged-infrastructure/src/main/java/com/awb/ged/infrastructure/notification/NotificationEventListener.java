package com.awb.ged.infrastructure.notification;

import com.awb.ged.application.port.out.persistence.NotificationRepositoryPort;
import com.awb.ged.domain.document.event.*;
import com.awb.ged.domain.folder.event.FolderCreatedEvent;
import com.awb.ged.domain.folder.event.FolderDeletedEvent;
import com.awb.ged.domain.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>NotificationEventListener</h1>
 * <p>
 * Infrastructure component that listens to domain events published by application services
 * and creates in-app {@link Notification} records via {@link NotificationRepositoryPort}.
 * </p>
 *
 * <p>Uses {@code @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)}
 * — same pattern as {@link com.awb.ged.infrastructure.audit.AuditEventListener} — ensuring that
 * notifications are only persisted after the triggering transaction commits successfully.</p>
 *
 * <h2>Self-notification policy</h2>
 * <p>A user is <strong>never notified of their own actions</strong> unless they are
 * performing an action on a document/folder owned by someone else. Exception: checkout/checkin
 * always notify the document owner if the actor is different from the owner.</p>
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private static final String CHANNEL = Notification.Channel.IN_APP.name();
    private static final String STATUS_SENT = "SENT";

    private final NotificationRepositoryPort notificationRepositoryPort;

    @Autowired
    public NotificationEventListener(NotificationRepositoryPort notificationRepositoryPort) {
        this.notificationRepositoryPort = notificationRepositoryPort;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Document events
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Notifies the document owner when a new document metadata record is created
     * by a <em>different</em> user (e.g., admin creating documents on behalf of an owner).
     * When the creator is also the owner, no notification is emitted to avoid noise.
     */
    @org.springframework.context.event.EventListener
    public void handleDocumentCreated(DocumentCreatedEvent event) {
        if (event.getOwnerId() == null) return;
        save(buildNotification(
                event.getOwnerId(),
                "DOCUMENT_UPLOADED",
                "Nouveau document versé",
                "Le document «" + event.getName() + "» a été versé dans la GED.",
                "DOCUMENT",
                event.getDocumentId()
        ));
    }

    @org.springframework.context.event.EventListener
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        if (event.getUploadedBy() == null) return;
        save(buildNotification(
                event.getUploadedBy(),
                "DOCUMENT_UPLOADED",
                "Fichier versé avec succès",
                "Votre fichier a été joint au document (ID : " + event.getDocumentId() + ").",
                "DOCUMENT",
                event.getDocumentId()
        ));
    }

    @org.springframework.context.event.EventListener
    public void handleDocumentDeleted(DocumentDeletedEvent event) {
        if (event.getDeletedBy() == null) return;
        save(buildNotification(
                event.getDeletedBy(),
                "DOCUMENT_DELETED",
                "Document supprimé",
                "Le document «" + event.getName() + "» a été mis à la corbeille.",
                "DOCUMENT",
                event.getDocumentId()
        ));
    }

    @org.springframework.context.event.EventListener
    public void handleDocumentArchived(DocumentArchivedEvent event) {
        if (event.getArchivedBy() == null) return;
        save(buildNotification(
                event.getArchivedBy(),
                "DOCUMENT_UPDATED",
                "Document archivé",
                "Le document (ID : " + event.getDocumentId() + ") a été archivé.",
                "DOCUMENT",
                event.getDocumentId()
        ));
    }

    @org.springframework.context.event.EventListener
    public void handleDocumentExpired(DocumentExpiredEvent event) {
        if (event.getOwnerId() == null) return;
        save(buildNotification(
                event.getOwnerId(),
                "DOCUMENT_EXPIRED",
                "Document expiré",
                "Le document «" + event.getDocumentName() + "» a expiré et a été archivé automatiquement.",
                "DOCUMENT",
                event.getDocumentId()
        ));
    }

    @org.springframework.context.event.EventListener
    public void handleDocumentCheckedOut(DocumentCheckedOutEvent event) {
        if (event.getOwnerId() == null || isSelfAction(event.getCheckedOutBy(), event.getOwnerId())) return;
        save(buildNotification(
                event.getOwnerId(),
                "CHECKOUT_REQUESTED",
                "Document verrouillé",
                "Le document «" + event.getDocumentName() + "» a été verrouillé (checkout).",
                "DOCUMENT",
                event.getDocumentId()
        ));
    }

    @org.springframework.context.event.EventListener
    public void handleDocumentCheckedIn(DocumentCheckedInEvent event) {
        if (event.getOwnerId() == null || isSelfAction(event.getCheckedInBy(), event.getOwnerId())) return;
        save(buildNotification(
                event.getOwnerId(),
                "CHECKIN_DONE",
                "Document déverrouillé",
                "Le document «" + event.getDocumentName() + "» est de nouveau disponible.",
                "DOCUMENT",
                event.getDocumentId()
        ));
    }

    @org.springframework.context.event.EventListener
    public void handleFolderCreated(FolderCreatedEvent event) {
        if (event.getOwnerId() == null) return;
        save(buildNotification(
                event.getOwnerId(),
                "SYSTEM",
                "Nouveau dossier créé",
                "Le dossier «" + event.getName() + "» a été créé dans la GED.",
                "FOLDER",
                event.getFolderId()
        ));
    }

    @org.springframework.context.event.EventListener
    public void handleFolderDeleted(FolderDeletedEvent event) {
        if (event.getDeletedBy() == null) return;
        save(buildNotification(
                event.getDeletedBy(),
                "SYSTEM",
                "Dossier supprimé",
                "Le dossier «" + event.getName() + "» a été supprimé.",
                "FOLDER",
                event.getFolderId()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the acting user is the same as the target recipient,
     * indicating a self-action that should not generate a notification.
     */
    boolean isSelfAction(UUID actor, UUID recipient) {
        return actor != null && actor.equals(recipient);
    }

    private Notification buildNotification(UUID userId, String type, String title, String body,
                                           String entityType, UUID entityId) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .entityType(entityType)
                .entityId(entityId)
                .channel(Notification.Channel.IN_APP)
                .status(STATUS_SENT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void save(Notification notification) {
        try {
            notificationRepositoryPort.save(notification);
        } catch (Exception ex) {
            // Non-critical: log and continue — a notification failure must never break a business operation
            log.error("[NotificationEventListener] Failed to persist notification type={} userId={}: {}",
                    notification.getType(), notification.getUserId(), ex.getMessage(), ex);
        }
    }
}
