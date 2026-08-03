package com.awb.ged.infrastructure.persistence.entity.category;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>CategoryJpaEntity</h1>
 * <p>
 * JPA entity representing a node in the document classification taxonomy.
 * Categories form a hierarchical tree (e.g., Finance → Invoices → 2024),
 * allowing fine-grained classification of documents.
 * </p>
 *
 * <p>
 * Each category can optionally define custom metadata field schemas via
 * {@link com.awb.ged.infrastructure.persistence.entity.metadata.MetadataDefinitionJpaEntity}.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>Self-referential M:1 → {@code parentCategory} (nullable for root categories).</li>
 *   <li>Self-referential 1:N → {@code children}.</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code createdBy}.</li>
 *   <li>1:N → {@code MetadataDefinitionJpaEntity} (category-scoped field definitions) — mapped on that side.</li>
 * </ul>
 *
 * <p><strong>Design Decision — path column:</strong>
 * The {@code path} column (PostgreSQL {@code ltree} type, stored as text) stores the
 * materialized path for efficient subtree queries. It is maintained by a database trigger
 * or application service — not by JPA cascade — to avoid complexity.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_parent_id", columnList = "parent_category_id"),
                @Index(name = "idx_categories_path",      columnList = "path")
        }
)
public class CategoryJpaEntity extends BaseEntity {

    /**
     * Display name of the category (e.g., "Invoices", "HR Contracts").
     */
    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Optional description of the category's purpose.
     */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Materialized path for efficient ancestor/descendant queries.
     * Stored in ltree dot-notation (e.g., {@code "1.4.12"}).
     * Maintained by application logic on create/move operations.
     */
    @Column(name = "path", nullable = false)
    private String path;

    /**
     * Hex color code for UI display (e.g., {@code "#3B82F6"}).
     */
    @Size(max = 7)
    @Column(name = "color", length = 7)
    private String color;

    /**
     * Icon identifier string for UI rendering (e.g., {@code "folder-finance"}).
     */
    @Size(max = 50)
    @Column(name = "icon", length = 50)
    private String icon;

    // ─────────────────────────────────────────────────────────────────────────
    //  Self-referential hierarchy
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parent category. {@code null} for root-level categories.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_category_id",
            foreignKey = @ForeignKey(name = "fk_categories_parent_category")
    )
    private CategoryJpaEntity parentCategory;

    /**
     * Direct child categories under this category.
     */
    @OneToMany(
            mappedBy      = "parentCategory",
            fetch         = FetchType.LAZY,
            cascade       = CascadeType.PERSIST,
            orphanRemoval = false
    )
    @Builder.Default
    private List<CategoryJpaEntity> children = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Audit
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * User who created this category.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_categories_created_by")
    )
    private UserJpaEntity createdBy;
}
