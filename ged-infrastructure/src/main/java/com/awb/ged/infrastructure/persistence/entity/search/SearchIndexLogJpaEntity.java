package com.awb.ged.infrastructure.persistence.entity.search;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * <h1>SearchIndexLogJpaEntity</h1>
 * <p>
 * JPA entity tracking the full-text search indexing state of each
 * {@link DocumentVersionJpaEntity} in GED-AWB.
 * </p>
 *
 * <p>
 * This table acts as a <strong>reliable async work queue</strong>:
 * <ol>
 *   <li>When a new version is created or OCR completes, a {@code PENDING} row is inserted.</li>
 *   <li>The indexing worker (scheduled job or event listener) picks up {@code PENDING} rows.</li>
 *   <li>On success, the row is updated to {@code INDEXED} with a timestamp.</li>
 *   <li>On failure, {@code retryCount} is incremented and status remains {@code PENDING}
 *       (or set to {@code FAILED} after max retries).</li>
 * </ol>
 * </p>
 *
 * <p>
 * This pattern decouples document ingestion from search indexing, enabling
 * independent scaling and retry logic for each.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>1:1 ← {@link DocumentVersionJpaEntity} via {@code version}.</li>
 * </ul>
 *
 * <p><strong>Design Decision — {@code indexEngine} field:</strong>
 * Storing the engine name ({@code ELASTICSEARCH}, {@code OPENSEARCH}, {@code POSTGRES_FTS})
 * allows the system to support multiple search backends simultaneously or migrate
 * between them without losing indexing state.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "search_index_log",
        indexes = {
                @Index(name = "idx_search_log_status",     columnList = "index_status"),
                @Index(name = "idx_search_log_version_id", columnList = "version_id")
        }
)
public class SearchIndexLogJpaEntity extends BaseEntity {

    /**
     * Indexing lifecycle status for this version.
     */
    public enum IndexStatus {
        /** Queued — waiting to be processed by the indexing worker */
        PENDING,
        /** Successfully indexed — the version is searchable */
        INDEXED,
        /** Indexing failed — see {@code errorMessage} and {@code retryCount} */
        FAILED
    }

    /**
     * The document version whose content needs to be (or has been) indexed.
     * One-to-one: one log entry per version.
     */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "version_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_search_log_version")
    )
    private DocumentVersionJpaEntity version;

    /**
     * Current indexing status for this version.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "index_status", nullable = false, length = 30)
    @Builder.Default
    private IndexStatus indexStatus = IndexStatus.PENDING;

    /**
     * The search engine backend used for indexing.
     * Examples: {@code "ELASTICSEARCH"}, {@code "OPENSEARCH"}, {@code "POSTGRES_FTS"}.
     * {@code null} until the first indexing attempt.
     */
    @Column(name = "index_engine", length = 50)
    private String indexEngine;

    /**
     * Timestamp when indexing completed successfully.
     * {@code null} until status becomes {@code INDEXED}.
     */
    @Column(name = "indexed_at")
    private Instant indexedAt;

    /**
     * Number of failed indexing attempts for this version.
     * Used by the retry policy to back off and eventually mark as {@code FAILED}.
     */
    @Min(0)
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /**
     * Error message from the most recent failed indexing attempt.
     * Overwritten on each retry — only the last failure reason is stored.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
