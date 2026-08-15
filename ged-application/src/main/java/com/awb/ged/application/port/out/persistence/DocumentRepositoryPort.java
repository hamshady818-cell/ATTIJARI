package com.awb.ged.application.port.out.persistence;

import com.awb.ged.application.dto.document.DocumentSearchQuery;
import com.awb.ged.application.dto.document.DocumentSearchResultDto;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h1>DocumentRepositoryPort</h1>
 * <p>
 * Output Port interface representing persistence capabilities for the {@link Document} Aggregate Root
 * and its associated {@link DocumentVersion} entities.
 * Implemented by persistence adapters in the infrastructure layer.
 * </p>
 */
public interface DocumentRepositoryPort {

    // --- Document CRUD Operations ---

    Document save(Document document);

    Optional<Document> findById(UUID id);

    Optional<Document> findByIdIncludingDeleted(UUID id);

    List<Document> findByFolderId(UUID folderId);

    List<Document> findAll();

    void delete(UUID id);

    // --- Search ---

    PageResponse<DocumentSearchResultDto> search(DocumentSearchQuery query);

    /**
     * Returns all active documents whose {@code expirationDate} is strictly before {@code today}.
     * Only documents with status {@code PUBLISHED} or {@code DRAFT} are considered;
     * documents already {@code ARCHIVED} or {@code TRASHED} are excluded.
     *
     * @param today the reference date (typically {@link java.time.LocalDate#now()})
     * @return a non-null, possibly empty list of expired active documents
     */
    List<Document> findExpiredActiveDocuments(java.time.LocalDate today);

    // --- Document Version Operations ---

    DocumentVersion saveVersion(DocumentVersion version);

    Optional<DocumentVersion> findVersionById(UUID versionId);

    List<DocumentVersion> findVersionsByDocumentId(UUID documentId);

    int countVersionsByDocumentId(UUID documentId);

    // --- Checkout / Lock Operations ---

    void saveCheckout(UUID documentId, UUID userId);

    void checkin(UUID documentId, UUID userId);

    Optional<CheckoutInfo> findActiveCheckout(UUID documentId);

    // --- Stats for Dashboard ---

    long countAllDocuments();

    long sumAllVersionSizeBytes();

    List<Document> findRecentUploads(int limit);

    List<Document> findRecentlyModified(int limit);

    // --- Bulk Tag ---

    void addTagToDocument(UUID documentId, String tagName);

    /**
     * Lightweight struct for checkout info returned to services.
     */
    record CheckoutInfo(UUID checkedOutBy, java.time.Instant checkedOutAt, java.time.Instant expectedReturnAt) {}
}
