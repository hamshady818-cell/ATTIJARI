package com.awb.ged.infrastructure.persistence.entity.user;

import com.awb.ged.infrastructure.persistence.entity.department.DepartmentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * <h1>UserJpaEntity</h1>
 * <p>
 * JPA entity representing a local cache of a Keycloak user profile within GED-AWB.
 * Keycloak remains the <strong>sole identity provider</strong>; this entity is kept
 * in sync via Keycloak events or a periodic sync job.
 * </p>
 *
 * <p>
 * It holds application-specific attributes (department, job title, locale, avatar)
 * that complement the Keycloak user profile, and anchors all FK relationships
 * (document ownership, audit logs, permissions, etc.) to a stable internal UUID.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:N with {@link RoleJpaEntity} — join table {@code user_roles} (owned here).</li>
 *   <li>M:N with {@link GroupJpaEntity} — join table {@code user_groups} (owned here).</li>
 * </ul>
 *
 * <p><strong>Design Decision — {@code @ManyToMany} ownership:</strong>
 * Both M:N relationships are owned by {@code UserJpaEntity} because the natural
 * query direction is always "which roles/groups does this user have?", never the reverse.
 * The inverse sides on Role and Group deliberately have no back-references to avoid
 * loading all users when fetching a role or group.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_keycloak_id", columnNames = "keycloak_id"),
                @UniqueConstraint(name = "uq_users_email",       columnNames = "email"),
                @UniqueConstraint(name = "uq_users_username",    columnNames = "username")
        },
        indexes = {
                @Index(name = "idx_users_keycloak_id", columnList = "keycloak_id"),
                @Index(name = "idx_users_email",       columnList = "email")
        }
)
public class UserJpaEntity extends BaseEntity {

    // ─────────────────────────────────────────────────────────────────────────
    //  Identity & Keycloak sync
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The Keycloak subject ({@code sub}) claim — the stable, authoritative
     * identifier used to correlate this record with the Keycloak user.
     * Never changes after initial provisioning.
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "keycloak_id", nullable = false, updatable = false, length = 255)
    private String keycloakId;

    /**
     * Unique login username (synced from Keycloak).
     */
    @NotBlank
    @Size(max = 150)
    @Column(name = "username", nullable = false, length = 150)
    private String username;

    /**
     * Primary email address (synced from Keycloak).
     */
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    // ─────────────────────────────────────────────────────────────────────────
    //  Profile
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Given name / first name (synced from Keycloak).
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Family name / last name (synced from Keycloak).
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Organizational department or business unit this user belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "department_id",
            foreignKey = @ForeignKey(name = "fk_users_department")
    )
    private DepartmentJpaEntity department;

    /**
     * Job title within the organization.
     * Application-specific — not managed by Keycloak.
     */
    @Size(max = 150)
    @Column(name = "job_title", length = 150)
    private String jobTitle;

    /**
     * URL pointing to the user's profile avatar image.
     */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Preferred UI locale (ISO 639-1 code, e.g., "fr", "ar", "en").
     * Defaults to French as the primary business language.
     */
    @Size(max = 10)
    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "fr";

    // ─────────────────────────────────────────────────────────────────────────
    //  Account status
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Whether the user's account is active in the application.
     * Mirrors the Keycloak "enabled" flag.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Whether the account is locked (e.g., after too many failed attempts).
     */
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean locked = false;

    /**
     * Timestamp of the last successful Keycloak profile synchronization.
     */
    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    // ─────────────────────────────────────────────────────────────────────────
    //  Relationships
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Application-level roles assigned to this user.
     *
     * <p>Fetch type is {@code LAZY} to prevent loading all roles on every user query.
     * The join table {@code user_roles} uses meaningful constraint names for
     * easier debugging in database logs.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id",  referencedColumnName = "id",
                                             foreignKey = @ForeignKey(name = "fk_user_roles_user")),
            inverseJoinColumns = @JoinColumn(name = "role_id",  referencedColumnName = "id",
                                             foreignKey = @ForeignKey(name = "fk_user_roles_role"))
    )
    @Builder.Default
    private Set<RoleJpaEntity> roles = new HashSet<>();

    /**
     * Organizational groups this user belongs to.
     *
     * <p>Fetch type is {@code LAZY}. The join table {@code user_groups} is owned here.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_groups",
            joinColumns        = @JoinColumn(name = "user_id",  referencedColumnName = "id",
                                             foreignKey = @ForeignKey(name = "fk_user_groups_user")),
            inverseJoinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id",
                                             foreignKey = @ForeignKey(name = "fk_user_groups_group"))
    )
    @Builder.Default
    private Set<GroupJpaEntity> groups = new HashSet<>();
}
