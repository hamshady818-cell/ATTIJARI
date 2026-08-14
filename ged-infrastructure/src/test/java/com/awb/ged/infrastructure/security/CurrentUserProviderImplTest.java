package com.awb.ged.infrastructure.security;

import com.awb.ged.application.port.out.persistence.DepartmentRepositoryPort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.security.AppPermission;
import com.awb.ged.common.security.AppRole;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.department.model.Department;
import com.awb.ged.domain.user.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * <h1>CurrentUserProviderImplTest</h1>
 * <p>
 * Verifies that {@link CurrentUserProviderImpl} correctly maps each JWT claim dimension
 * to the corresponding {@link CurrentUser} field, using a realistic Keycloak token structure.
 * </p>
 *
 * <p>Test token mirrors the real example in the project context:</p>
 * <pre>
 * {
 *   "realm_access": { "roles": ["USER", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_DOWNLOAD"] },
 *   "resource_access": { "ged-boot": { "roles": ["DOC_TYPE_FINANCE"] } },
 *   "groups": ["/FINANCE"],
 *   "preferred_username": "test.user",
 *   "email": "test.user@attijariwafa.com"
 * }
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class CurrentUserProviderImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private DepartmentRepositoryPort departmentRepositoryPort;

    private CurrentUserProviderImpl provider;

    private static final UUID USER_ID        = UUID.randomUUID();
    private static final UUID DEPT_ID_FINANCE = UUID.randomUUID();
    private static final String SUB           = "test-sub-001";

    @BeforeEach
    void setUp() {
        provider = new CurrentUserProviderImpl(userRepositoryPort, departmentRepositoryPort);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Main scenario: realistic test.user token
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should map USER role, DOCUMENT_READ permission, and FINANCE documentTypeAccess from JWT claims")
    void getCurrentUser_WithRealisticJwt_MapsAllDimensions() {
        // Given — build a JWT matching the realistic test.user token
        Jwt jwt = buildTestUserJwt();
        installJwtInSecurityContext(jwt);

        // User exists in local DB with departmentId
        User localUser = User.builder()
                .id(USER_ID)
                .keycloakSub(SUB)
                .departmentId(DEPT_ID_FINANCE)
                .build();
        given(userRepositoryPort.findByKeycloakSub(SUB)).willReturn(Optional.of(localUser));

        // When
        Optional<CurrentUser> result = provider.getCurrentUser();

        // Then
        assertThat(result).isPresent();
        CurrentUser user = result.get();

        // 1. realm_access.roles USER → AppRole.USER
        assertThat(user.getRoles()).contains(AppRole.USER);
        assertThat(user.getRoles()).doesNotContain(AppRole.ADMIN, AppRole.SUPER_ADMIN);

        // 2. realm_access.roles DOCUMENT_READ → AppPermission.DOCUMENT_READ
        assertThat(user.getPermissions()).contains(AppPermission.DOCUMENT_READ);

        // 3. realm_access.roles DOCUMENT_WRITE → DOCUMENT_CREATE + DOCUMENT_UPDATE
        assertThat(user.getPermissions()).contains(AppPermission.DOCUMENT_CREATE, AppPermission.DOCUMENT_UPDATE);

        // 4. realm_access.roles DOCUMENT_DOWNLOAD → DOCUMENT_READ (already present, idempotent)
        assertThat(user.getPermissions()).contains(AppPermission.DOCUMENT_READ);

        // 5. resource_access.ged-boot.roles DOC_TYPE_FINANCE → documentTypeAccess = {"FINANCE"}
        assertThat(user.getDocumentTypeAccess()).containsExactly("FINANCE");

        // 6. groups /FINANCE + local user with departmentId → departmentId resolved
        assertThat(user.getDepartmentId()).isEqualTo(DEPT_ID_FINANCE);

        // 7. identity fields
        assertThat(user.getUsername()).isEqualTo("test.user");
        assertThat(user.getEmail()).isEqualTo("test.user@attijariwafa.com");
    }

    @Test
    @DisplayName("Should fall back to department name matching when user is not in local DB")
    void getCurrentUser_UserNotInDb_FallsBackToDepartmentNameMatch() {
        // Given
        Jwt jwt = buildTestUserJwt();
        installJwtInSecurityContext(jwt);

        // User NOT in local DB
        given(userRepositoryPort.findByKeycloakSub(SUB)).willReturn(Optional.empty());

        // Department exists with matching name
        Department financeDept = Department.builder()
                .id(DEPT_ID_FINANCE)
                .name("FINANCE")
                .build();
        given(departmentRepositoryPort.findAll()).willReturn(List.of(financeDept));

        // When
        Optional<CurrentUser> result = provider.getCurrentUser();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getDepartmentId()).isEqualTo(DEPT_ID_FINANCE);
    }

    @Test
    @DisplayName("Should map DOCUMENT_ADMIN realm role to all document and folder permissions")
    void getCurrentUser_WithDocumentAdminRole_MapsAllDocumentPermissions() {
        // Given
        Jwt jwt = buildJwtWithRealmRoles(List.of("USER", "DOCUMENT_ADMIN"));
        installJwtInSecurityContext(jwt);
        // No groups in JWT → departmentRepositoryPort.findAll() is never called (groups list is empty)
        given(userRepositoryPort.findByKeycloakSub(SUB)).willReturn(Optional.empty());

        // When
        CurrentUser user = provider.getRequiredCurrentUser();

        // Then
        assertThat(user.getPermissions()).containsAll(List.of(
                AppPermission.DOCUMENT_READ,
                AppPermission.DOCUMENT_CREATE,
                AppPermission.DOCUMENT_UPDATE,
                AppPermission.DOCUMENT_DELETE,
                AppPermission.FOLDER_READ,
                AppPermission.FOLDER_CREATE,
                AppPermission.FOLDER_UPDATE,
                AppPermission.FOLDER_DELETE
        ));
    }

    @Test
    @DisplayName("Should return DEV_USER when no JWT is in the security context")
    void getCurrentUser_NoJwt_ReturnsDevUser() {
        // Given — no JWT in security context (null authentication)
        SecurityContextHolder.clearContext();

        // When
        Optional<CurrentUser> result = provider.getCurrentUser();

        // Then — DEV_USER is returned (local dev mode)
        assertThat(result).isPresent();
        assertThat(result.get().getRoles()).contains(AppRole.SUPER_ADMIN);
    }

    @Test
    @DisplayName("Should include multiple documentTypeAccess keys when user has multiple DOC_TYPE roles")
    void getCurrentUser_MultipleDocTypeRoles_MapsAll() {
        // Given
        Jwt jwt = buildJwtWithDocTypeRoles(List.of("DOC_TYPE_FINANCE", "DOC_TYPE_RH"));
        installJwtInSecurityContext(jwt);
        // No groups in JWT → departmentRepositoryPort.findAll() is never called (groups list is empty)
        given(userRepositoryPort.findByKeycloakSub(SUB)).willReturn(Optional.empty());

        // When
        CurrentUser user = provider.getRequiredCurrentUser();

        // Then
        assertThat(user.getDocumentTypeAccess()).containsExactlyInAnyOrder("FINANCE", "RH");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  JWT helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Builds the realistic test.user JWT matching the project context example. */
    private Jwt buildTestUserJwt() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(SUB)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("preferred_username", "test.user")
                .claim("email", "test.user@attijariwafa.com")
                .claim("realm_access", Map.of(
                        "roles", List.of("offline_access", "default-roles-ged-awb", "uma_authorization",
                                "USER", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_DOWNLOAD")
                ))
                .claim("resource_access", Map.of(
                        "ged-boot", Map.of("roles", List.of("DOC_TYPE_FINANCE"))
                ))
                .claim("groups", List.of("/FINANCE"))
                .build();
    }

    /** Builds a JWT with custom realm roles only. */
    private Jwt buildJwtWithRealmRoles(List<String> realmRoles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(SUB)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("preferred_username", "test.user")
                .claim("email", "test.user@attijariwafa.com")
                .claim("realm_access", Map.of("roles", realmRoles))
                .claim("resource_access", Map.of())
                .claim("groups", List.of())
                .build();
    }

    /** Builds a JWT with custom ged-boot client roles only. */
    private Jwt buildJwtWithDocTypeRoles(List<String> docTypeRoles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(SUB)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("preferred_username", "test.user")
                .claim("email", "test.user@attijariwafa.com")
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .claim("resource_access", Map.of(
                        "ged-boot", Map.of("roles", docTypeRoles)
                ))
                .claim("groups", List.of())
                .build();
    }

    /** Installs a Jwt principal into the Spring Security context for testing. */
    private void installJwtInSecurityContext(Jwt jwt) {
        Authentication auth = mock(Authentication.class);
        given(auth.isAuthenticated()).willReturn(true);
        given(auth.getPrincipal()).willReturn(jwt);

        SecurityContext context = mock(SecurityContext.class);
        given(context.getAuthentication()).willReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}
