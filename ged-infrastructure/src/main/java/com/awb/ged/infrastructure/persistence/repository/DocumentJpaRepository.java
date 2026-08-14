package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    Page<DocumentJpaEntity> findByStatus(DocumentJpaEntity.DocumentStatus status, Pageable pageable);

    @Query("SELECT d FROM DocumentJpaEntity d WHERE d.deleted = true OR d.deletedAt IS NOT NULL OR d.status = com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity.DocumentStatus.TRASHED")
    Page<DocumentJpaEntity> findTrashedDocuments(Pageable pageable);

    @Query("SELECT d FROM DocumentJpaEntity d WHERE d.status = 'TRASHED' AND ((d.deletedBy IS NOT NULL AND d.deletedBy.id = :userId) OR (d.owner IS NOT NULL AND d.owner.id = :userId))")
    Page<DocumentJpaEntity> findTrashedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(d) FROM DocumentJpaEntity d WHERE d.deletedAt IS NULL")
    long countNonDeleted();

    @Query("SELECT COALESCE(SUM(v.fileSizeBytes), 0) FROM DocumentVersionJpaEntity v")
    long sumAllVersionSizeBytes();

    @Query("SELECT d FROM DocumentJpaEntity d WHERE d.deletedAt IS NULL ORDER BY d.createdAt DESC")
    List<DocumentJpaEntity> findRecentUploads(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT d FROM DocumentJpaEntity d WHERE d.deletedAt IS NULL ORDER BY d.updatedAt DESC")
    List<DocumentJpaEntity> findRecentlyModified(org.springframework.data.domain.Pageable pageable);

    /**
     * Returns IDs of non-deleted documents in the given folder.
     */
    @Query("SELECT d.id FROM DocumentJpaEntity d WHERE d.folder.id = :folderId AND d.deletedAt IS NULL")
    List<UUID> findActiveIdsByFolderId(@Param("folderId") UUID folderId);

    /**
     * Bulk soft-delete documents by their IDs without loading them into the persistence context.
     */
    @Modifying
    @Query("UPDATE DocumentJpaEntity d SET d.deletedAt = :deletedAt, d.status = 'TRASHED' WHERE d.id IN :ids AND d.deletedAt IS NULL")
    int bulkSoftDelete(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}
