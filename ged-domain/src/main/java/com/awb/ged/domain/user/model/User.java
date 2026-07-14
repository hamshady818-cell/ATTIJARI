package com.awb.ged.domain.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>User</h1>
 * <p>
 * Core domain aggregate representing a user in the GED-AWB system.
 * User profiles are synchronized from Keycloak (the sole identity provider).
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** Unique application identifier for the user */
    private UUID id;

    /** Stable unique subject identifier (sub) from Keycloak */
    private String keycloakSub;

    /** Email address of the user */
    private String email;

    /** First name / Given name */
    private String firstName;

    /** Last name / Family name */
    private String lastName;

    /** Identifier of the department the user belongs to (optional) */
    private UUID departmentId;

    /** Active status of the user profile in the application */
    private boolean active;

    /** Timestamp of user profile synchronization/creation in UTC */
    private Instant createdAt;

    /** Timestamp of last user profile modification in UTC */
    private Instant updatedAt;
}
