package com.awb.ged.application.port.in.notification;

import java.util.UUID;

/**
 * <h1>MarkAllNotificationsReadUseCase</h1>
 * <p>
 * Input port for marking all unread notifications of the current user as read in bulk.
 * </p>
 */
public interface MarkAllNotificationsReadUseCase {

    /**
     * Marks all PENDING and SENT notifications belonging to {@code userId} as READ.
     *
     * @param userId the authenticated user's ID
     */
    void markAllAsRead(UUID userId);
}
