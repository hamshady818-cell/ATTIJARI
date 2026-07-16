package com.awb.ged.infrastructure.persistence.entity.user;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h1>RoleJpaEntity</h1>
 * <p>
 * JPA entity representing an application-level role within the GED-AWB system.
 * Roles are used for coarse-grained authorization alongside Keycloak realm roles.
 * Examples: {@code ADMIN}, {@code EDITOR}, {@code VIEWER}, {@code ARCHIVER}.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:N with {@link UserJpaEntity} — managed from the {@code UserJpaEntity} side
 *       via a {@code @ManyToMany} join table {@code user_roles}.</li>
 * </ul>
 *
 * <p><strong>Design Decision:</strong>
 * No {@code @ManyToMany} collection is mapped here (unidirectional ownership on the User side)
 * to avoid loading all users when fetching a role — a common N+1 trap.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uq_roles_name", columnNames = "name")
)
public class RoleJpaEntity extends BaseEntity {

    /**
     * Unique technical name of the role.
     * Stored in UPPER_SNAKE_CASE (e.g., {@code ADMIN}, {@code DOCUMENT_EDITOR}).
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Human-readable description of what this role grants.
     */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;
}
