package com.awb.ged.application.service.permission;

import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.mapper.DocumentPermissionMapper;
import com.awb.ged.application.port.in.document.GrantDocumentPermissionUseCase;
import com.awb.ged.application.port.in.document.ListDocumentPermissionsUseCase;
import com.awb.ged.application.port.in.document.RevokeDocumentPermissionUseCase;
import com.awb.ged.application.port.out.persistence.DocumentPermissionRepositoryPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentPermissionService implements GrantDocumentPermissionUseCase, RevokeDocumentPermissionUseCase, ListDocumentPermissionsUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final DocumentPermissionRepositoryPort documentPermissionRepositoryPort;
    private final DocumentPermissionMapper documentPermissionMapper;

    @Autowired
    public DocumentPermissionService(DocumentRepositoryPort documentRepositoryPort,
                                     DocumentPermissionRepositoryPort documentPermissionRepositoryPort,
                                     DocumentPermissionMapper documentPermissionMapper) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.documentPermissionRepositoryPort = documentPermissionRepositoryPort;
        this.documentPermissionMapper = documentPermissionMapper;
    }

    @Override
    public PermissionResponseDto grantPermission(GrantPermissionCommand command, boolean isAdminOrManager) {
        // 1. Find document
        Document document = documentRepositoryPort.findById(command.getTargetId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + command.getTargetId() + " was not found."
                ));

        // 2. Validate authority (Owner or Admin/Manager)
        if (!isAdminOrManager && !document.getOwnerId().equals(command.getGrantedBy())) {
            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN,
                    "You are not authorized to manage permissions for this document."
            );
        }

        // 3. Create permission
        DocumentPermission permission = DocumentPermission.builder()
                .id(UUID.randomUUID())
                .documentId(command.getTargetId())
                .userId(command.getUserId())
                .groupId(command.getGroupId())
                .canRead(command.isCanRead())
                .canWrite(command.isCanWrite())
                .canDelete(command.isCanDelete())
                .canShare(command.isCanShareOrManage())
                .grantedBy(command.getGrantedBy())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        DocumentPermission saved = documentPermissionRepositoryPort.save(permission);
        return documentPermissionMapper.toResponseDto(saved);
    }

    @Override
    public void revokePermission(UUID documentId, UUID permissionId, UUID userId, boolean isAdminOrManager) {
        // 1. Find document
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        // 2. Validate authority
        if (!isAdminOrManager && !document.getOwnerId().equals(userId)) {
            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN,
                    "You are not authorized to manage permissions for this document."
            );
        }

        // 3. Find permission
        DocumentPermission permission = documentPermissionRepositoryPort.findById(permissionId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Permission with ID " + permissionId + " was not found."
                ));

        // 4. Validate context target matches
        if (!permission.getDocumentId().equals(documentId)) {
            throw new IllegalArgumentException("Permission does not belong to this document.");
        }

        // 5. Delete
        documentPermissionRepositoryPort.delete(permissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponseDto> listPermissions(UUID documentId, UUID userId, boolean isAdminOrManager) {
        // 1. Find document
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        // 2. Validate authority (Owner or Admin/Manager can view permissions)
        if (!isAdminOrManager && !document.getOwnerId().equals(userId)) {
            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN,
                    "You are not authorized to view permissions for this document."
            );
        }

        // 3. List and map
        List<DocumentPermission> list = documentPermissionRepositoryPort.findByDocumentId(documentId);
        return list.stream()
                .map(documentPermissionMapper::toResponseDto)
                .toList();
    }
}
