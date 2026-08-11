package com.awb.ged.api.trash;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.application.dto.trash.TrashItemResponseDto;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.in.trash.GetTrashUseCase;
import com.awb.ged.application.port.in.trash.RestoreFromTrashUseCase;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.awb.ged.common.model.PageResponse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrashController.class)
@Import({GlobalExceptionHandler.class, TrashControllerTest.MethodSecurityConfig.class})
class TrashControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetTrashUseCase getTrashUseCase;

    @MockitoBean
    private RestoreFromTrashUseCase restoreFromTrashUseCase;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        CurrentUser currentUser = CurrentUser.builder()
                .keycloakSub("sub-trash")
                .username("trashuser")
                .email("trashuser@awb.ma")
                .build();

        given(currentUserProvider.getRequiredCurrentUser()).willReturn(currentUser);
        given(userRepositoryPort.findByKeycloakSub("sub-trash"))
                .willReturn(Optional.of(User.builder().id(userId).keycloakSub("sub-trash").build()));
    }

    @Test
    @WithMockUser(authorities = "TRASH_READ")
    @DisplayName("GET /api/v1/trash - Should return 200 OK with paginated trash items")
    void getTrash_Success() throws Exception {
        // Given
        TrashItemResponseDto item = TrashItemResponseDto.builder()
                .id(UUID.randomUUID())
                .entityType("DOCUMENT")
                .entityId(UUID.randomUUID())
                .deletedBy(userId)
                .deletedAt(Instant.now())
                .build();

        PageResponse<TrashItemResponseDto> pageResponse = PageResponse.<TrashItemResponseDto>builder()
                .content(List.of(item))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        given(getTrashUseCase.getTrash(any(), anyBoolean(), anyInt(), anyInt())).willReturn(pageResponse);

        // When / Then
        mockMvc.perform(get("/api/v1/trash")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].entityType").value("DOCUMENT"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(authorities = "TRASH_RESTORE")
    @DisplayName("POST /api/v1/trash/{id}/restore - Should restore item and return 204 No Content")
    void restoreFromTrash_Success() throws Exception {
        // Given
        UUID trashId = UUID.randomUUID();
        doNothing().when(restoreFromTrashUseCase).restoreFromTrash(trashId, userId);

        // When / Then
        mockMvc.perform(post("/api/v1/trash/{id}/restore", trashId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "TRASH_RESTORE")
    @DisplayName("POST /api/v1/trash/{id}/restore - Should return 404 Not Found when trash item missing")
    void restoreFromTrash_NotFound_Returns404() throws Exception {
        // Given
        UUID trashId = UUID.randomUUID();
        doThrow(new NotFoundException(ErrorCode.INVALID_INPUT, "Trash not found"))
                .when(restoreFromTrashUseCase).restoreFromTrash(trashId, userId);

        // When / Then
        mockMvc.perform(post("/api/v1/trash/{id}/restore", trashId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR-SYS-002"));
    }
}
