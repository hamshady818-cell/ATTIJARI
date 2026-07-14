package com.awb.ged.application.port.in.security;

import com.awb.ged.common.security.AppPermission;
import com.awb.ged.common.security.AppRole;

import java.util.Set;

/**
 * <h1>PermissionService</h1>
 * <p>
 * Input Port interface defining role-to-permission mapping and evaluation capabilities.
 * Allows checking if the currently authenticated user has the required functional permissions.
 * </p>
 */
public interface PermissionService {

    /**
     * Checks if the currently authenticated context possesses the specified functional permission.
     *
     * @param permission the functional permission to check
     * @return true if the current user has the permission, false otherwise
     */
    boolean hasPermission(AppPermission permission);

    /**
     * Resolves the set of fine-grained permissions associated with a set of roles.
     *
     * @param roles the set of roles to map
     * @return the set of corresponding permissions
     */
    Set<AppPermission> resolvePermissions(Set<AppRole> roles);
}
