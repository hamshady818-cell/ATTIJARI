package com.awb.ged.application.service.notification;

import com.awb.ged.application.port.in.notification.MarkNotificationReadUseCase;
import com.awb.ged.application.port.out.persistence.NotificationRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.notification.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MarkNotificationReadService implements MarkNotificationReadUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;

    @Autowired
    public MarkNotificationReadService(NotificationRepositoryPort notificationRepositoryPort) {
        this.notificationRepositoryPort = notificationRepositoryPort;
    }

    @Override
    public void markAsRead(UUID notificationId, UUID userId) {
        // 1. Retrieve notification
        Notification notification = notificationRepositoryPort.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Notification with ID " + notificationId + " was not found."
                ));

        // 2. Security validation: Ensure user owns this notification
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN,
                    "You are not authorized to modify another user's notification."
            );
        }

        // 3. Mark as read
        notification.setStatus("READ");
        notification.setReadAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        // 4. Save
        notificationRepositoryPort.save(notification);
    }
}
