package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentJpaRepository
        extends JpaRepository<DocumentJpaEntity, UUID>,
                JpaSpecificationExecutor<DocumentJpaEntity> {

    List<DocumentJpaEntity> findByFolderId(UUID folderId);

    List<DocumentJpaEntity> findByFolderIsNull();

    List<DocumentJpaEntity> findByFolderIdAndDeletedAtIsNull(UUID folderId);

    List<DocumentJpaEntity> findByFolderIsNullAndDeletedAtIsNull();

    @Query("SELECT COUNT(d) FROM DocumentJpaEntity d WHERE d.deletedAt IS NULL")
    long countNonDeleted();

    @Query("SELECT COALESCE(SUM(v.fileSizeBytes), 0) FROM DocumentVersionJpaEntity v")
    long sumAllVersionSizeBytes();

    @Query("SELECT d FROM DocumentJpaEntity d WHERE d.deletedAt IS NULL ORDER BY d.createdAt DESC")
    List<DocumentJpaEntity> findRecentUploads(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT d FROM DocumentJpaEntity d WHERE d.deletedAt IS NULL ORDER BY d.updatedAt DESC")
    List<DocumentJpaEntity> findRecentlyModified(org.springframework.data.domain.Pageable pageable);
}
