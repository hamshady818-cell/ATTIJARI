package com.awb.ged.infrastructure.security.permission;

import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.in.security.PermissionService;
import com.awb.ged.common.security.AppPermission;
import com.awb.ged.common.security.AppRole;
import com.awb.ged.common.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * <h1>PermissionServiceImpl</h1>
 * <p>
 * Implementation of the {@link PermissionService} port.
 * Manages the functional mapping between high-level security roles ({@link AppRole})
 * and fine-grained permissions ({@link AppPermission}).
 * </p>
 */
@Service("securityService")
public class PermissionServiceImpl implements PermissionService {

    private final CurrentUserProvider currentUserProvider;
    private final Map<AppRole, Set<AppPermission>> rolePermissionMatrix;

    public PermissionServiceImpl(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
        this.rolePermissionMatrix = new EnumMap<>(AppRole.class);
        initializeMatrix();
    }

    private void initializeMatrix() {
        // SUPER_ADMIN and ADMIN have access to everything
        rolePermissionMatrix.put(AppRole.SUPER_ADMIN, EnumSet.allOf(AppPermission.class));
        rolePermissionMatrix.put(AppRole.ADMIN, EnumSet.allOf(AppPermission.class));

        // MANAGER permissions
        rolePermissionMatrix.put(AppRole.MANAGER, EnumSet.of(
                AppPermission.DOCUMENT_READ,
                AppPermission.DOCUMENT_CREATE,
                AppPermission.DOCUMENT_UPDATE,
                AppPermission.DOCUMENT_DELETE,
                AppPermission.FOLDER_READ,
                AppPermission.FOLDER_CREATE,
                AppPermission.FOLDER_UPDATE,
                AppPermission.FOLDER_DELETE,
                AppPermission.AUDIT_READ,
                AppPermission.AI_CHAT,
                AppPermission.AI_SUMMARIZE,
                AppPermission.AI_SEARCH
        ));

        // USER permissions
        rolePermissionMatrix.put(AppRole.USER, EnumSet.of(
                AppPermission.DOCUMENT_READ,
                AppPermission.DOCUMENT_CREATE,
                AppPermission.DOCUMENT_UPDATE,
                AppPermission.FOLDER_READ,
                AppPermission.FOLDER_CREATE,
                AppPermission.AI_CHAT,
                AppPermission.AI_SUMMARIZE,
                AppPermission.AI_SEARCH
        ));

        // VIEWER permissions (read-only)
        rolePermissionMatrix.put(AppRole.VIEWER, EnumSet.of(
                AppPermission.DOCUMENT_READ,
                AppPermission.FOLDER_READ,
                AppPermission.AI_SEARCH
        ));
    }

    @Override
    public boolean hasPermission(AppPermission permission) {
        return currentUserProvider.getCurrentUser()
                .map(user -> hasPermission(user, permission))
                .orElse(false);
    }

    /**
     * Checks if a specific {@link CurrentUser} has the requested permission.
     * Accessible by argument resolver or custom filters.
     *
     * @param user       the user context to check
     * @param permission the permission to check
     * @return true if the user possesses the permission, false otherwise
     */
    public boolean hasPermission(CurrentUser user, AppPermission permission) {
        if (user == null || permission == null) {
            return false;
        }
        
        // 1. Direct permission checks (if permissions are explicitly loaded on the CurrentUser object)
        if (user.getPermissions() != null && user.getPermissions().contains(permission)) {
            return true;
        }

        // 2. Fallback to Role-to-Permission mapping matrix
        if (user.getRoles() != null) {
            for (AppRole role : user.getRoles()) {
                Set<AppPermission> rolePermissions = rolePermissionMatrix.get(role);
                if (rolePermissions != null && rolePermissions.contains(permission)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Set<AppPermission> resolvePermissions(Set<AppRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return EnumSet.noneOf(AppPermission.class);
        }
        Set<AppPermission> permissions = EnumSet.noneOf(AppPermission.class);
        for (AppRole role : roles) {
            Set<AppPermission> rolePermissions = rolePermissionMatrix.get(role);
            if (rolePermissions != null) {
                permissions.addAll(rolePermissions);
            }
        }
        return permissions;
    }
}
