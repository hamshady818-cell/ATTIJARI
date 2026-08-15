package com.awb.ged.api.document;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.api.permission.dto.GrantPermissionRequest;
import com.awb.ged.application.dto.document.DocumentLockResponseDto;
import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.DocumentSearchResultDto;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.port.in.document.*;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.List;
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
    private SearchDocumentsUseCase searchDocumentsUseCase;

    @MockitoBean
    private DownloadDocumentUseCase downloadDocumentUseCase;

    @MockitoBean
    private PreviewDocumentUseCase previewDocumentUseCase;

    @MockitoBean
    private UploadNewVersionUseCase uploadNewVersionUseCase;

    @MockitoBean
    private ListDocumentVersionsUseCase listDocumentVersionsUseCase;

    @MockitoBean
    private CheckoutDocumentUseCase checkoutDocumentUseCase;

    @MockitoBean
    private CheckinDocumentUseCase checkinDocumentUseCase;

    @MockitoBean
    private GetDocumentLockUseCase getDocumentLockUseCase;

    @MockitoBean
    private UpdateDocumentUseCase updateDocumentUseCase;

    @MockitoBean
    private UpdateDocumentStatusUseCase updateDocumentStatusUseCase;

    @MockitoBean
    private BulkDocumentActionUseCase bulkDocumentActionUseCase;

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
    @WithMockUser(authorities = "DOCUMENT_READ")
    @DisplayName("GET /api/v1/documents/search - Should search documents and return 200 OK")
    void searchDocuments_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentSearchResultDto dto = DocumentSearchResultDto.builder()
                .id(docId)
                .name("SearchResult.pdf")
                .build();

        PageResponse<DocumentSearchResultDto> pageResponse = PageResponse.<DocumentSearchResultDto>builder()
                .content(List.of(dto))
                .totalElements(1)
                .totalPages(1)
                .build();

        given(searchDocumentsUseCase.search(any())).willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/documents/search")
                        .param("keyword", "SearchResult"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(docId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("SearchResult.pdf"));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_READ")
    @DisplayName("GET /api/v1/documents/{id}/download - Should stream download document content")
    void downloadDocument_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        byte[] content = "File binary content".getBytes();
        DownloadDocumentUseCase.DownloadResult downloadResult = new DownloadDocumentUseCase.DownloadResult(
                new ByteArrayInputStream(content),
                "Document.pdf",
                "application/pdf",
                content.length
        );

        given(downloadDocumentUseCase.download(eq(docId), any(), any())).willReturn(downloadResult);

        mockMvc.perform(get("/api/v1/documents/{id}/download", docId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Document.pdf\""))
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_WRITE")
    @DisplayName("POST /api/v1/documents/{id}/checkout - Should lock document and return 200 OK")
    void checkoutDocument_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentLockResponseDto lockResponse = DocumentLockResponseDto.builder()
                .documentId(docId)
                .locked(true)
                .lockedBy(userId)
                .build();

        given(checkoutDocumentUseCase.checkout(eq(docId), eq(userId))).willReturn(lockResponse);

        mockMvc.perform(post("/api/v1/documents/{id}/checkout", docId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.lockedBy").value(userId.toString()));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_WRITE")
    @DisplayName("PATCH /api/v1/documents/{id} - Should update document without moveToRoot property in JSON (200 OK)")
    void updateDocument_WithoutMoveToRoot_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentResponseDto updatedDto = DocumentResponseDto.builder()
                .id(docId)
                .name("test.pdf")
                .build();

        given(updateDocumentUseCase.updateDocument(eq(docId), any(), any())).willReturn(updatedDto);

        mockMvc.perform(patch("/api/v1/documents/{id}", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newName\": \"test.pdf\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.name").value("test.pdf"));
    }

    @Test
    @WithMockUser(authorities = "DOCUMENT_WRITE")
    @DisplayName("PATCH /api/v1/documents/{id} - Should update document with explicit null moveToRoot in JSON (200 OK)")
    void updateDocument_WithExplicitNullMoveToRoot_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentResponseDto updatedDto = DocumentResponseDto.builder()
                .id(docId)
                .name("test.pdf")
                .build();

        given(updateDocumentUseCase.updateDocument(eq(docId), any(), any())).willReturn(updatedDto);

        mockMvc.perform(patch("/api/v1/documents/{id}", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newName\": \"test.pdf\", \"moveToRoot\": null}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.name").value("test.pdf"));
    }
}
