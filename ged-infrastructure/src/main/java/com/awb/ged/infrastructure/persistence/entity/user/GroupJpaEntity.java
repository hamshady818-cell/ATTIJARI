package com.awb.ged.infrastructure.persistence.entity.user;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>GroupJpaEntity</h1>
 * <p>
 * JPA entity representing an organizational group (department, team, or business unit)
 * within GED-AWB. Groups allow bulk permission assignments — granting access to a group
 * automatically applies to all its members.
 * </p>
 *
 * <p>
 * Groups support an <strong>unlimited self-referential hierarchy</strong>:
 * a group can have one parent and multiple children (e.g., "Bank" → "IT" → "Core Banking").
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>Self-referential M:1 → {@code parentGroup} (nullable — root groups have no parent).</li>
 *   <li>Self-referential 1:N → {@code children} (groups that have this group as parent).</li>
 *   <li>M:N with {@link UserJpaEntity} — inverse side, managed from {@code UserJpaEntity}.</li>
 * </ul>
 *
 * <p><strong>Design Decision — {@code orphanRemoval = false}:</strong>
 * Child groups are independent entities; deleting a parent group should not cascade-delete children.
 * Business logic in the application layer must handle re-parenting before deletion.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "groups",
        uniqueConstraints = @UniqueConstraint(name = "uq_groups_name", columnNames = "name"),
        indexes           = @Index(name = "idx_groups_parent_id", columnList = "parent_group_id")
)
public class GroupJpaEntity extends BaseEntity {

    /**
     * Unique display name of the group (e.g., "Finance Department", "IT Team").
     */
    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Optional description of the group's purpose or scope.
     */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    // ─────────────────────────────────────────────────────────────────────────
    //  Self-referential hierarchy
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parent group in the organizational hierarchy.
     * {@code null} for root-level groups.
     *
     * <p>LAZY fetch — parent is rarely needed when working with a specific group.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_group_id",
            foreignKey = @ForeignKey(name = "fk_groups_parent_group")
    )
    private GroupJpaEntity parentGroup;

    /**
     * Direct child groups under this group in the hierarchy.
     *
     * <p>LAZY fetch — children are only needed for tree-traversal operations.
     * {@code orphanRemoval = false}: child groups survive parent deletion;
     * the application layer must handle re-parenting.</p>
     */
    @OneToMany(
            mappedBy      = "parentGroup",
            fetch         = FetchType.LAZY,
            cascade       = CascadeType.PERSIST,
            orphanRemoval = false
    )
    @Builder.Default
    private List<GroupJpaEntity> children = new ArrayList<>();
}
