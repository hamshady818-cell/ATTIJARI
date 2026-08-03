package com.awb.ged.infrastructure.persistence.entity.department;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>DepartmentJpaEntity</h1>
 * <p>
 * JPA entity representing an organizational department or business unit within GED-AWB.
 * Supports hierarchical nesting via a self-referential parent-child relationship.
 * Maps to the {@code departments} database table defined in V1 init schema.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "departments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_departments_name",
                columnNames = "name"
        ),
        indexes = @Index(name = "idx_departments_parent", columnList = "parent_id")
)
public class DepartmentJpaEntity extends BaseEntity {

    /**
     * Display name of the department (e.g., "Direction Financière", "RH").
     */
    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Parent department. {@code null} for top-level departments.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_id",
            foreignKey = @ForeignKey(name = "fk_departments_parent")
    )
    private DepartmentJpaEntity parentDepartment;

    /**
     * Direct child sub-departments under this department.
     */
    @OneToMany(
            mappedBy      = "parentDepartment",
            fetch         = FetchType.LAZY,
            cascade       = CascadeType.PERSIST,
            orphanRemoval = false
    )
    @Builder.Default
    private List<DepartmentJpaEntity> children = new ArrayList<>();
}
