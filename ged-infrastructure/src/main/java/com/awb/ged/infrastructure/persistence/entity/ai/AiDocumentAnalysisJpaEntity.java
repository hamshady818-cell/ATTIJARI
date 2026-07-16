package com.awb.ged.infrastructure.persistence.entity.ai;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * <h1>AiDocumentAnalysisJpaEntity</h1>
 * <p>
 * JPA entity storing AI-generated analysis results for a {@link DocumentVersionJpaEntity}.
 * </p>
 *
 * <p>
 * Each analysis run produces:
 * <ul>
 *   <li>An auto-generated summary of the document's content.</li>
 *   <li>Suggested categories with confidence scores (JSONB array).</li>
 *   <li>Named entities extracted from the text (persons, organizations, dates, amounts).</li>
 *   <li>Key phrases and topic signals.</li>
 *   <li>A risk score and risk flag list for compliance screening.</li>
 *   <li>Tag suggestions for auto-tagging.</li>
 * </ul>
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
        name = "ai_document_analysis",
        indexes = @Index(name = "idx_ai_analysis_version_id", columnList = "version_id")
)
public class AiDocumentAnalysisJpaEntity extends BaseEntity {

    /**
     * Sentiment classification result.
     */
    public enum Sentiment {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }

    /**
     * Analysis processing status.
     */
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        DONE,
        FAILED
    }

    /**
     * The document version this analysis belongs to.
     */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "version_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_ai_analysis_version")
    )
    private DocumentVersionJpaEntity version;

    /**
     * Name of the AI model used for this analysis.
     * Example: {@code "gpt-4o"}, {@code "claude-3-sonnet"}.
     */
    @NotBlank
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /**
     * Specific version string of the AI model.
     */
    @Column(name = "model_version", length = 50)
    private String modelVersion;

    /**
     * AI-generated summary of the document's content.
     */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /**
     * Array of suggested category IDs with confidence scores.
     * Example: {@code [{"categoryId": "uuid", "confidence": 0.92}, ...]}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_categories", columnDefinition = "jsonb")
    private Object suggestedCategories;

    /**
     * Named entities extracted from the document.
     * Example: {@code [{"type": "PERSON", "value": "John Doe"}, {"type": "DATE", "value": "2024-07-15"}]}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_entities", columnDefinition = "jsonb")
    private Object extractedEntities;

    /**
     * Key phrases identified as significant topics or themes in the document.
     * Example: {@code ["contract renewal", "payment terms", "force majeure"]}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_phrases", columnDefinition = "jsonb")
    private Object keyPhrases;

    /**
     * Overall sentiment of the document's content.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", length = 20)
    private Sentiment sentiment;

    /**
     * Composite risk indicator score (0.0 = no risk, 1.0 = maximum risk).
     * Used for compliance screening and escalation workflows.
     */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "risk_score", precision = 5, scale = 4)
    private BigDecimal riskScore;

    /**
     * List of specific risk flags detected in the document.
     * Example: {@code ["PII_DETECTED", "FINANCIAL_DATA", "GDPR_SENSITIVE"]}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_flags", columnDefinition = "jsonb")
    private Object riskFlags;

    /**
     * AI-suggested tags to apply to this document.
     * Example: {@code ["invoice", "supplier-xyz", "q4-2024"]}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_tags", columnDefinition = "jsonb")
    private Object suggestedTags;

    /**
     * Current analysis processing status.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    /**
     * Timestamp when the analysis completed.
     */
    @Column(name = "processed_at")
    private Instant processedAt;
}
