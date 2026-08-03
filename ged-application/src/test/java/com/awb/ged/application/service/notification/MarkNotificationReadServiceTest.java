package com.awb.ged.application.service.notification;

import com.awb.ged.application.port.out.persistence.NotificationRepositoryPort;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.notification.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarkNotificationReadServiceTest {

    @Mock
    private NotificationRepositoryPort notificationRepositoryPort;

    private MarkNotificationReadService markNotificationReadService;

    @BeforeEach
    void setUp() {
        markNotificationReadService = new MarkNotificationReadService(notificationRepositoryPort);
    }

    @Test
    @DisplayName("Should successfully mark notification as read when it belongs to the user")
    void markAsRead_Success() {
        // Given
        UUID notifId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notifId)
                .userId(userId)
                .status("PENDING")
                .build();

        given(notificationRepositoryPort.findById(notifId)).willReturn(Optional.of(notification));

        // When
        markNotificationReadService.markAsRead(notifId, userId);

        // Then
        assertThat(notification.getStatus()).isEqualTo("READ");
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepositoryPort).save(notification);
    }

    @Test
    @DisplayName("Should throw NotFoundException when notification does not exist")
    void markAsRead_NotFound_ThrowsNotFoundException() {
        // Given
        UUID notifId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(notificationRepositoryPort.findById(notifId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> markNotificationReadService.markAsRead(notifId, userId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ForbiddenException when notification belongs to another user")
    void markAsRead_NotOwned_ThrowsForbiddenException() {
        // Given
        UUID notifId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notifId)
                .userId(anotherUserId)
                .status("PENDING")
                .build();

        given(notificationRepositoryPort.findById(notifId)).willReturn(Optional.of(notification));

        // When / Then
        assertThatThrownBy(() -> markNotificationReadService.markAsRead(notifId, userId))
                .isInstanceOf(ForbiddenException.class);
    }
}
