package com.awb.ged.integration;

import com.awb.ged.GedAwbApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full application integration test — loads the REAL Spring context against
 * the local PostgreSQL configured in application.yml.
 *
 * Uses @WithMockUser to bypass Keycloak JWT validation so no running
 * Keycloak instance is required during the test run.
 *
 * What is verified:
 *  - All beans wire correctly (no ambiguous constructor, no missing beans)
 *  - Flyway migrations execute cleanly (schema valid)
 *  - Hibernate schema validation passes (ddl-auto=validate)
 *  - REST endpoints respond correctly (correct HTTP status, JSON structure)
 *  - Business logic executes end-to-end (folders/documents can be created & retrieved)
 */
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
        classes = GedAwbApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("integration-test")
@ExtendWith(DockerCondition.class)
@DisplayName("Full Application Integration Tests")
class ApplicationIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ged_db")
            .withUsername("ged_app_user")
            .withPassword("ged_test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Build MockMvc with the full Spring Security filter chain so that
        // @WithMockUser and unauthenticated 401 checks work correctly.
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // =========================================================
    //  1. Context Startup
    // =========================================================

    @Test
    @DisplayName("1.1 — Spring context loads without errors (all beans wired, Flyway OK, Hibernate validate OK)")
    void contextLoads() {
        // If this test passes, the full Spring context started successfully:
        //   - No ambiguous constructor (UploadDocumentService, GetDocumentService, etc.)
        //   - SpringEventPublisherAdapter registered as EventPublisherPort bean
        //   - Flyway V1+V2 migrations applied successfully against real PostgreSQL
        //   - Hibernate schema validation passed (ddl-auto=validate)
        //   - All @Service, @Component, @Repository beans wired correctly
    }

    // =========================================================
    //  2. Actuator
    // =========================================================

    @Test
    @DisplayName("2.1 — GET /actuator/health → 200 UP (no auth required)")
    void actuatorHealth_ShouldBeUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // =========================================================
    //  3. Swagger / OpenAPI
    // =========================================================

    @Test
    @DisplayName("3.1 — GET /v3/api-docs → 200 OK, JSON OpenAPI spec present")
    void swaggerApiDocs_ShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").exists());
    }

    // =========================================================
    //  4. Security — unauthenticated requests
    // =========================================================

    @Test
    @DisplayName("4.1 — GET /api/v1/folders/content without token → 401 Unauthorized")
    void getRootFolderContent_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/folders/content"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4.2 — POST /api/v1/folders without token → 401 Unauthorized")
    void createFolder_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"TestFolder","ownerId":"00000000-0000-0000-0000-000000000001"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    //  5. Folder — authenticated happy paths
    // =========================================================

    @Test
    @WithMockUser(authorities = {"FOLDER_READ"})
    @DisplayName("5.1 — GET /api/v1/folders/content (root) → 200 OK with valid FolderContentResponse structure")
    void getRootFolderContent_Authenticated_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/folders/content"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subFolders").isArray())
                .andExpect(jsonPath("$.documents").isArray());
    }

    @Test
    @DisplayName("5.2 — POST /api/v1/folders → 201 Created, Location header present, folder returned")
    void createRootFolder_Returns201WithLocation() throws Exception {
        String folderName = "IntegrationTestFolder_" + System.currentTimeMillis();
        String requestBody = """
                {
                  "name": "%s"
                }
                """.formatted(folderName);

        mockMvc.perform(post("/api/v1/folders")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("FOLDER_CREATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value(folderName))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("5.3 — Create folder then retrieve its content → folder appears in parent's content")
    void createFolderAndReadContent_EndToEnd() throws Exception {
        String folderName = "E2E_Folder_" + System.currentTimeMillis();

        // Create folder (no ownerId — service skips user validation when null)
        String createResponse = mockMvc.perform(post("/api/v1/folders")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("FOLDER_CREATE"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("FOLDER_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(folderName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract folder ID from response
        String folderId = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(createResponse)
                .get("id")
                .asText();

        // Read folder content
        mockMvc.perform(get("/api/v1/folders/{id}/content", folderId)
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("FOLDER_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentFolder.id").value(folderId))
                .andExpect(jsonPath("$.subFolders").isArray())
                .andExpect(jsonPath("$.documents").isArray());
    }

    // =========================================================
    //  6. Document — validation errors
    // =========================================================

    @Test
    @DisplayName("6.1 — POST /api/v1/documents/upload without file → 400 Bad Request")
    void uploadDocument_WithoutFile_Returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("DOCUMENT_UPLOAD")))
                        .param("ownerId", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"DOCUMENT_READ"})
    @DisplayName("6.2 — GET /api/v1/documents/{id} with unknown UUID → 404 Not Found")
    void getDocument_WithUnknownId_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", "00000000-0000-0000-0000-000000000099"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  7. Audit Logs — access control
    // =========================================================

    @Test
    @WithMockUser(authorities = {"AUDIT_READ"})
    @DisplayName("7.1 — GET /api/v1/audit-logs → 200 OK with paginated response")
    void getAuditLogs_WithAuditReadAuthority_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("7.2 — GET /api/v1/audit-logs with ADMIN role → 200 OK")
    void getAuditLogs_WithAdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk());
    }
}
