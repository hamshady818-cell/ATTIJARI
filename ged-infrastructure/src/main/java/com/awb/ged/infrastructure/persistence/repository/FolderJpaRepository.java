package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FolderJpaRepository extends JpaRepository<FolderJpaEntity, UUID> {

    List<FolderJpaEntity> findByParentFolderId(UUID parentId);

    List<FolderJpaEntity> findByParentFolderIsNull();

    List<FolderJpaEntity> findByParentFolderIdAndDeletedAtIsNull(UUID parentId);

    List<FolderJpaEntity> findByParentFolderIsNullAndDeletedAtIsNull();

    List<FolderJpaEntity> findByDeletedAtIsNull();

    long countByDeletedAtIsNull();

    /**
     * Bulk soft-delete folders by their IDs without loading them into the persistence context.
     */
    @Modifying
    @Query("UPDATE FolderJpaEntity f SET f.deletedAt = :deletedAt, f.deleted = true WHERE f.id IN :ids AND f.deletedAt IS NULL")
    int bulkSoftDelete(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);

    /**
     * Returns all non-deleted folder IDs that are children of the given parent.
     */
    @Query("SELECT f.id FROM FolderJpaEntity f WHERE f.parentFolder.id = :parentId AND f.deletedAt IS NULL")
    List<UUID> findActiveChildIds(@Param("parentId") UUID parentId);
}
