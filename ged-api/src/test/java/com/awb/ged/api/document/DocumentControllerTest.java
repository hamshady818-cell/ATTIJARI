package com.awb.ged.api.document;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.api.permission.dto.GrantPermissionRequest;
import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.port.in.document.DeleteDocumentUseCase;
import com.awb.ged.application.port.in.document.GetDocumentUseCase;
import com.awb.ged.application.port.in.document.UploadDocumentUseCase;
import com.awb.ged.application.port.in.document.GrantDocumentPermissionUseCase;
import com.awb.ged.application.port.in.document.RevokeDocumentPermissionUseCase;
import com.awb.ged.application.port.in.document.ListDocumentPermissionsUseCase;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@Import(GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadDocumentUseCase uploadDocumentUseCase;

    @MockitoBean
    private GetDocumentUseCase getDocumentUseCase;

    @MockitoBean
    private DeleteDocumentUseCase deleteDocumentUseCase;

    @MockitoBean
    private GrantDocumentPermissionUseCase grantDocumentPermissionUseCase;

    @MockitoBean
    private RevokeDocumentPermissionUseCase revokeDocumentPermissionUseCase;

    @MockitoBean
    private ListDocumentPermissionsUseCase listDocumentPermissionsUseCase;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        CurrentUser currentUser = CurrentUser.builder()
                .keycloakSub("sub-456")
                .username("docuser")
                .email("docuser@awb.ma")
                .build();

        given(currentUserProvider.getRequiredCurrentUser()).willReturn(currentUser);
        given(userRepositoryPort.findByKeycloakSub("sub-456"))
                .willReturn(Optional.of(User.builder().id(userId).keycloakSub("sub-456").build()));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_CREATE")
    @DisplayName("POST /api/v1/documents/upload - Should upload document and return 201 Created")
    void uploadDocument_Success() throws Exception {
        // Given
        UUID docId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "Contract.pdf", "application/pdf", "Dummy PDF Bytes".getBytes()
        );

        DocumentResponseDto responseDto = DocumentResponseDto.builder()
                .id(docId)
                .name("Contract 2024")
                .ownerId(userId)
                .build();

        given(uploadDocumentUseCase.uploadDocument(any())).willReturn(responseDto);

        // When / Then
        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .param("name", "Contract 2024")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/documents/" + docId))
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.name").value("Contract 2024"));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_CREATE")
    @DisplayName("POST /api/v1/documents/upload - Should return 400 Bad Request on disallowed MIME type")
    void uploadDocument_DisallowedMimeType_Returns400() throws Exception {
        // Given
        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "malicious.exe", "application/x-msdownload", "Executable Bytes".getBytes()
        );

        // When / Then
        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(exeFile)
                        .param("name", "Malicious Exe")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-DOC-005"));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_READ")
    @DisplayName("GET /api/v1/documents/{id} - Should return 200 OK with document details")
    void getDocumentById_Success() throws Exception {
        // Given
        UUID docId = UUID.randomUUID();
        DocumentResponseDto responseDto = DocumentResponseDto.builder()
                .id(docId)
                .name("Invoice.pdf")
                .build();

        given(getDocumentUseCase.getDocumentById(docId)).willReturn(responseDto);

        // When / Then
        mockMvc.perform(get("/api/v1/documents/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.name").value("Invoice.pdf"));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_READ")
    @DisplayName("GET /api/v1/documents/{id} - Should return 404 Not Found when document missing")
    void getDocumentById_NotFound_Returns404() throws Exception {
        // Given
        UUID docId = UUID.randomUUID();
        given(getDocumentUseCase.getDocumentById(docId))
                .willThrow(new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document not found"));

        // When / Then
        mockMvc.perform(get("/api/v1/documents/{id}", docId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR-DOC-001"));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_WRITE")
    @DisplayName("POST /api/v1/documents/{id}/permissions - Should grant permission and return 201 Created")
    void grantPermission_Success() throws Exception {
        // Given
        UUID docId = UUID.randomUUID();
        UUID permId = UUID.randomUUID();
        GrantPermissionRequest request = GrantPermissionRequest.builder()
                .userId(UUID.randomUUID())
                .canRead(true)
                .build();

        PermissionResponseDto responseDto = PermissionResponseDto.builder()
                .id(permId)
                .targetId(docId)
                .canRead(true)
                .build();

        given(grantDocumentPermissionUseCase.grantPermission(any(), anyBoolean())).willReturn(responseDto);

        ObjectMapper objectMapper = new ObjectMapper();

        // When / Then
        mockMvc.perform(post("/api/v1/documents/{id}/permissions", docId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(permId.toString()))
                .andExpect(jsonPath("$.targetId").value(docId.toString()))
                .andExpect(jsonPath("$.canRead").value(true));
    }
}
