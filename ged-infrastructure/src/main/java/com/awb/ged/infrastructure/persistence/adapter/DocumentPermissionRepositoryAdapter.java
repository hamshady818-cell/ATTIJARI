package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.DocumentPermissionRepositoryPort;
import com.awb.ged.domain.document.model.DocumentPermission;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentPermissionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.GroupJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.DocumentJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.DocumentPermissionJpaRepository;
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
public class DocumentPermissionRepositoryAdapter implements DocumentPermissionRepositoryPort {

    private final DocumentPermissionJpaRepository documentPermissionJpaRepository;
    private final DocumentJpaRepository documentJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final EntityManager entityManager;

    @Autowired
    public DocumentPermissionRepositoryAdapter(DocumentPermissionJpaRepository documentPermissionJpaRepository,
                                               DocumentJpaRepository documentJpaRepository,
                                               UserJpaRepository userJpaRepository,
                                               EntityManager entityManager) {
        this.documentPermissionJpaRepository = documentPermissionJpaRepository;
        this.documentJpaRepository = documentJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public DocumentPermission save(DocumentPermission permission) {
        DocumentPermissionJpaEntity entity = mapToEntity(permission);
        DocumentPermissionJpaEntity saved = documentPermissionJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentPermission> findById(UUID id) {
        return documentPermissionJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentPermission> findByDocumentId(UUID documentId) {
        return documentPermissionJpaRepository.findByDocumentId(documentId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        documentPermissionJpaRepository.deleteById(id);
    }

    private DocumentPermission mapToDomain(DocumentPermissionJpaEntity entity) {
        if (entity == null) return null;
        return DocumentPermission.builder()
                .id(entity.getId())
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .groupId(entity.getGroup() != null ? entity.getGroup().getId() : null)
                .canRead(entity.isCanRead())
                .canWrite(entity.isCanWrite())
                .canDelete(entity.isCanDelete())
                .canShare(entity.isCanShare())
                .grantedBy(entity.getGrantedBy() != null ? entity.getGrantedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DocumentPermissionJpaEntity mapToEntity(DocumentPermission domain) {
        if (domain == null) return null;

        DocumentJpaEntity document = null;
        if (domain.getDocumentId() != null) {
            document = documentJpaRepository.findById(domain.getDocumentId()).orElse(null);
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

        DocumentPermissionJpaEntity entity = DocumentPermissionJpaEntity.builder()
                .document(document)
                .user(user)
                .group(group)
                .canRead(domain.isCanRead())
                .canWrite(domain.isCanWrite())
                .canDelete(domain.isCanDelete())
                .canShare(domain.isCanShare())
                .grantedBy(grantedBy)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
