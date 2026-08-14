package com.awb.ged.infrastructure.security;

import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.application.port.out.persistence.DepartmentRepositoryPort;
import com.awb.ged.application.port.out.persistence.DocumentPermissionRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.domain.category.model.Category;
import com.awb.ged.domain.department.model.Department;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <h1>DocumentAccessValidatorImpl</h1>
 * <p>
 * Implementation of the {@link DocumentAccessValidator} port in the infrastructure layer.
 * Evaluates dynamic Keycloak group and department authorization rules against Spring Security GrantedAuthorities.
 * </p>
 */
@Service
public class DocumentAccessValidatorImpl implements DocumentAccessValidator {

    private static final Logger log = LoggerFactory.getLogger(DocumentAccessValidatorImpl.class);

    private static final Set<String> BYPASS_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_MANAGER",
            "ADMIN", "SUPER_ADMIN", "MANAGER"
    );

    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final DocumentPermissionRepositoryPort documentPermissionRepositoryPort;

    @Autowired
    public DocumentAccessValidatorImpl(DepartmentRepositoryPort departmentRepositoryPort,
                                       CategoryRepositoryPort categoryRepositoryPort,
                                       DocumentPermissionRepositoryPort documentPermissionRepositoryPort) {
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.documentPermissionRepositoryPort = documentPermissionRepositoryPort;
    }

    @Override
    public void validateAccess(Document document, UUID userId, String requiredAction) {
        if (document == null) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException(ErrorCode.UNAUTHORIZED, "Utilisateur non authentifié.");
        }

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

        // 1. Bypass check for ADMIN / MANAGER roles
        if (hasBypassRole(authorities)) {
            return;
        }

        // 2. Ownership check
        if (userId != null && userId.equals(document.getOwnerId())) {
            return;
        }

        // 3. Check explicit document ACL grants
        if (hasExplicitAclGrant(document.getId(), userId, authorities, requiredAction)) {
            return;
        }

        // 4. Check Department / Entity restriction
        boolean deptOk = false;
        String deptNameResolved = null;
        if (document.getDepartmentId() != null) {
            Optional<Department> deptOpt = departmentRepositoryPort.findById(document.getDepartmentId());
            if (deptOpt.isPresent()) {
                deptNameResolved = deptOpt.get().getName();
                if (!matchesUserGroups(deptNameResolved, authorities)) {
                    log.warn("Access DENIED: User {} with authorities {} attempted action {} on document {} belonging to department {}",
                            userId, authorities, requiredAction, document.getId(), deptNameResolved);
                    throw new ForbiddenException(ErrorCode.FORBIDDEN,
                            "Accès refusé. Vous n'avez pas l'habilitation Keycloak pour l'entité/département « " + deptNameResolved + " ».");
                }
                deptOk = true;
            }
        }

        // 5. Check Category / Document Type restriction (Double Habilitation: Department OK AND Category OK)
        if (document.getCategoryId() != null) {
            Optional<Category> catOpt = categoryRepositoryPort.findById(document.getCategoryId());
            if (catOpt.isPresent()) {
                String securityClass = catOpt.get().getSecurityClass();

                // If no securityClass is defined on this category, no type restriction applies
                if (securityClass != null && !securityClass.isBlank()) {
                    // a) Global department group membership automatically covers categories of that department (Choix 1)
                    boolean matchesDeptGroup = deptOk;

                    // b) Direct category group match (GROUP_FINANCE matches securityClass=FINANCE)
                    boolean matchesDirectGroup = matchesUserGroups(securityClass, authorities);

                    // c) Explicit DOC_TYPE_ client role match (ROLE_DOC_TYPE_FINANCE matches securityClass=FINANCE)
                    boolean matchesDocTypeRole = matchesDocTypeRole(securityClass, authorities);

                    if (!matchesDirectGroup && !matchesDeptGroup && !matchesDocTypeRole) {
                        log.warn("Access DENIED: User {} with authorities {} attempted action {} on document {} belonging to category securityClass {}",
                                userId, authorities, requiredAction, document.getId(), securityClass);
                        throw new ForbiddenException(ErrorCode.FORBIDDEN,
                                "Accès refusé. Vous n'avez pas l'habilitation pour le type de document '" + securityClass + "'.");
                    }
                }
            }
        }
    }

    private boolean hasBypassRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) return false;
        return authorities.stream()
                .anyMatch(a -> BYPASS_ROLES.contains(a.getAuthority().toUpperCase()));
    }

    private boolean hasExplicitAclGrant(UUID documentId, UUID userId, Collection<? extends GrantedAuthority> authorities, String requiredAction) {
        if (documentId == null) return false;

        List<DocumentPermission> permissions = documentPermissionRepositoryPort.findByDocumentId(documentId);
        if (permissions == null || permissions.isEmpty()) return false;

        Set<String> userGroupAuthorities = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("GROUP_"))
                .map(a -> a.substring(6))
                .collect(Collectors.toSet());

        for (DocumentPermission perm : permissions) {
            boolean userMatch = userId != null && userId.equals(perm.getUserId());
            boolean groupMatch = perm.getGroupId() != null; // Checked against group permissions if mapped

            if (userMatch || groupMatch) {
                if ("DELETE".equalsIgnoreCase(requiredAction) && perm.isCanDelete()) return true;
                if ("WRITE".equalsIgnoreCase(requiredAction) && perm.isCanWrite()) return true;
                if ("READ".equalsIgnoreCase(requiredAction) && perm.isCanRead()) return true;
            }
        }
        return false;
    }

    private boolean matchesUserGroups(String targetName, Collection<? extends GrantedAuthority> authorities) {
        if (targetName == null || targetName.isBlank() || authorities == null) return false;

        String normalizedTarget = targetName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        for (GrantedAuthority authority : authorities) {
            String authName = authority.getAuthority();
            if (authName.startsWith("GROUP_")) {
                String groupName = authName.substring(6).replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                if (groupName.equals(normalizedTarget) || normalizedTarget.contains(groupName) || groupName.contains(normalizedTarget)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether any of the user's Spring Security authorities match a Keycloak client role
     * of the form {@code ROLE_DOC_TYPE_{securityClass}}.
     *
     * <p>Example: if {@code securityClass = "FINANCE"}, this method checks for authority
     * {@code ROLE_DOC_TYPE_FINANCE}, which is produced by {@code KeycloakJwtRoleConverter}
     * from the client role {@code DOC_TYPE_FINANCE} on the {@code ged-boot} client.</p>
     *
     * @param securityClass the category security classification key (e.g., "FINANCE")
     * @param authorities   the user's Spring Security granted authorities
     * @return {@code true} if the user holds the matching DOC_TYPE_ client role
     */
    private boolean matchesDocTypeRole(String securityClass, Collection<? extends GrantedAuthority> authorities) {
        if (securityClass == null || securityClass.isBlank() || authorities == null) return false;

        String expected = "ROLE_DOC_TYPE_" + securityClass.toUpperCase().replaceAll("[^A-Z0-9]", "");

        return authorities.stream()
                .anyMatch(a -> a.getAuthority().toUpperCase().equals(expected));
    }
}
