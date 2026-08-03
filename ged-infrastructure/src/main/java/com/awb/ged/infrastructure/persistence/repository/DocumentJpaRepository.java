package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, UUID> {

    List<DocumentJpaEntity> findByFolderId(UUID folderId);

    List<DocumentJpaEntity> findByFolderIsNull();

    List<DocumentJpaEntity> findByFolderIdAndDeletedAtIsNull(UUID folderId);

    List<DocumentJpaEntity> findByFolderIsNullAndDeletedAtIsNull();
}
