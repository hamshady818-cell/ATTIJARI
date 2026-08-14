package com.awb.ged.application.service.notification;

import com.awb.ged.application.port.out.persistence.NotificationRepositoryPort;
import com.awb.ged.domain.notification.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkAllNotificationsReadServiceTest {

    @Mock
    private NotificationRepositoryPort notificationRepositoryPort;

    private MarkAllNotificationsReadService service;

    @BeforeEach
    void setUp() {
        service = new MarkAllNotificationsReadService(notificationRepositoryPort);
    }

    @Test
    @DisplayName("Should mark all unread notifications as READ and save each one")
    void markAllAsRead_WithUnreadNotifications_MarksAllAsRead() {
        // Given
        UUID userId = UUID.randomUUID();
        Notification n1 = Notification.builder().id(UUID.randomUUID()).userId(userId).status("PENDING").build();
        Notification n2 = Notification.builder().id(UUID.randomUUID()).userId(userId).status("SENT").build();
        Notification n3 = Notification.builder().id(UUID.randomUUID()).userId(userId).status("PENDING").build();

        given(notificationRepositoryPort.findUnreadByUserId(userId)).willReturn(List.of(n1, n2, n3));

        // When
        service.markAllAsRead(userId);

        // Then — each notification must have status READ, readAt set, and be saved
        assertThat(n1.getStatus()).isEqualTo("READ");
        assertThat(n1.getReadAt()).isNotNull();
        assertThat(n2.getStatus()).isEqualTo("READ");
        assertThat(n2.getReadAt()).isNotNull();
        assertThat(n3.getStatus()).isEqualTo("READ");
        assertThat(n3.getReadAt()).isNotNull();

        verify(notificationRepositoryPort, times(3)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should do nothing and not call save when there are no unread notifications")
    void markAllAsRead_WithNoUnreadNotifications_DoesNothing() {
        // Given
        UUID userId = UUID.randomUUID();
        given(notificationRepositoryPort.findUnreadByUserId(userId)).willReturn(Collections.emptyList());

        // When
        service.markAllAsRead(userId);

        // Then
        verify(notificationRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should use the same readAt timestamp for all notifications in the batch")
    void markAllAsRead_UsesConsistentTimestamp() {
        // Given
        UUID userId = UUID.randomUUID();
        Notification n1 = Notification.builder().id(UUID.randomUUID()).userId(userId).status("PENDING").build();
        Notification n2 = Notification.builder().id(UUID.randomUUID()).userId(userId).status("SENT").build();
        given(notificationRepositoryPort.findUnreadByUserId(userId)).willReturn(List.of(n1, n2));

        // When
        service.markAllAsRead(userId);

        // Then — both readAt should be equal (same Instant captured in the service)
        assertThat(n1.getReadAt()).isEqualTo(n2.getReadAt());
    }
}
