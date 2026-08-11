package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.TrashRepositoryPort;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.trash.model.TrashItem;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.trash.TrashJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.DocumentJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.FolderJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.TrashJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final DocumentJpaRepository documentJpaRepository;

    @Autowired
    public TrashRepositoryAdapter(TrashJpaRepository trashJpaRepository,
                                  UserJpaRepository userJpaRepository,
                                  FolderJpaRepository folderJpaRepository,
                                  DocumentJpaRepository documentJpaRepository) {
        this.trashJpaRepository = trashJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.folderJpaRepository = folderJpaRepository;
        this.documentJpaRepository = documentJpaRepository;
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
        Optional<TrashJpaEntity> trashOpt = trashJpaRepository.findById(id);
        if (trashOpt.isPresent()) {
            return trashOpt.map(this::mapToDomain);
        }
        // Fallback: Check documentJpaRepository by ID for soft-deleted documents
        return documentJpaRepository.findById(id)
                .filter(doc -> doc.getStatus() == DocumentJpaEntity.DocumentStatus.TRASHED || doc.getDeletedAt() != null)
                .map(doc -> TrashItem.builder()
                        .id(doc.getId())
                        .entityType("DOCUMENT")
                        .entityId(doc.getId())
                        .name(doc.getTitle())
                        .originalFolderId(doc.getFolder() != null ? doc.getFolder().getId() : null)
                        .deletedBy(doc.getDeletedBy() != null ? doc.getDeletedBy().getId() : (doc.getOwner() != null ? doc.getOwner().getId() : null))
                        .ownerUsername(doc.getOwner() != null ? doc.getOwner().getUsername() : (doc.getDeletedBy() != null ? doc.getDeletedBy().getUsername() : null))
                        .deletedAt(doc.getDeletedAt() != null ? doc.getDeletedAt() : doc.getUpdatedAt())
                        .autoPurgeAt(doc.getDeletedAt() != null ? doc.getDeletedAt().plus(30, java.time.temporal.ChronoUnit.DAYS) : java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS))
                        .build());
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
    @Transactional(readOnly = true)
    public PageResponse<TrashItem> findTrash(UUID userId, boolean isAdminOrManager, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("deletedAt"), Sort.Order.desc("id")));

        // 1. Query documentJpaRepository for documents with TRASHED status
        Page<DocumentJpaEntity> docPage = isAdminOrManager
                ? documentJpaRepository.findByStatus(DocumentJpaEntity.DocumentStatus.TRASHED, pageable)
                : documentJpaRepository.findTrashedByUser(userId, pageable);

        if (!docPage.isEmpty() || docPage.getTotalElements() > 0) {
            List<TrashItem> content = docPage.getContent().stream()
                    .map(doc -> TrashItem.builder()
                            .id(doc.getId())
                            .entityType("DOCUMENT")
                            .entityId(doc.getId())
                            .name(doc.getTitle())
                            .originalFolderId(doc.getFolder() != null ? doc.getFolder().getId() : null)
                            .deletedBy(doc.getDeletedBy() != null ? doc.getDeletedBy().getId() : (doc.getOwner() != null ? doc.getOwner().getId() : null))
                            .ownerUsername(doc.getOwner() != null ? doc.getOwner().getUsername() : (doc.getDeletedBy() != null ? doc.getDeletedBy().getUsername() : null))
                            .deletedAt(doc.getDeletedAt() != null ? doc.getDeletedAt() : doc.getUpdatedAt())
                            .autoPurgeAt(doc.getDeletedAt() != null ? doc.getDeletedAt().plus(30, java.time.temporal.ChronoUnit.DAYS) : java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS))
                            .build())
                    .toList();

            return PageResponse.<TrashItem>builder()
                    .content(content)
                    .pageNumber(docPage.getNumber())
                    .pageSize(docPage.getSize())
                    .totalElements(docPage.getTotalElements())
                    .totalPages(docPage.getTotalPages())
                    .first(docPage.isFirst())
                    .last(docPage.isLast())
                    .empty(docPage.isEmpty())
                    .sortBy("deletedAt")
                    .sortDirection("DESC")
                    .build();
        }

        // 2. Fallback to trash table if no TRASHED documents found
        Page<TrashJpaEntity> jpaPage = isAdminOrManager
                ? trashJpaRepository.findByPurgedAtIsNull(pageable)
                : trashJpaRepository.findByDeletedByIdAndPurgedAtIsNull(userId, pageable);

        List<TrashItem> content = jpaPage.getContent().stream()
                .map(this::mapToDomain)
                .toList();

        return PageResponse.<TrashItem>builder()
                .content(content)
                .pageNumber(jpaPage.getNumber())
                .pageSize(jpaPage.getSize())
                .totalElements(jpaPage.getTotalElements())
                .totalPages(jpaPage.getTotalPages())
                .first(jpaPage.isFirst())
                .last(jpaPage.isLast())
                .empty(jpaPage.isEmpty())
                .sortBy("deletedAt")
                .sortDirection("DESC")
                .build();
    }

    @Override
    public void delete(UUID id) {
        trashJpaRepository.deleteById(id);
    }

    private TrashItem mapToDomain(TrashJpaEntity entity) {
        if (entity == null) return null;

        String name = null;
        String ownerUsername = entity.getDeletedBy() != null ? entity.getDeletedBy().getUsername() : null;

        if (entity.getEntityType() == TrashJpaEntity.EntityType.DOCUMENT) {
            Optional<DocumentJpaEntity> docOpt = documentJpaRepository.findById(entity.getEntityId());
            if (docOpt.isPresent()) {
                name = docOpt.get().getTitle();
                if (ownerUsername == null && docOpt.get().getOwner() != null) {
                    ownerUsername = docOpt.get().getOwner().getUsername();
                }
            }
        } else if (entity.getEntityType() == TrashJpaEntity.EntityType.FOLDER) {
            Optional<FolderJpaEntity> folderOpt = folderJpaRepository.findById(entity.getEntityId());
            if (folderOpt.isPresent()) {
                name = folderOpt.get().getName();
                if (ownerUsername == null && folderOpt.get().getOwner() != null) {
                    ownerUsername = folderOpt.get().getOwner().getUsername();
                }
            }
        }

        return TrashItem.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType().name())
                .entityId(entity.getEntityId())
                .name(name != null ? name : "Élément supprimé")
                .originalFolderId(entity.getOriginalFolder() != null ? entity.getOriginalFolder().getId() : null)
                .deletedBy(entity.getDeletedBy() != null ? entity.getDeletedBy().getId() : null)
                .ownerUsername(ownerUsername)
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
