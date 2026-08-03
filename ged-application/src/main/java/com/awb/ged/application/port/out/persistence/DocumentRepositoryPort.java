package com.awb.ged.application.port.out.persistence;

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

    // --- Document Operations ---

    /**
     * Persists or updates a document aggregate root.
     *
     * @param document the document to save
     * @return the saved document
     */
    Document save(Document document);

    /**
     * Resolves a document by its ID.
     *
     * @param id the document UUID
     * @return an {@link Optional} containing the document, or empty
     */
    Optional<Document> findById(UUID id);

    Optional<Document> findByIdIncludingDeleted(UUID id);

    /**
     * Finds all documents in a specific folder.
     *
     * @param folderId the folder UUID, or null for root-level documents
     * @return list of documents
     */
    List<Document> findByFolderId(UUID folderId);

    /**
     * Lists all documents.
     *
     * @return list of all documents
     */
    List<Document> findAll();

    /**
     * Deletes a document by ID.
     *
     * @param id the document UUID
     */
    void delete(UUID id);

    // --- Document Version Operations ---

    /**
     * Persists a new version for a document.
     *
     * @param version the version details to save
     * @return the saved document version
     */
    DocumentVersion saveVersion(DocumentVersion version);

    /**
     * Resolves a document version by its ID.
     *
     * @param versionId the version UUID
     * @return an {@link Optional} containing the document version, or empty
     */
    Optional<DocumentVersion> findVersionById(UUID versionId);

    /**
     * Lists all versions associated with a document, ordered chronologically.
     *
     * @param documentId the document UUID
     * @return list of document versions
     */
    List<DocumentVersion> findVersionsByDocumentId(UUID documentId);
}
