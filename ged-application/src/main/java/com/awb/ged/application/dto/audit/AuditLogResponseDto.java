package com.awb.ged.application.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDto {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String action;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
    private Instant occurredAt;
}
