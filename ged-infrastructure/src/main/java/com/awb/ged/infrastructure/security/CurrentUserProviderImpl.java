package com.awb.ged.infrastructure.security;

import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.DepartmentRepositoryPort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.security.AppPermission;
import com.awb.ged.common.security.AppRole;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.department.model.Department;
import com.awb.ged.domain.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * <h1>CurrentUserProviderImpl</h1>
 * <p>
 * Resolves the current authenticated user from the Spring Security context (JWT token issued by Keycloak)
 * and maps each JWT claim dimension to the corresponding {@link CurrentUser} field.
 * </p>
 *
 * <h2>JWT claim → CurrentUser mapping</h2>
 * <ul>
 *   <li><b>realm_access.roles</b> (SUPER_ADMIN, ADMIN, USER, MANAGER, VIEWER) → {@link CurrentUser#getRoles()} as {@link AppRole}</li>
 *   <li><b>realm_access.roles</b> (DOCUMENT_READ, DOCUMENT_WRITE, DOCUMENT_DELETE, DOCUMENT_DOWNLOAD, DOCUMENT_ADMIN)
 *       → {@link CurrentUser#getPermissions()} as {@link AppPermission} (see mapping table below)</li>
 *   <li><b>resource_access.ged-boot.roles</b> prefixed with {@code DOC_TYPE_}
 *       → {@link CurrentUser#getDocumentTypeAccess()} as uppercase strings without the prefix (e.g., {@code "FINANCE"})</li>
 *   <li><b>groups</b> (e.g., {@code "/FINANCE"}) → {@link CurrentUser#getDepartmentId()} resolved by:
 *     <ol>
 *       <li>Looking up the user's {@code departmentId} from the local {@code users} table via {@code keycloakSub}</li>
 *       <li>Fallback: matching the group name (without "/") case-insensitively against {@code departments.name}</li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * <h2>Keycloak realm role → AppPermission mapping</h2>
 * <pre>
 * DOCUMENT_READ     → AppPermission.DOCUMENT_READ
 * DOCUMENT_WRITE    → AppPermission.DOCUMENT_CREATE, AppPermission.DOCUMENT_UPDATE
 * DOCUMENT_DELETE   → AppPermission.DOCUMENT_DELETE
 * DOCUMENT_DOWNLOAD → AppPermission.DOCUMENT_READ  (download ⊆ read in the current permission model)
 * DOCUMENT_ADMIN    → DOCUMENT_READ, DOCUMENT_CREATE, DOCUMENT_UPDATE, DOCUMENT_DELETE,
 *                     FOLDER_READ, FOLDER_CREATE, FOLDER_UPDATE, FOLDER_DELETE
 * </pre>
 *
 * <p>
 * Note: If {@code DOCUMENT_DOWNLOAD} needs to be distinguished from {@code DOCUMENT_READ} in the future,
 * add {@code AppPermission.DOCUMENT_DOWNLOAD} to the enum and update this mapping.
 * </p>
 */
@Component
public class CurrentUserProviderImpl implements CurrentUserProvider {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserProviderImpl.class);

    private static final String KEYCLOAK_CLIENT_ID = "ged-boot";
    private static final String DOC_TYPE_PREFIX    = "DOC_TYPE_";

    /** Fallback DEV user used when no JWT principal is present (local dev without Keycloak). */
    private static final CurrentUser DEV_USER = CurrentUser.builder()
            .keycloakSub("00000000-0000-0000-0000-000000000001")
            .username("admin")
            .email("admin@attijariwafa.com")
            .roles(Set.of(AppRole.SUPER_ADMIN, AppRole.ADMIN, AppRole.MANAGER, AppRole.USER))
            .permissions(EnumSet.allOf(AppPermission.class))
            .documentTypeAccess(Set.of("DIRECTION", "FINANCE", "IT", "JURIDIQUE", "RH"))
            .build();

    private final UserRepositoryPort userRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;

    public CurrentUserProviderImpl(UserRepositoryPort userRepositoryPort,
                                   DepartmentRepositoryPort departmentRepositoryPort) {
        this.userRepositoryPort      = userRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
    }

    @Override
    public Optional<CurrentUser> getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            // No active security context — use dev fallback (non-production only)
            return Optional.of(DEV_USER);
        }

        if (auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(buildFromJwt(jwt));
        }

        // Authenticated but not via JWT (e.g., anonymous or test stub) — use dev fallback
        return Optional.of(DEV_USER);
    }

    @Override
    public CurrentUser getRequiredCurrentUser() {
        return getCurrentUser().orElse(DEV_USER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core JWT → CurrentUser builder
    // ─────────────────────────────────────────────────────────────────────────

    private CurrentUser buildFromJwt(Jwt jwt) {
        String sub      = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email    = jwt.getClaimAsString("email");

        // 1. Map realm_access.roles → AppRole + explicit AppPermissions
        Set<AppRole>       roles       = new HashSet<>();
        Set<AppPermission> permissions = EnumSet.noneOf(AppPermission.class);
        mapRealmRoles(jwt, roles, permissions);

        // 2. Map resource_access.ged-boot.roles (DOC_TYPE_*) → documentTypeAccess
        Set<String> documentTypeAccess = mapDocumentTypeRoles(jwt);

        // 3. Resolve departmentId from Keycloak groups claim
        UUID departmentId = resolveDepartmentId(jwt, sub);

        return CurrentUser.builder()
                .keycloakSub(sub)
                .username(username != null ? username : "unknown")
                .email(email != null ? email : "")
                .roles(roles)
                .permissions(permissions)
                .documentTypeAccess(documentTypeAccess)
                .departmentId(departmentId)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  1. realm_access.roles → AppRole + AppPermission
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads {@code realm_access.roles} from the JWT and maps each role to either an {@link AppRole}
     * (when the role represents a user grade) or one or more {@link AppPermission}s
     * (when the role represents a CRUD capability).
     *
     * <p>Keycloak realm roles intended as user grades: SUPER_ADMIN, ADMIN, MANAGER, USER, VIEWER.</p>
     * <p>Keycloak realm roles intended as CRUD capabilities: DOCUMENT_READ, DOCUMENT_WRITE,
     * DOCUMENT_DELETE, DOCUMENT_DOWNLOAD, DOCUMENT_ADMIN.</p>
     */
    @SuppressWarnings("unchecked")
    private void mapRealmRoles(Jwt jwt, Set<AppRole> roles, Set<AppPermission> permissions) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return;

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof Collection)) return;

        for (Object roleObj : (Collection<?>) rolesObj) {
            String roleName = roleObj.toString().toUpperCase();

            switch (roleName) {
                // --- Grade roles ---
                case "SUPER_ADMIN" -> roles.add(AppRole.SUPER_ADMIN);
                case "ADMIN"       -> roles.add(AppRole.ADMIN);
                case "MANAGER"     -> roles.add(AppRole.MANAGER);
                case "USER"        -> roles.add(AppRole.USER);
                case "VIEWER"      -> roles.add(AppRole.VIEWER);

                // --- CRUD permission roles ---
                // DOCUMENT_READ → view documents
                case "DOCUMENT_READ"     -> permissions.add(AppPermission.DOCUMENT_READ);

                // DOCUMENT_WRITE → create new documents and update existing ones
                case "DOCUMENT_WRITE"    -> {
                    permissions.add(AppPermission.DOCUMENT_CREATE);
                    permissions.add(AppPermission.DOCUMENT_UPDATE);
                }

                // DOCUMENT_DELETE → move to trash / delete
                case "DOCUMENT_DELETE"   -> permissions.add(AppPermission.DOCUMENT_DELETE);

                // DOCUMENT_DOWNLOAD → download is a read operation in the current model.
                // If a separate DOCUMENT_DOWNLOAD permission is added to AppPermission later,
                // update this mapping.
                case "DOCUMENT_DOWNLOAD" -> permissions.add(AppPermission.DOCUMENT_READ);

                // DOCUMENT_ADMIN → full document and folder management
                case "DOCUMENT_ADMIN"    -> {
                    permissions.add(AppPermission.DOCUMENT_READ);
                    permissions.add(AppPermission.DOCUMENT_CREATE);
                    permissions.add(AppPermission.DOCUMENT_UPDATE);
                    permissions.add(AppPermission.DOCUMENT_DELETE);
                    permissions.add(AppPermission.FOLDER_READ);
                    permissions.add(AppPermission.FOLDER_CREATE);
                    permissions.add(AppPermission.FOLDER_UPDATE);
                    permissions.add(AppPermission.FOLDER_DELETE);
                }

                // Ignore Keycloak system roles (offline_access, uma_authorization, default-roles-*)
                default -> { /* no-op */ }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. resource_access.ged-boot.roles (DOC_TYPE_*) → documentTypeAccess
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads {@code resource_access.ged-boot.roles} from the JWT and collects all roles
     * prefixed with {@code DOC_TYPE_}, stripping the prefix to return the bare type key.
     *
     * <p>Example: {@code DOC_TYPE_FINANCE} → {@code "FINANCE"}</p>
     *
     * <p>These keys are compared against {@link com.awb.ged.domain.category.model.Category#getSecurityClass()}
     * by {@code DocumentAccessValidatorImpl} to enforce document type access control.</p>
     */
    @SuppressWarnings("unchecked")
    private Set<String> mapDocumentTypeRoles(Jwt jwt) {
        Set<String> result = new HashSet<>();

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) return result;

        Object clientAccessObj = resourceAccess.get(KEYCLOAK_CLIENT_ID);
        if (!(clientAccessObj instanceof Map)) return result;

        Map<?, ?> clientAccess = (Map<?, ?>) clientAccessObj;
        Object clientRolesObj  = clientAccess.get("roles");
        if (!(clientRolesObj instanceof Collection)) return result;

        for (Object roleObj : (Collection<?>) clientRolesObj) {
            String role = roleObj.toString().toUpperCase();
            if (role.startsWith(DOC_TYPE_PREFIX)) {
                // "DOC_TYPE_FINANCE" → "FINANCE"
                result.add(role.substring(DOC_TYPE_PREFIX.length()));
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. groups → departmentId resolution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the user's {@link UUID} department identifier from the Keycloak {@code groups} claim.
     *
     * <p>Resolution strategy (in order):</p>
     * <ol>
     *   <li>Look up the user in the local {@code users} table by {@code keycloakSub};
     *       if found and {@code departmentId} is set, use it directly.</li>
     *   <li>Fallback: extract the group name from the first {@code groups} entry (strip leading "/"),
     *       then find a matching {@code Department} by case-insensitive name comparison.
     *       This covers users not yet synchronized in the local database.</li>
     * </ol>
     *
     * @param jwt the decoded JWT
     * @param sub the Keycloak subject (user UUID)
     * @return the resolved department UUID, or {@code null} if not determinable
     */
    private UUID resolveDepartmentId(Jwt jwt, String sub) {
        // Strategy 1 — resolve from local users table (fastest, most reliable)
        if (sub != null) {
            try {
                Optional<User> userOpt = userRepositoryPort.findByKeycloakSub(sub);
                if (userOpt.isPresent() && userOpt.get().getDepartmentId() != null) {
                    return userOpt.get().getDepartmentId();
                }
            } catch (Exception e) {
                log.debug("Could not resolve departmentId from local user table for sub={}: {}", sub, e.getMessage());
            }
        }

        // Strategy 2 — fallback: match group name against department name in DB
        List<String> groups = jwt.getClaim("groups");
        if (groups == null || groups.isEmpty()) return null;

        // Use the first group entry only (primary department group)
        String groupName = groups.get(0).replace("/", "").trim().toUpperCase();

        try {
            List<Department> departments = departmentRepositoryPort.findAll();
            for (Department dept : departments) {
                if (dept.getName() != null && dept.getName().toUpperCase().equals(groupName)) {
                    log.debug("Resolved departmentId={} for group={} via name fallback", dept.getId(), groupName);
                    return dept.getId();
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve departmentId via department name fallback for group={}: {}", groupName, e.getMessage());
        }

        return null;
    }
}