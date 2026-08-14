package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.notification.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findByUserId(UUID userId, int page, int size);

    /**
     * Returns all notifications for the given user that have not yet been read
     * (status PENDING or SENT). Used by the bulk mark-all-read operation.
     *
     * @param userId the recipient user's ID
     * @return unread notifications, unordered
     */
    List<Notification> findUnreadByUserId(UUID userId);
}
