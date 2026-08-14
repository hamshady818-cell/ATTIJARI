package com.awb.ged.application.service.notification;

import com.awb.ged.application.port.in.notification.MarkAllNotificationsReadUseCase;
import com.awb.ged.application.port.out.persistence.NotificationRepositoryPort;
import com.awb.ged.domain.notification.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * <h1>MarkAllNotificationsReadService</h1>
 * <p>
 * Application service that marks all unread notifications (PENDING or SENT)
 * belonging to the current user as READ in a single transaction.
 * </p>
 */
@Service
@Transactional
public class MarkAllNotificationsReadService implements MarkAllNotificationsReadUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;

    @Autowired
    public MarkAllNotificationsReadService(NotificationRepositoryPort notificationRepositoryPort) {
        this.notificationRepositoryPort = notificationRepositoryPort;
    }

    @Override
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepositoryPort.findUnreadByUserId(userId);
        Instant now = Instant.now();

        for (Notification notification : unread) {
            notification.setStatus("READ");
            notification.setReadAt(now);
            notification.setUpdatedAt(now);
            notificationRepositoryPort.save(notification);
        }
    }
}
