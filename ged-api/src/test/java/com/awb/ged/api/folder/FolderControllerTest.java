package com.awb.ged.api.folder;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.api.folder.dto.CreateFolderRequest;
import com.awb.ged.api.permission.dto.GrantPermissionRequest;
import com.awb.ged.application.dto.folder.FolderContentResponseDto;
import com.awb.ged.application.dto.folder.FolderResponseDto;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.port.in.folder.CreateFolderUseCase;
import com.awb.ged.application.port.in.folder.DeleteFolderUseCase;
import com.awb.ged.application.port.in.folder.GetAllFoldersUseCase;
import com.awb.ged.application.port.in.folder.GetFolderContentUseCase;
import com.awb.ged.application.port.in.folder.GrantFolderPermissionUseCase;
import com.awb.ged.application.port.in.folder.RevokeFolderPermissionUseCase;
import com.awb.ged.application.port.in.folder.ListFolderPermissionsUseCase;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FolderController.class)
@Import({GlobalExceptionHandler.class, FolderControllerTest.MethodSecurityConfig.class})
class FolderControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CreateFolderUseCase createFolderUseCase;

    @MockitoBean
    private GetFolderContentUseCase getFolderContentUseCase;

    @MockitoBean
    private GetAllFoldersUseCase getAllFoldersUseCase;

    @MockitoBean
    private DeleteFolderUseCase deleteFolderUseCase;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UserRepositoryPort userRepositoryPort;

    @MockitoBean
    private GrantFolderPermissionUseCase grantFolderPermissionUseCase;

    @MockitoBean
    private RevokeFolderPermissionUseCase revokeFolderPermissionUseCase;

    @MockitoBean
    private ListFolderPermissionsUseCase listFolderPermissionsUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        CurrentUser currentUser = CurrentUser.builder()
                .keycloakSub("sub-123")
                .username("testuser")
                .email("user@awb.ma")
                .build();

        given(currentUserProvider.getRequiredCurrentUser()).willReturn(currentUser);
        given(userRepositoryPort.findByKeycloakSub("sub-123"))
                .willReturn(Optional.of(User.builder().id(userId).keycloakSub("sub-123").build()));
    }

    @Test
    @WithMockUser(authorities = "FOLDER_CREATE")
    @DisplayName("POST /api/v1/folders - Should create folder and return 201 Created")
    void createFolder_Success() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Finance 2024")
                .build();

        FolderResponseDto responseDto = FolderResponseDto.builder()
                .id(folderId)
                .name("Finance 2024")
                .ownerId(userId)
                .createdAt(Instant.now())
                .build();

        given(createFolderUseCase.createFolder(any())).willReturn(responseDto);

        // When / Then
        mockMvc.perform(post("/api/v1/folders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/folders/" + folderId))
                .andExpect(jsonPath("$.id").value(folderId.toString()))
                .andExpect(jsonPath("$.name").value("Finance 2024"));
    }

    @Test
    @WithMockUser(authorities = "FOLDER_CREATE")
    @DisplayName("POST /api/v1/folders - Should return 400 Bad Request on blank name")
    void createFolder_BlankName_Returns400() throws Exception {
        // Given
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("   ")
                .build();

        // When / Then
        mockMvc.perform(post("/api/v1/folders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-SYS-002"));
    }

    @Test
    @WithMockUser(authorities = "FOLDER_CREATE")
    @DisplayName("POST /api/v1/folders - Should return 409 Conflict when name already exists")
    void createFolder_Duplicate_Returns409() throws Exception {
        // Given
        CreateFolderRequest request = CreateFolderRequest.builder().name("Existing").build();
        given(createFolderUseCase.createFolder(any()))
                .willThrow(new ConflictException(ErrorCode.FOLDER_DUPLICATE, "Folder named Existing already exists."));

        // When / Then
        mockMvc.perform(post("/api/v1/folders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR-FLD-002"));
    }

    @Test
    @WithMockUser(authorities = "FOLDER_READ")
    @DisplayName("GET /api/v1/folders/{id}/content - Should return 200 OK with folder content")
    void getFolderContent_Success() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        FolderContentResponseDto contentDto = FolderContentResponseDto.builder()
                .currentFolder(FolderResponseDto.builder().id(folderId).name("Finance").build())
                .subFolders(List.of())
                .documents(List.of())
                .build();

        given(getFolderContentUseCase.getFolderContent(folderId)).willReturn(contentDto);

        // When / Then
        mockMvc.perform(get("/api/v1/folders/{id}/content", folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentFolder.name").value("Finance"));
    }

    @Test
    @WithMockUser(authorities = "FOLDER_READ")
    @DisplayName("GET /api/v1/folders/{id}/content - Should return 404 Not Found when folder missing")
    void getFolderContent_NotFound_Returns404() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        given(getFolderContentUseCase.getFolderContent(folderId))
                .willThrow(new NotFoundException(ErrorCode.FOLDER_NOT_FOUND, "Folder not found"));

        // When / Then
        mockMvc.perform(get("/api/v1/folders/{id}/content", folderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR-FLD-001"));
    }

    @Test
    @WithMockUser(authorities = "FOLDER_DELETE")
    @DisplayName("DELETE /api/v1/folders/{id} - Should delete folder and return 204 No Content")
    void deleteFolder_Success() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        doNothing().when(deleteFolderUseCase).deleteFolder(eq(folderId), eq(userId), anyBoolean());

        // When / Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/folders/{id}", folderId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "FOLDER_DELETE")
    @DisplayName("DELETE /api/v1/folders/{id} - Should return 404 when folder not found")
    void deleteFolder_NotFound_Returns404() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        doThrow(new NotFoundException(ErrorCode.FOLDER_NOT_FOUND, "Folder not found"))
                .when(deleteFolderUseCase).deleteFolder(eq(folderId), eq(userId), anyBoolean());

        // When / Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/folders/{id}", folderId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR-FLD-001"));
    }

    @Test
    @WithMockUser(authorities = "FOLDER_DELETE")
    @DisplayName("DELETE /api/v1/folders/{id} - Should return 409 when folder is not empty")
    void deleteFolder_NotEmpty_Returns409() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        doThrow(new ConflictException(ErrorCode.FOLDER_NOT_EMPTY, "Folder is not empty"))
                .when(deleteFolderUseCase).deleteFolder(eq(folderId), eq(userId), anyBoolean());

        // When / Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/folders/{id}", folderId)
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR-FLD-004"));
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_AUTH")
    @DisplayName("DELETE /api/v1/folders/{id} - Should return 403 when user lacks authority")
    void deleteFolder_Forbidden_Returns403() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();

        // When / Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/folders/{id}", folderId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FOLDER_WRITE")
    @DisplayName("POST /api/v1/folders/{id}/permissions - Should grant permission and return 201 Created")
    void grantPermission_Success() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID permId = UUID.randomUUID();
        GrantPermissionRequest request = GrantPermissionRequest.builder()
                .userId(UUID.randomUUID())
                .canRead(true)
                .build();

        PermissionResponseDto responseDto = PermissionResponseDto.builder()
                .id(permId)
                .targetId(folderId)
                .canRead(true)
                .build();

        given(grantFolderPermissionUseCase.grantPermission(any(), anyBoolean())).willReturn(responseDto);

        // When / Then
        mockMvc.perform(post("/api/v1/folders/{id}/permissions", folderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(permId.toString()))
                .andExpect(jsonPath("$.targetId").value(folderId.toString()))
                .andExpect(jsonPath("$.canRead").value(true));
    }
}
