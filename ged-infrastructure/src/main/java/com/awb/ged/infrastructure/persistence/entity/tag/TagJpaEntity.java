package com.awb.ged.infrastructure.persistence.entity.tag;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h1>TagJpaEntity</h1>
 * <p>
 * JPA entity representing a normalized document tag within the GED-AWB system.
 * Tags are global (not scoped to a category or folder) and shared across all documents.
 * </p>
 *
 * <p>
 * Tag names are stored in <strong>lowercase-slug</strong> format (e.g., {@code "contract-2024"},
 * {@code "urgent"}) to ensure consistent deduplication regardless of the user's input casing.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:N with {@code DocumentJpaEntity} — inverse side; the join table
 *       {@code document_tags} is owned and managed by {@code DocumentJpaEntity}.</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code createdBy}.</li>
 * </ul>
 *
 * <p><strong>Design Decision — Normalization:</strong>
 * Tags are normalized into their own table to avoid string duplication.
 * A single canonical tag row is shared by any number of documents,
 * enabling efficient tag-based search and rename operations.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "tags",
        uniqueConstraints = @UniqueConstraint(name = "uq_tags_name", columnNames = "name")
)
public class TagJpaEntity extends BaseEntity {

    /**
     * Unique tag label stored in lowercase-slug format.
     * Example valid values: {@code "invoice"}, {@code "contract-2024"}, {@code "urgent"}.
     */
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
             message = "Tag name must be a lowercase slug (e.g., 'my-tag')")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Optional hex color code for UI badge rendering (e.g., {@code "#F59E0B"}).
     */
    @Size(max = 7)
    @Column(name = "color", length = 7)
    private String color;

    /**
     * User who created this tag.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_tags_created_by")
    )
    private UserJpaEntity createdBy;
}
