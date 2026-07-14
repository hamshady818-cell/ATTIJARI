package com.awb.ged.common.security;

/**
 * <h1>AppRole</h1>
 * <p>
 * Defines the main application roles mapping to Keycloak realm roles.
 * These roles establish the broad tier of access granted to a user.
 * </p>
 */
public enum AppRole {
    /** Total administration, config, and monitoring permissions */
    SUPER_ADMIN,

    /** Administrative actions (user management, settings) */
    ADMIN,

    /** Manager actions (approvals, department supervision, documents creation/management) */
    MANAGER,

    /** Regular system user (standard CRUD on authorized documents/folders) */
    USER,

    /** Read-only observer (can browse and view documents but cannot modify or upload) */
    VIEWER
}
