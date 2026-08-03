package com.awb.ged.application.service.notification;

import com.awb.ged.application.dto.notification.NotificationResponseDto;
import com.awb.ged.application.mapper.NotificationMapper;
import com.awb.ged.application.port.out.persistence.NotificationRepositoryPort;
import com.awb.ged.domain.notification.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetNotificationsServiceTest {

    @Mock
    private NotificationRepositoryPort notificationRepositoryPort;

    private final NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);

    private GetNotificationsService getNotificationsService;

    @BeforeEach
    void setUp() {
        getNotificationsService = new GetNotificationsService(notificationRepositoryPort, notificationMapper);
    }

    @Test
    @DisplayName("Should successfully retrieve user notifications list")
    void getNotifications_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        Notification n1 = Notification.builder().id(UUID.randomUUID()).userId(userId).type("DOC_CREATED").title("New Document").body("A new doc was created").channel(Notification.Channel.IN_APP).status("PENDING").build();
        Notification n2 = Notification.builder().id(UUID.randomUUID()).userId(userId).type("DOC_SHARED").title("Document Shared").body("A doc was shared with you").channel(Notification.Channel.IN_APP).status("PENDING").build();

        given(notificationRepositoryPort.findByUserId(userId, 0, 10)).willReturn(List.of(n1, n2));

        // When
        List<NotificationResponseDto> result = getNotificationsService.getNotifications(userId, 0, 10);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo("DOC_CREATED");
        assertThat(result.get(1).getType()).isEqualTo("DOC_SHARED");
    }
}
