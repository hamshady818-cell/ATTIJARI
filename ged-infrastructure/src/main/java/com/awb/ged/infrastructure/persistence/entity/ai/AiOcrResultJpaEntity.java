package com.awb.ged.infrastructure.persistence.entity.ai;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * <h1>AiOcrResultJpaEntity</h1>
 * <p>
 * JPA entity storing the result of an OCR (Optical Character Recognition) processing
 * run performed on a specific {@link DocumentVersionJpaEntity}.
 * </p>
 *
 * <p>
 * One OCR result per document version (1:1). The extracted text is stored in
 * {@code extractedText} and feeds both the full-text search index and the RAG pipeline.
 * Per-page data (bounding boxes, confidence per page) is stored in {@code pageData} as JSONB.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>1:1 ← {@link DocumentVersionJpaEntity} via {@code version}.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "ai_ocr_results",
        indexes = @Index(name = "idx_ocr_version_id", columnList = "version_id")
)
public class AiOcrResultJpaEntity extends BaseEntity {

    /**
     * OCR processing status lifecycle.
     */
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        DONE,
        FAILED
    }

    /**
     * The document version this OCR result belongs to.
     * One-to-one: one OCR result per version.
     */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "version_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_ocr_results_version")
    )
    private DocumentVersionJpaEntity version;

    /**
     * Full text extracted from the document by the OCR engine.
     * {@code null} if processing is not yet complete.
     */
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    /**
     * Total number of pages processed.
     */
    @Column(name = "page_count")
    private Integer pageCount;

    /**
     * Average confidence score across all pages (0.0 = worst, 1.0 = perfect).
     */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "confidence_score", precision = 5, scale = 4)
    private java.math.BigDecimal confidenceScore;

    /**
     * OCR engine used for this extraction (e.g., {@code TESSERACT}, {@code GOOGLE_VISION}).
     */
    @Column(name = "ocr_engine", nullable = false, length = 50)
    @Builder.Default
    private String ocrEngine = "TESSERACT";

    /**
     * ISO 639-1 language code detected by the OCR engine (e.g., {@code "fr"}, {@code "ar"}).
     */
    @Column(name = "language_detected", length = 10)
    private String languageDetected;

    /**
     * Per-page structured data: text blocks, bounding boxes, word-level confidence.
     * Stored as JSONB for flexible per-page granularity.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "page_data", columnDefinition = "jsonb")
    private Object pageData;

    /**
     * Current processing status of this OCR job.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    /**
     * How long the OCR processing took in milliseconds.
     */
    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    /**
     * Error details if {@code processingStatus = FAILED}.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp when OCR processing completed (success or failure).
     */
    @Column(name = "processed_at")
    private Instant processedAt;
}
