package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolderJpaRepository extends JpaRepository<FolderJpaEntity, UUID> {

    List<FolderJpaEntity> findByParentFolderId(UUID parentId);

    List<FolderJpaEntity> findByParentFolderIsNull();

    List<FolderJpaEntity> findByParentFolderIdAndDeletedAtIsNull(UUID parentId);

    List<FolderJpaEntity> findByParentFolderIsNullAndDeletedAtIsNull();

    long countByDeletedAtIsNull();
}

