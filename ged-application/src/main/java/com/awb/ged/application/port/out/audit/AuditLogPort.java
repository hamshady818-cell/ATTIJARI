package com.awb.ged.application.port.out.audit;

import java.util.Map;
import java.util.UUID;

public interface AuditLogPort {

    void record(String action, String entityType, UUID entityId, String entityName,
                UUID userId, Map<String, Object> metadata);
}
