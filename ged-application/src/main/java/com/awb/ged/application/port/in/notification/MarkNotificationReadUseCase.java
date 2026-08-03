package com.awb.ged.application.port.in.notification;

import java.util.UUID;

public interface MarkNotificationReadUseCase {
    void markAsRead(UUID notificationId, UUID userId);
}
