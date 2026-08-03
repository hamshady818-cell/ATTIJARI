package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.FolderPermissionRepositoryPort;
import com.awb.ged.domain.folder.model.FolderPermission;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderPermissionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.GroupJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.FolderJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.FolderPermissionJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class FolderPermissionRepositoryAdapter implements FolderPermissionRepositoryPort {

    private final FolderPermissionJpaRepository folderPermissionJpaRepository;
    private final FolderJpaRepository folderJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final EntityManager entityManager;

    @Autowired
    public FolderPermissionRepositoryAdapter(FolderPermissionJpaRepository folderPermissionJpaRepository,
                                             FolderJpaRepository folderJpaRepository,
                                             UserJpaRepository userJpaRepository,
                                             EntityManager entityManager) {
        this.folderPermissionJpaRepository = folderPermissionJpaRepository;
        this.folderJpaRepository = folderJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public FolderPermission save(FolderPermission permission) {
        FolderPermissionJpaEntity entity = mapToEntity(permission);
        FolderPermissionJpaEntity saved = folderPermissionJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FolderPermission> findById(UUID id) {
        return folderPermissionJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FolderPermission> findByFolderId(UUID folderId) {
        return folderPermissionJpaRepository.findByFolderId(folderId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        folderPermissionJpaRepository.deleteById(id);
    }

    private FolderPermission mapToDomain(FolderPermissionJpaEntity entity) {
        if (entity == null) return null;
        return FolderPermission.builder()
                .id(entity.getId())
                .folderId(entity.getFolder() != null ? entity.getFolder().getId() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .groupId(entity.getGroup() != null ? entity.getGroup().getId() : null)
                .canRead(entity.isCanRead())
                .canWrite(entity.isCanWrite())
                .canDelete(entity.isCanDelete())
                .canManage(entity.isCanManage())
                .inherited(entity.isInherited())
                .grantedBy(entity.getGrantedBy() != null ? entity.getGrantedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private FolderPermissionJpaEntity mapToEntity(FolderPermission domain) {
        if (domain == null) return null;

        FolderJpaEntity folder = null;
        if (domain.getFolderId() != null) {
            folder = folderJpaRepository.findById(domain.getFolderId()).orElse(null);
        }

        UserJpaEntity user = null;
        if (domain.getUserId() != null) {
            user = userJpaRepository.findById(domain.getUserId()).orElse(null);
        }

        GroupJpaEntity group = null;
        if (domain.getGroupId() != null) {
            group = entityManager.find(GroupJpaEntity.class, domain.getGroupId());
        }

        UserJpaEntity grantedBy = null;
        if (domain.getGrantedBy() != null) {
            grantedBy = userJpaRepository.findById(domain.getGrantedBy()).orElse(null);
        }

        FolderPermissionJpaEntity entity = FolderPermissionJpaEntity.builder()
                .folder(folder)
                .user(user)
                .group(group)
                .canRead(domain.isCanRead())
                .canWrite(domain.isCanWrite())
                .canDelete(domain.isCanDelete())
                .canManage(domain.isCanManage())
                .inherited(domain.isInherited())
                .grantedBy(grantedBy)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
