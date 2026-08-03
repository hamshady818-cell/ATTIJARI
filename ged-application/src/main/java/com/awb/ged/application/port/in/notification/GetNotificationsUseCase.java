package com.awb.ged.application.port.in.notification;

import com.awb.ged.application.dto.notification.NotificationResponseDto;

import java.util.List;
import java.util.UUID;

public interface GetNotificationsUseCase {
    List<NotificationResponseDto> getNotifications(UUID userId, int page, int size);
}
