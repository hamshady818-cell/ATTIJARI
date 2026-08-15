package com.awb.ged.api.notification;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.application.dto.notification.NotificationResponseDto;
import com.awb.ged.application.port.in.notification.GetNotificationsUseCase;
import com.awb.ged.application.port.in.notification.MarkAllNotificationsReadUseCase;
import com.awb.ged.application.port.in.notification.MarkNotificationReadUseCase;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.ForbiddenException;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({GlobalExceptionHandler.class, NotificationControllerTest.MethodSecurityConfig.class})
class NotificationControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetNotificationsUseCase getNotificationsUseCase;

    @MockitoBean
    private MarkNotificationReadUseCase markNotificationReadUseCase;

    @MockitoBean
    private MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        CurrentUser currentUser = CurrentUser.builder()
                .keycloakSub("sub-notif")
                .username("notifuser")
                .email("notifuser@awb.ma")
                .build();

        given(currentUserProvider.getRequiredCurrentUser()).willReturn(currentUser);
        given(userRepositoryPort.findByKeycloakSub("sub-notif"))
                .willReturn(Optional.of(User.builder().id(userId).keycloakSub("sub-notif").build()));
    }

    @Test
    @WithMockUser(authorities = "NOTIFICATION_READ")
    @DisplayName("GET /api/v1/notifications - Should return 200 OK with notifications list")
    void getNotifications_Success() throws Exception {
        // Given
        NotificationResponseDto item = NotificationResponseDto.builder()
                .id(UUID.randomUUID())
                .type("DOC_CREATED")
                .title("New Document")
                .body("A document was uploaded")
                .channel("IN_APP")
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        given(getNotificationsUseCase.getNotifications(any(), anyInt(), anyInt())).willReturn(List.of(item));

        // When / Then
        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("DOC_CREATED"));
    }

    @Test
    @WithMockUser(authorities = "NOTIFICATION_WRITE")
    @DisplayName("PATCH /api/v1/notifications/{id}/read - Should mark notification as read and return 204 No Content")
    void markAsRead_Success() throws Exception {
        // Given
        UUID notifId = UUID.randomUUID();
        doNothing().when(markNotificationReadUseCase).markAsRead(notifId, userId);

        // When / Then
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notifId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "NOTIFICATION_WRITE")
    @DisplayName("PATCH /api/v1/notifications/{id}/read - Should return 404 Not Found when notification missing")
    void markAsRead_NotFound_Returns404() throws Exception {
        // Given
        UUID notifId = UUID.randomUUID();
        doThrow(new NotFoundException(ErrorCode.INVALID_INPUT, "Not found"))
                .when(markNotificationReadUseCase).markAsRead(notifId, userId);

        // When / Then
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notifId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR-SYS-002"));
    }

    @Test
    @WithMockUser(authorities = "NOTIFICATION_WRITE")
    @DisplayName("PATCH /api/v1/notifications/{id}/read - Should return 403 Forbidden when not owned")
    void markAsRead_NotOwned_Returns403() throws Exception {
        // Given
        UUID notifId = UUID.randomUUID();
        doThrow(new ForbiddenException(ErrorCode.FORBIDDEN, "Not owned"))
                .when(markNotificationReadUseCase).markAsRead(notifId, userId);

        // When / Then
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notifId)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-SEC-002"));
    }
}
