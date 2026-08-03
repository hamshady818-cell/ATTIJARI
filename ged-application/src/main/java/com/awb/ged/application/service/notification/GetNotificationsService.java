package com.awb.ged.application.service.notification;

import com.awb.ged.application.dto.notification.NotificationResponseDto;
import com.awb.ged.application.mapper.NotificationMapper;
import com.awb.ged.application.port.in.notification.GetNotificationsUseCase;
import com.awb.ged.application.port.out.persistence.NotificationRepositoryPort;
import com.awb.ged.domain.notification.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetNotificationsService implements GetNotificationsUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationMapper notificationMapper;

    @Autowired
    public GetNotificationsService(NotificationRepositoryPort notificationRepositoryPort,
                                  NotificationMapper notificationMapper) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public List<NotificationResponseDto> getNotifications(UUID userId, int page, int size) {
        List<Notification> notifications = notificationRepositoryPort.findByUserId(userId, page, size);
        return notifications.stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }
}
