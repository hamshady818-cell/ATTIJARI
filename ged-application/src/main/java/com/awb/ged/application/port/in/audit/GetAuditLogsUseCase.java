package com.awb.ged.application.port.in.audit;

import com.awb.ged.application.dto.audit.AuditLogResponseDto;
import com.awb.ged.common.model.PageResponse;

import java.util.UUID;

public interface GetAuditLogsUseCase {

    PageResponse<AuditLogResponseDto> getAuditLogs(String entityType, UUID entityId, int page, int size);
}
