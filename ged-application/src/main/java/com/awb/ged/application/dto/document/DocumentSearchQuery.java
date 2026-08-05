package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Query object for searching documents.
 * All fields are optional — unset fields are ignored in the query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchQuery {

    /** Free-text keyword matched against name and description */
    private String keyword;

    /** Filter by category ID */
    private UUID categoryId;

    /** Filter by tag name (partial match) */
    private String tagName;

    /** Filter by folder ID (null = root) */
    private UUID folderId;

    /** Filter by owner user ID */
    private UUID ownerId;

    /** Filter by document status (DRAFT, PUBLISHED, ARCHIVED, TRASHED) */
    private String status;

    /** Filter documents created on or after this timestamp */
    private Instant createdFrom;

    /** Filter documents created on or before this timestamp */
    private Instant createdTo;

    /** Filter documents modified on or after this timestamp */
    private Instant updatedFrom;

    /** Filter documents modified on or before this timestamp */
    private Instant updatedTo;

    /** Page number (0-based) */
    @Builder.Default
    private int page = 0;

    /** Page size */
    @Builder.Default
    private int size = 20;

    /** Sort field (e.g. "createdAt", "name", "updatedAt") */
    @Builder.Default
    private String sortBy = "createdAt";

    /** Sort direction: "ASC" or "DESC" */
    @Builder.Default
    private String sortDirection = "DESC";
}
