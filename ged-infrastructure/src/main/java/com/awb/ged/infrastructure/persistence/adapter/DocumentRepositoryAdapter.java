package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.dto.document.DocumentSearchQuery;
import com.awb.ged.application.dto.document.DocumentSearchResultDto;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.domain.document.model.*;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentCheckoutJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.tag.TagJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.*;
import com.awb.ged.infrastructure.persistence.specification.DocumentSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Transactional
public class DocumentRepositoryAdapter implements DocumentRepositoryPort {

    private final DocumentJpaRepository documentJpaRepository;
    private final DocumentVersionJpaRepository documentVersionJpaRepository;
    private final DocumentCheckoutJpaRepository documentCheckoutJpaRepository;
    private final FolderJpaRepository folderJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final TagJpaRepository tagJpaRepository;

    public DocumentRepositoryAdapter(DocumentJpaRepository documentJpaRepository,
                                     DocumentVersionJpaRepository documentVersionJpaRepository,
                                     DocumentCheckoutJpaRepository documentCheckoutJpaRepository,
                                     FolderJpaRepository folderJpaRepository,
                                     UserJpaRepository userJpaRepository,
                                     TagJpaRepository tagJpaRepository) {
        this.documentJpaRepository = documentJpaRepository;
        this.documentVersionJpaRepository = documentVersionJpaRepository;
        this.documentCheckoutJpaRepository = documentCheckoutJpaRepository;
        this.folderJpaRepository = folderJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.tagJpaRepository = tagJpaRepository;
    }

    // --- Document CRUD ---

    @Override
    public Document save(Document document) {
        DocumentJpaEntity entity = mapToEntity(document);
        DocumentJpaEntity saved = documentJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        return documentJpaRepository.findById(id)
                .filter(entity -> !entity.isDeleted())
                .map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findByIdIncludingDeleted(UUID id) {
        return documentJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByFolderId(UUID folderId) {
        List<DocumentJpaEntity> list = (folderId == null)
                ? documentJpaRepository.findByFolderIsNullAndDeletedAtIsNull()
                : documentJpaRepository.findByFolderIdAndDeletedAtIsNull(folderId);
        return list.stream().map(this::mapToDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return documentJpaRepository.findAll().stream().map(this::mapToDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        documentJpaRepository.deleteById(id);
    }

    // --- Search ---

    @Override
    @Transactional(readOnly = true)
    public com.awb.ged.common.model.PageResponse<DocumentSearchResultDto> search(DocumentSearchQuery query) {
        String sortField = "title".equals(query.getSortBy()) || "name".equals(query.getSortBy()) ? "title" : query.getSortBy();
        org.springframework.data.domain.Sort.Direction direction = "ASC".equalsIgnoreCase(query.getSortDirection())
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), org.springframework.data.domain.Sort.by(direction, sortField));

        Page<DocumentJpaEntity> page = documentJpaRepository.findAll(
                DocumentSpecifications.buildSearch(query), pageable);

        List<DocumentSearchResultDto> dtos = page.getContent()
                .stream()
                .map(this::mapToSearchResult)
                .toList();

        return com.awb.ged.common.model.PageResponse.<DocumentSearchResultDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .sortBy(query.getSortBy())
                .sortDirection(query.getSortDirection())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }


    // --- Document Version Operations ---

    @Override
    public DocumentVersion saveVersion(DocumentVersion version) {
        DocumentVersionJpaEntity entity = mapVersionToEntity(version);
        DocumentVersionJpaEntity saved = documentVersionJpaRepository.save(entity);
        return mapVersionToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentVersion> findVersionById(UUID versionId) {
        return documentVersionJpaRepository.findById(versionId).map(this::mapVersionToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentVersion> findVersionsByDocumentId(UUID documentId) {
        return documentVersionJpaRepository.findByDocumentIdOrderByVersionNumberAsc(documentId)
                .stream()
                .map(this::mapVersionToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countVersionsByDocumentId(UUID documentId) {
        return documentVersionJpaRepository.countByDocumentId(documentId);
    }

    // --- Checkout / Lock Operations ---

    @Override
    public void saveCheckout(UUID documentId, UUID userId) {
        DocumentJpaEntity document = documentJpaRepository.findById(documentId)
                .orElseThrow(() -> new com.awb.ged.common.exception.NotFoundException(
                        com.awb.ged.common.exception.ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document not found: " + documentId));
        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new com.awb.ged.common.exception.NotFoundException(
                        com.awb.ged.common.exception.ErrorCode.USER_NOT_FOUND,
                        "User not found: " + userId));

        DocumentCheckoutJpaEntity checkout = DocumentCheckoutJpaEntity.builder()
                .document(document)
                .checkedOutBy(user)
                .checkedOutAt(Instant.now())
                .build();

        documentCheckoutJpaRepository.save(checkout);
    }

    @Override
    public void checkin(UUID documentId, UUID userId) {
        documentCheckoutJpaRepository
                .findByDocumentIdAndCheckedInAtIsNull(documentId)
                .ifPresent(checkout -> {
                    checkout.setCheckedInAt(Instant.now());
                    documentCheckoutJpaRepository.save(checkout);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckoutInfo> findActiveCheckout(UUID documentId) {
        return documentCheckoutJpaRepository
                .findByDocumentIdAndCheckedInAtIsNull(documentId)
                .map(c -> new CheckoutInfo(
                        c.getCheckedOutBy() != null ? c.getCheckedOutBy().getId() : null,
                        c.getCheckedOutAt(),
                        c.getExpectedReturnAt()
                ));
    }

    // --- Dashboard Stats ---

    @Override
    @Transactional(readOnly = true)
    public long countAllDocuments() {
        return documentJpaRepository.countNonDeleted();
    }

    @Override
    @Transactional(readOnly = true)
    public long sumAllVersionSizeBytes() {
        return documentJpaRepository.sumAllVersionSizeBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findRecentUploads(int limit) {
        return documentJpaRepository
                .findRecentUploads(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findRecentlyModified(int limit) {
        return documentJpaRepository
                .findRecentlyModified(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    // --- Bulk Tag ---

    @Override
    public void addTagToDocument(UUID documentId, String tagName) {
        DocumentJpaEntity document = documentJpaRepository.findById(documentId).orElse(null);
        if (document == null) return;

        TagJpaEntity tag = tagJpaRepository.findByName(tagName).orElseGet(() -> {
            TagJpaEntity newTag = new TagJpaEntity();
            newTag.setId(UUID.randomUUID());
            newTag.setName(tagName);
            return tagJpaRepository.save(newTag);
        });

        document.getTags().add(tag);
        documentJpaRepository.save(document);
    }

    // --- Private Mapping Helpers ---

    private Document mapToDomain(DocumentJpaEntity entity) {
        if (entity == null) return null;

        DocumentLock lock = null;
        Optional<DocumentCheckoutJpaEntity> activeCheckout = documentCheckoutJpaRepository
                .findByDocumentIdAndCheckedInAtIsNull(entity.getId());

        if (activeCheckout.isPresent()) {
            DocumentCheckoutJpaEntity checkout = activeCheckout.get();
            lock = DocumentLock.builder()
                    .lockedBy(checkout.getCheckedOutBy() != null ? checkout.getCheckedOutBy().getId() : null)
                    .lockedAt(checkout.getCheckedOutAt())
                    .expiration(checkout.getExpectedReturnAt())
                    .build();
        }

        List<DocumentTag> tags = entity.getTags().stream()
                .map(t -> DocumentTag.builder()
                        .name(t.getName())
                        .generatedByIa(false)
                        .confidence(null)
                        .build())
                .toList();

        // Map status from JPA enum to domain enum
        Document.DocumentStatus domainStatus = Document.DocumentStatus.DRAFT;
        if (entity.getStatus() != null) {
            try {
                domainStatus = Document.DocumentStatus.valueOf(entity.getStatus().name());
            } catch (IllegalArgumentException ignored) {}
        }

        return Document.builder()
                .id(entity.getId())
                .name(entity.getTitle())
                .description(entity.getDescription())
                .status(domainStatus)
                .mimeType(entity.getMimeType())
                .folderId(entity.getFolder() != null ? entity.getFolder().getId() : null)
                .categoryId(null)
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .activeVersionId(entity.getCurrentVersion() != null ? entity.getCurrentVersion().getId() : null)
                .lock(lock)
                .tags(new ArrayList<>(tags))
                .createdAt(entity.getCreatedAt())
                .deletedAt(entity.getDeletedAt())
                .deletedBy(entity.getDeletedBy() != null ? entity.getDeletedBy().getId() : null)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DocumentSearchResultDto mapToSearchResult(DocumentJpaEntity entity) {
        boolean locked = documentCheckoutJpaRepository
                .findByDocumentIdAndCheckedInAtIsNull(entity.getId()).isPresent();

        List<String> tagNames = entity.getTags().stream()
                .map(TagJpaEntity::getName)
                .toList();

        return DocumentSearchResultDto.builder()
                .id(entity.getId())
                .name(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .mimeType(entity.getMimeType())
                .folderId(entity.getFolder() != null ? entity.getFolder().getId() : null)
                .folderName(entity.getFolder() != null ? entity.getFolder().getName() : null)
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .ownerUsername(entity.getOwner() != null ? entity.getOwner().getUsername() : null)
                .activeVersionId(entity.getCurrentVersion() != null ? entity.getCurrentVersion().getId() : null)
                .isLocked(locked)
                .tags(tagNames)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DocumentJpaEntity mapToEntity(Document domain) {
        if (domain == null) return null;

        FolderJpaEntity folder = null;
        if (domain.getFolderId() != null) {
            folder = folderJpaRepository.findById(domain.getFolderId()).orElse(null);
        }

        UserJpaEntity owner = null;
        if (domain.getOwnerId() != null) {
            owner = userJpaRepository.findById(domain.getOwnerId()).orElse(null);
        }

        DocumentVersionJpaEntity currentVersion = null;
        if (domain.getActiveVersionId() != null) {
            currentVersion = documentVersionJpaRepository.findById(domain.getActiveVersionId()).orElse(null);
        }

        Set<TagJpaEntity> tags = new HashSet<>();
        if (domain.getTags() != null) {
            for (DocumentTag tagDomain : domain.getTags()) {
                tagJpaRepository.findByName(tagDomain.getName()).ifPresent(tags::add);
            }
        }

        UserJpaEntity deleter = null;
        if (domain.getDeletedBy() != null) {
            deleter = userJpaRepository.findById(domain.getDeletedBy()).orElse(null);
        }

        // Map domain status to JPA status
        DocumentJpaEntity.DocumentStatus jpaStatus = DocumentJpaEntity.DocumentStatus.DRAFT;
        if (domain.getStatus() != null) {
            try {
                jpaStatus = DocumentJpaEntity.DocumentStatus.valueOf(domain.getStatus().name());
            } catch (IllegalArgumentException ignored) {}
        }

        DocumentJpaEntity entity = DocumentJpaEntity.builder()
                .title(domain.getName())
                .description(domain.getDescription())
                .status(jpaStatus)
                .mimeType(domain.getMimeType())
                .folder(folder)
                .owner(owner)
                .createdBy(owner)
                .updatedBy(owner)
                .deleted(domain.isDeleted())
                .deletedAt(domain.getDeletedAt())
                .deletedBy(deleter)
                .currentVersion(currentVersion)
                .tags(tags)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }

    private DocumentVersion mapVersionToDomain(DocumentVersionJpaEntity entity) {
        if (entity == null) return null;

        String fileRefValue = entity.getStorageBucket() + "/" + entity.getStoragePath();

        return DocumentVersion.builder()
                .id(entity.getId())
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .versionNumber(entity.getVersionNumber())
                .hash(entity.getChecksumSha256())
                .sizeBytes(entity.getFileSizeBytes())
                .fileReferenceId(new FileReferenceId(fileRefValue))
                .uploadedBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .uploadedAt(entity.getCreatedAt())
                .build();
    }

    private DocumentVersionJpaEntity mapVersionToEntity(DocumentVersion domain) {
        if (domain == null) return null;

        DocumentJpaEntity document = null;
        if (domain.getDocumentId() != null) {
            document = documentJpaRepository.findById(domain.getDocumentId()).orElse(null);
        }

        UserJpaEntity uploader = null;
        if (domain.getUploadedBy() != null) {
            uploader = userJpaRepository.findById(domain.getUploadedBy()).orElse(null);
        }

        String bucket = "ged-documents";
        String path = "default-path";
        if (domain.getFileReferenceId() != null && domain.getFileReferenceId().getValue() != null) {
            String val = domain.getFileReferenceId().getValue();
            int slashIndex = val.indexOf('/');
            if (slashIndex > 0) {
                bucket = val.substring(0, slashIndex);
                path = val.substring(slashIndex + 1);
            } else {
                path = val;
            }
        }

        DocumentVersionJpaEntity entity = DocumentVersionJpaEntity.builder()
                .document(document)
                .versionNumber(domain.getVersionNumber())
                .checksumSha256(domain.getHash() != null ? domain.getHash() : "0".repeat(64))
                .fileSizeBytes(domain.getSizeBytes())
                .storageBucket(bucket)
                .storagePath(path)
                .fileReferenceId(domain.getFileReferenceId() != null
                        ? domain.getFileReferenceId().getValue()
                        : (bucket + "/" + path))
                .mimeType("application/octet-stream")
                .createdBy(uploader)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
