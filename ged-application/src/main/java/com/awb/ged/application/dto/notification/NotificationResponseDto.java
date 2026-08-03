package com.awb.ged.application.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private UUID id;
    private String type;
    private String title;
    private String body;
    private String entityType;
    private UUID entityId;
    private String channel;
    private String status;
    private Instant readAt;
    private Instant sentAt;
    private Instant createdAt;
}
