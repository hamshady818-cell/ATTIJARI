package com.awb.ged.infrastructure.persistence.entity.ai;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * <h1>AiEmbeddingJpaEntity</h1>
 * <p>
 * JPA entity acting as a <strong>metadata registry</strong> for vector embeddings
 * generated from the text chunks of a {@link DocumentVersionJpaEntity}.
 * </p>
 *
 * <p>
 * This table does <em>not</em> store the actual high-dimensional float vectors.
 * Those live in a purpose-built vector store (pgvector, Qdrant, or Weaviate).
 * This entity tracks:
 * <ul>
 *   <li>Which chunks exist and their text content.</li>
 *   <li>The model used to generate the embedding.</li>
 *   <li>The vector store ID for retrieval during RAG queries.</li>
 *   <li>Character offsets for UI highlighting / citation.</li>
 * </ul>
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link DocumentVersionJpaEntity} — the source version.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "ai_embeddings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_embedding_version_chunk",
                columnNames = {"version_id", "chunk_index"}
        ),
        indexes = @Index(name = "idx_embeddings_version_id", columnList = "version_id")
)
public class AiEmbeddingJpaEntity extends BaseEntity {

    /**
     * The document version this embedding chunk was extracted from.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "version_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_embeddings_version")
    )
    private DocumentVersionJpaEntity version;

    /**
     * Sequential index of this chunk within the document version (0-based).
     * Combined with {@code versionId} to uniquely identify a chunk.
     */
    @Min(0)
    @Column(name = "chunk_index", nullable = false, updatable = false)
    private int chunkIndex;

    /**
     * The text content of this chunk — the actual text that was embedded.
     * Stored here for retrieval augmentation without re-fetching from the vector store.
     */
    @NotBlank
    @Column(name = "chunk_text", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String chunkText;

    /**
     * Character offset in the full extracted text where this chunk starts.
     * Used for UI text highlighting when citing this chunk.
     */
    @Column(name = "chunk_start_char", updatable = false)
    private Integer chunkStartChar;

    /**
     * Character offset in the full extracted text where this chunk ends.
     */
    @Column(name = "chunk_end_char", updatable = false)
    private Integer chunkEndChar;

    /**
     * Name of the embedding model used.
     * Example: {@code "text-embedding-3-small"}, {@code "nomic-embed-text"}.
     */
    @NotBlank
    @Column(name = "embedding_model", nullable = false, updatable = false, length = 100)
    private String embeddingModel;

    /**
     * The ID of this chunk's vector in the external vector store.
     * Used to retrieve the actual embedding for similarity search.
     */
    @Column(name = "vector_store_id", updatable = false)
    private String vectorStoreId;

    /**
     * The collection or index name within the vector store.
     * Example: {@code "ged-documents"}, {@code "ged-en"}.
     */
    @Column(name = "vector_collection", length = 100, updatable = false)
    private String vectorCollection;

    /**
     * Number of tokens in this chunk (for cost tracking and context management).
     */
    @Column(name = "token_count", updatable = false)
    private Integer tokenCount;
}
