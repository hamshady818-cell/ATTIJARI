package com.awb.ged.common.security;

import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

/**
 * <h1>CurrentUser</h1>
 * <p>
 * Represents the authenticated caller identity context in the GED-AWB system.
 * Isolates the application and domain layers from direct reliance on security frameworks
 * (such as Spring Security or Keycloak SDKs).
 * </p>
 * <p>
 * Contains key identification details, roles, functional permissions, and department context.
 * </p>
 */
@Value
@Builder
public class CurrentUser {

    /** Unique database ID of the user (resolved during synch / request filter) */
    UUID id;

    /** Stable Keycloak Subject ID (sub claim) */
    String keycloakSub;

    /** Username / Preferred username of the user */
    String username;

    /** Email address */
    String email;

    /** Set of roles assigned to the user */
    Set<AppRole> roles;

    /** Set of functional permissions resolved for the user */
    Set<AppPermission> permissions;

    /** Database identifier of the user's department (if assigned) */
    UUID departmentId;
}
