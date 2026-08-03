package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.TrashRepositoryPort;
import com.awb.ged.domain.trash.model.TrashItem;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.trash.TrashJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.FolderJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.TrashJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class TrashRepositoryAdapter implements TrashRepositoryPort {

    private final TrashJpaRepository trashJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final FolderJpaRepository folderJpaRepository;

    @Autowired
    public TrashRepositoryAdapter(TrashJpaRepository trashJpaRepository,
                                  UserJpaRepository userJpaRepository,
                                  FolderJpaRepository folderJpaRepository) {
        this.trashJpaRepository = trashJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.folderJpaRepository = folderJpaRepository;
    }

    @Override
    public TrashItem save(TrashItem item) {
        TrashJpaEntity entity = mapToEntity(item);
        TrashJpaEntity saved = trashJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrashItem> findById(UUID id) {
        return trashJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrashItem> findByDeletedByAndPurgedAtIsNull(UUID userId) {
        return trashJpaRepository.findByDeletedByIdAndPurgedAtIsNull(userId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrashItem> findAllActive() {
        return trashJpaRepository.findByPurgedAtIsNull().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        trashJpaRepository.deleteById(id);
    }

    private TrashItem mapToDomain(TrashJpaEntity entity) {
        if (entity == null) return null;
        return TrashItem.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType().name())
                .entityId(entity.getEntityId())
                .originalFolderId(entity.getOriginalFolder() != null ? entity.getOriginalFolder().getId() : null)
                .deletedBy(entity.getDeletedBy() != null ? entity.getDeletedBy().getId() : null)
                .deletedAt(entity.getDeletedAt())
                .autoPurgeAt(entity.getAutoPurgeAt())
                .purgedAt(entity.getPurgedAt())
                .build();
    }

    private TrashJpaEntity mapToEntity(TrashItem domain) {
        if (domain == null) return null;

        UserJpaEntity deletedBy = null;
        if (domain.getDeletedBy() != null) {
            deletedBy = userJpaRepository.findById(domain.getDeletedBy()).orElse(null);
        }

        FolderJpaEntity originalFolder = null;
        if (domain.getOriginalFolderId() != null) {
            originalFolder = folderJpaRepository.findById(domain.getOriginalFolderId()).orElse(null);
        }

        TrashJpaEntity.EntityType typeEnum = TrashJpaEntity.EntityType.valueOf(domain.getEntityType().toUpperCase());

        TrashJpaEntity entity = TrashJpaEntity.builder()
                .entityType(typeEnum)
                .entityId(domain.getEntityId())
                .originalFolder(originalFolder)
                .deletedBy(deletedBy)
                .deletedAt(domain.getDeletedAt())
                .autoPurgeAt(domain.getAutoPurgeAt())
                .purgedAt(domain.getPurgedAt())
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
