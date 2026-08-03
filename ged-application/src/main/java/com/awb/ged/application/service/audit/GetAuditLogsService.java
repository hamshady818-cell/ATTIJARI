package com.awb.ged.application.service.audit;

import com.awb.ged.application.dto.audit.AuditLogResponseDto;
import com.awb.ged.application.port.in.audit.GetAuditLogsUseCase;
import com.awb.ged.application.port.out.persistence.AuditLogRepositoryPort;
import com.awb.ged.common.model.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAuditLogsService implements GetAuditLogsUseCase {

    private final AuditLogRepositoryPort auditLogRepositoryPort;

    public GetAuditLogsService(AuditLogRepositoryPort auditLogRepositoryPort) {
        this.auditLogRepositoryPort = auditLogRepositoryPort;
    }

    @Override
    public PageResponse<AuditLogResponseDto> getAuditLogs(String entityType, UUID entityId, int page, int size) {
        return auditLogRepositoryPort.findAuditLogs(entityType, entityId, page, size);
    }
}
