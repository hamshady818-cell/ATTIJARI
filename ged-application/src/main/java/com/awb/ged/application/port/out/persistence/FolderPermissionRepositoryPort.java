package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.folder.model.FolderPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderPermissionRepositoryPort {

    FolderPermission save(FolderPermission permission);

    Optional<FolderPermission> findById(UUID id);

    List<FolderPermission> findByFolderId(UUID folderId);

    void delete(UUID id);
}
