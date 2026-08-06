package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.domain.folder.model.Folder;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.FolderJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class FolderRepositoryAdapter implements FolderRepositoryPort {

    private final FolderJpaRepository folderJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public FolderRepositoryAdapter(FolderJpaRepository folderJpaRepository,
            UserJpaRepository userJpaRepository) {
        this.folderJpaRepository = folderJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Folder save(Folder folder) {
        FolderJpaEntity entity = mapToEntity(folder);
        FolderJpaEntity saved = folderJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folder> findById(UUID id) {
        return folderJpaRepository.findById(id)
                .filter(entity -> !entity.isDeleted())
                .map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folder> findByIdIncludingDeleted(UUID id) {
        return folderJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> findByParentId(UUID parentId) {
        List<FolderJpaEntity> list = (parentId == null)
                ? folderJpaRepository.findByParentFolderIsNullAndDeletedAtIsNull()
                : folderJpaRepository.findByParentFolderIdAndDeletedAtIsNull(parentId);
        return list.stream().map(this::mapToDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> findAll() {
        return folderJpaRepository.findByDeletedAtIsNull().stream().map(this::mapToDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        folderJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAllFolders() {
        return folderJpaRepository.countByDeletedAtIsNull();
    }

    private Folder mapToDomain(FolderJpaEntity entity) {
        if (entity == null)
            return null;
        return Folder.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParentFolder() != null ? entity.getParentFolder().getId() : null)
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .createdAt(entity.getCreatedAt())
                .deletedAt(entity.getDeletedAt())
                .deletedBy(entity.getDeletedBy() != null ? entity.getDeletedBy().getId() : null)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private FolderJpaEntity mapToEntity(Folder domain) {
        if (domain == null)
            return null;

        FolderJpaEntity parent = null;
        if (domain.getParentId() != null) {
            parent = folderJpaRepository.findById(domain.getParentId()).orElse(null);
        }

        UserJpaEntity owner = null;
        if (domain.getOwnerId() != null) {
            owner = userJpaRepository.findById(domain.getOwnerId()).orElse(null);
        }

        String path = (parent != null)
                ? parent.getPath() + "."
                        + (domain.getId() != null ? domain.getId().toString().replace("-", "_") : "new")
                : (domain.getId() != null ? domain.getId().toString().replace("-", "_") : "root");

        UserJpaEntity deleter = null;
        if (domain.getDeletedBy() != null) {
            deleter = userJpaRepository.findById(domain.getDeletedBy()).orElse(null);
        }

        FolderJpaEntity entity = FolderJpaEntity.builder()
                .name(domain.getName())
                .parentFolder(parent)
                .owner(owner)
                .createdBy(owner)
                .updatedBy(owner)
                .deleted(domain.isDeleted())
                .deletedAt(domain.getDeletedAt())
                .deletedBy(deleter)
                .path(path)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
