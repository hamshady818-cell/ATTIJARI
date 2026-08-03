package com.awb.ged.application.port.out.persistence;

import com.awb.ged.application.dto.audit.AuditLogResponseDto;
import com.awb.ged.common.model.PageResponse;

import java.util.UUID;

public interface AuditLogRepositoryPort {

    PageResponse<AuditLogResponseDto> findAuditLogs(String entityType, UUID entityId, int page, int size);
}
