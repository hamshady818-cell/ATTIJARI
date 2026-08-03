package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.folder.FolderPermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolderPermissionJpaRepository extends JpaRepository<FolderPermissionJpaEntity, UUID> {

    List<FolderPermissionJpaEntity> findByFolderId(UUID folderId);
}
