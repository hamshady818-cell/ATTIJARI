package com.awb.ged.application.service.permission;

import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.mapper.FolderPermissionMapper;
import com.awb.ged.application.port.in.folder.GrantFolderPermissionUseCase;
import com.awb.ged.application.port.in.folder.ListFolderPermissionsUseCase;
import com.awb.ged.application.port.in.folder.RevokeFolderPermissionUseCase;
import com.awb.ged.application.port.out.persistence.FolderPermissionRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.folder.model.Folder;
import com.awb.ged.domain.folder.model.FolderPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FolderPermissionService implements GrantFolderPermissionUseCase, RevokeFolderPermissionUseCase, ListFolderPermissionsUseCase {

    private final FolderRepositoryPort folderRepositoryPort;
    private final FolderPermissionRepositoryPort folderPermissionRepositoryPort;
    private final FolderPermissionMapper folderPermissionMapper;

    @Autowired
    public FolderPermissionService(FolderRepositoryPort folderRepositoryPort,
                                   FolderPermissionRepositoryPort folderPermissionRepositoryPort,
                                   FolderPermissionMapper folderPermissionMapper) {
        this.folderRepositoryPort = folderRepositoryPort;
        this.folderPermissionRepositoryPort = folderPermissionRepositoryPort;
        this.folderPermissionMapper = folderPermissionMapper;
    }

    @Override
    public PermissionResponseDto grantPermission(GrantPermissionCommand command, boolean isAdminOrManager) {
        // 1. Find folder
        Folder folder = folderRepositoryPort.findById(command.getTargetId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.FOLDER_NOT_FOUND,
                        "Folder with ID " + command.getTargetId() + " was not found."
                ));

        // 2. Validate authority (Owner or Admin/Manager)
        if (!isAdminOrManager && !folder.getOwnerId().equals(command.getGrantedBy())) {
            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN,
                    "You are not authorized to manage permissions for this folder."
            );
        }

        // 3. Create permission
        FolderPermission permission = FolderPermission.builder()
                .id(UUID.randomUUID())
                .folderId(command.getTargetId())
                .userId(command.getUserId())
                .groupId(command.getGroupId())
                .canRead(command.isCanRead())
                .canWrite(command.isCanWrite())
                .canDelete(command.isCanDelete())
                .canManage(command.isCanShareOrManage())
                .inherited(false) // explicitly set, not inherited
                .grantedBy(command.getGrantedBy())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        FolderPermission saved = folderPermissionRepositoryPort.save(permission);
        return folderPermissionMapper.toResponseDto(saved);
    }

    @Override
    public void revokePermission(UUID folderId, UUID permissionId, UUID userId, boolean isAdminOrManager) {
        // 1. Find folder
        Folder folder = folderRepositoryPort.findById(folderId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.FOLDER_NOT_FOUND,
                        "Folder with ID " + folderId + " was not found."
                ));

        // 2. Validate authority
        if (!isAdminOrManager && !folder.getOwnerId().equals(userId)) {
            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN,
                    "You are not authorized to manage permissions for this folder."
            );
        }

        // 3. Find permission
        FolderPermission permission = folderPermissionRepositoryPort.findById(permissionId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Permission with ID " + permissionId + " was not found."
                ));

        // 4. Validate context target matches
        if (!permission.getFolderId().equals(folderId)) {
            throw new IllegalArgumentException("Permission does not belong to this folder.");
        }

        // 5. Delete
        folderPermissionRepositoryPort.delete(permissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponseDto> listPermissions(UUID folderId, UUID userId, boolean isAdminOrManager) {
        // 1. Find folder
        Folder folder = folderRepositoryPort.findById(folderId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.FOLDER_NOT_FOUND,
                        "Folder with ID " + folderId + " was not found."
                ));

        // 2. Validate authority
        if (!isAdminOrManager && !folder.getOwnerId().equals(userId)) {
            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN,
                    "You are not authorized to view permissions for this folder."
            );
        }

        // 3. List and map
        List<FolderPermission> list = folderPermissionRepositoryPort.findByFolderId(folderId);
        return list.stream()
                .map(folderPermissionMapper::toResponseDto)
                .toList();
    }
}
